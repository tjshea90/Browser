# D1 - the blocking engine runs INSIDE the extension (design, 2026-11-11; implements research/ADBLOCK_ENGINE.md section 1 and REVIEW-V7 P1)

Why: today every uncached sub-resource request makes the built-in extension send a native message; GeckoView serves it on the Android UI thread and Gecko holds the request until the reply
(3 UI tasks + 2 thread hops per request, hundreds of requests per page, on 4 x Cortex-A53). D1 decides inside the extension with the same compiled lists, so a request costs one synchronous JS call
and no message. The Kotlin engine stays for the WebView build, NavPolicy (main frames, malware), popups, `isPageAllowed` and as the fallback described below.

## What stays in Kotlin (the app owns policy and data)
* Per-site Shields switches (`Shields.effective`), `FilterSet.isPageAllowed(host)` and the tab counters. The extension asks ONE question per page: `cfg`.
* The lists: `Engine` loads/refreshes the `FilterSet`; every assignment bumps `Engine.generation` (0 = nothing loaded yet).
* Cosmetic CSS (`css` message) is unchanged.

## Messages (extension -> app; all via `runtime.sendNativeMessage('kestrel', <JSON text>)`, replies are plain strings, the only shape the engine accepted)
| k | request | reply |
| - | - | - |
| `cfg` | `{"k":"cfg","h":"<top page host>"}` once per `main_frame` (deduped per host while in flight) | `"<mode>:<gen>"`. mode `1` = block on this page, `0` = Shields off / page allowed, `L` = legacy (kill switch: ask per request). gen = `Engine.generation` |
| `img` | `{"k":"img","g":<gen>,"i":<chunk>}` | chunk 0 = `"<gen>:<totalBytes>:<chunkBytes>:"` + base64, later chunks base64 only; `""` when gen no longer current |
| `cnt` | `{"k":"cnt","p":"<page url>","c":[ads,trackers,malware,annoyance]}` batched every 500 ms | `""` |
| `st` | `{"k":"st",...}` at most every 5 s while checks run (Diagnostics numbers) | `""` |
| `req` | legacy per-request question (unchanged) | `"1"` / `"0"` |

## Extension state machine (background.js + engine.js)
1. Start: `engine = null`. Every request uses the legacy `req` path (what ships today) until an engine is ready, so nothing is ever unblocked at start-up.
2. A `cfg` reply with `gen != engineGen` and `gen > 0` starts an image pull (chunks of 256 KB raw, sequential, `setTimeout` between so the parent main thread is never held > ~10 ms). The new engine replaces the old
   one in one assignment; the old one keeps serving until then. A list refresh therefore takes effect from the next page load after the pull (seconds), never mid-request.
3. With an engine and `mode == '1'`: `onBeforeRequest` is fully synchronous: `topPage(d)` -> cached host -> `engine.check(url, pageHost, typeMask)` -> `{cancel:true}` or `{}`. Blocked requests add to a
   per-page counter array; a timer flushes `cnt`. No per-request cache any more (a check is about as cheap as the cache key).
4. `main_frame`: delete the host's cached cfg, fire a fresh `cfg` (not awaited: the reply beats the main document's bytes over a hotspot). A sub-resource that arrives before its host has a cfg waits on the
   in-flight promise (one RTT, once), so a toggled Shields switch can never be applied late. `mode L` or engine failure -> legacy path.
5. Fail open and loud: any exception inside `check` is caught, counted (`errors` in Diagnostics) and the request passes; 20 errors turn the engine off for the session (legacy path takes over).

## Image format KEX1 (little-endian, 4-byte aligned; written by `:core` `ExtImage.encode(FilterSet, Psl)` from the loaded sets, so there is exactly one list compiler)
Header `[magic 0x3158454B 'KEX1'][version 1][generation][sectionCount]` then `sectionCount x [byteOffset][elementCount]`, then the sections, then a trailer word = wrapping sum of all earlier 32-bit words.
Sections (hash arrays are 64-bit values stored as two Int32 `[lo, hi]`, sorted by signed 64-bit order = (hi signed, lo unsigned)): host hashes + flags, host-exception hashes + flags, pattern bytes, pattern offsets, meta, dom,
domPool, token table (key/start/len/ruleIds), gram table (key/start/len/ruleIds), noToken, PSL rules / wildcard parents / exceptions. JS reads every section as a zero-copy typed-array view.
The JS engine is a line-by-line port of `FilterSet.check` (hashing on 32-bit limbs: FNV-1a 64 with prime 0x100000001B3 = (h << 40) + h * 0x1B3, murmur3 finaliser).

## Tests (the point: JS verdicts == Kotlin verdicts)
* `web-tests/engine.test.js` (node --test, also run in the `test-js` job): hash vectors, glob, `suffixHashes`, PSL `siteOf`, and 100 % verdict equality on `web-tests/fixtures/mini.corpus.tsv` using `mini.kex`.
* `:core` `ExtImageTest` regenerates both fixtures in memory and fails if the committed files differ (`KESTREL_REGEN=1` rewrites them), so Kotlin and JS cannot drift.
* One-off, not committed: the same comparison over a corpus derived from the real `default.kfs` (hundreds of thousands of requests: rule text turned into URLs, near-misses, real page URLs).
* Emulator: `ads.html` blocked 3 of 3 as before, `req` message count per page ~0, Diagnostics shows checks/avg/max us.

## Kill switch and numbers
Diagnostics > "Blocking engine" row shows `in the extension / compatibility mode`, generation, image size, load ms, checks, average and max microseconds, fallbacks, errors; tapping the row flips `Prefs.engineInExtension`
(takes effect at the next page load: `cfg` then answers `L`). The tablet numbers decide whether the legacy path is ever deleted.

## Not in D1 (still open)
`top`-frame flag for cosmetic CSS, `$redirect`/`$removeparam`, scriptlets (D2), tab-id keyed page state, deleting the legacy path.
