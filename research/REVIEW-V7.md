# REVIEW-V7 - speed / page-load / stutter code review (owner request 8, 2026-11-11)

Two read-only reviewers (page loading; UI-thread stutter) read the code with the target device in mind (MT8168B 4x Cortex-A53, Android 10 Go, 2 GB, GeckoView 160, hotspot). Nothing was measured on the
tablet: every ms figure below is an estimate, "[V]" = verified in code or GeckoView 160 bytecode. Status column: DONE (alpha12), OPEN (next), TABLET (needs measuring on the real device).

## Page loading
| # | Finding | Status |
| - | - | - |
| P1 | Every uncached sub-resource request costs 3 Android UI-thread tasks + 2 thread hops: Gecko -> UI thread (`registerUiThreadListener`), `ExtensionStore.get().accept()` is a `Handler.post`, our `GeckoResult()` was built on the UI thread so `complete()` was re-posted there, plus a `Log.d` per message and `toJSONObject()` of the whole bundle [V]. **Real fix = D1: decide inside the extension** (`research/ADBLOCK_ENGINE.md` s1-3; the network half of `default.kfs` is 2.62 of 4.40 MB: 99,318 host rules, 20,926 pattern rules, 885 site rules, 138 gram rules, 93 token-less rules that run on every request; a 250-line JS port matched the Kotlin engine on 156,731 requests; needs KFS2 (little-endian, aligned) or a one-time byte swap, a per-main-frame config message, batched counters, and a way for refreshed lists to reach the extension: `file://` fetch / chunked Port, spike first). | CODE DONE (alpha13, D1): engine.js + KEX1 image + per-page cfg + saved copy; Kotlin/JS verdicts proven equal on 2,200 requests; emulator A/B and tablet numbers in Diagnostics (Blocker line) |
| P2 | `setAllowedInPrivateBrowsing(ext, true)` ran at every launch and Activity re-creation and RELOADS the running extension (full shutdown + start of its background page) right while the first page loads; built-ins already have the permission [V] | DONE: only called when `metaData.allowedInPrivateBrowsing` is false. Emulator: first page after a cold start is now blocked and sniffed without a reload |
| P3a | Reply completed on the UI thread | DONE: `GeckoResult(handler)` on a default-priority HandlerThread (sendSuccess/sendError are dispatched to Gecko's thread by the engine, any thread may call them) |
| P3b | No release rule removing GeckoView's per-message `Log.d` | DONE: `-assumenosideeffects` for Log.v/Log.d in `geckoapp/proguard-rules.pro` |
| P3c | Send the page host, not the URL, in 'req'/'cnt' | OPEN (moot after D1) |
| P4 | Verdict cache: key has the FULL page URL so first-visit hit rate is ~0 %; FIFO 4000 entries; never invalidated: Shields off for a site, a Shields switch or a swapped list kept answering from before; every blocked cache hit sent a native 'cnt' message (3 UI tasks again) | DONE: cache cleared at every `main_frame`, counters batched (500 ms, `{k:'cnt',p,n}`) |
| P5 | Cold start order: Gecko runtime created only after the first frame; `buildUi` builds TabGrid/Sheet/FindBar/ErrorView/HomeView eagerly; loader thread at nice 13 (background cgroup); PSL parse calls `IDN.toASCII` for 10,411 lines (9,952 pure ASCII); no `speculativeConnect` call site; until `current` is set `shouldBlock` fails open | OPEN (hundreds of ms [E]; TABLET) |
| P6 | Cosmetic CSS ('css' message) shares the single verdict worker: the first generic-selector build (~2 s on the emulator) stalls every queued verdict; every frame (ad iframes too) gets generic CSS (`top` flag ignored) | PARTLY DONE: 'css' runs on its own thread; the `top` flag is still ignored (OPEN) |
| P7 | Kotlin side of 'req' recomputes page-level data per request (`hostOf(pageUrl)`, `Shields.effective` = 5 SharedPreferences reads, `isPageAllowed`, `tabForPage`) | OPEN (moot after D1) |
| P8 | Gecko prefs: nothing wrong for 4xA53/2 GB. A/B candidates (TABLET): `TRR_MODE_FIRST` to Cloudflare (first DoH lookup needs its own TCP+TLS; every miss waits for the TRR timeout before native DNS), GeckoView default idle traffic on a metered link (Remote Settings poll, extension/system add-on updates, connectivity probes, ETP list updates): audit with a logging proxy | OPEN |
| P9 | `onLoadRequest` does ~5 URL parses, 3-4 `Shields.effective`, redirect unwrapping and a full `FilterSet.check` on the UI thread per navigation and redirect hop; a rewrite denies then re-issues `loadUri` (second navigation start); the neon pulses again 3 s into a load | OPEN |
| P10 | The passive media sniffer asks for `responseHeaders` on every xhr/other/frame response (Gecko builds a header array on the parent main thread each time) | OPEN (gain unverified) |

## UI-thread stutter
| # | Finding | Status |
| - | - | - |
| S1 | Download progress rebuilt/posted up to 5x/s with 5-6 Binder calls each (`getLaunchIntentForPackage` = 2 PackageManager queries, `PendingIntent.getActivity/getService`, `notify`); `wake.acquire(10 min)` at every tick; `allViews()` (a pass over every job under `Engine.lock`, a `java.net.URI` parse and a sort each) 2-3x per tick | DONE: `Notifier.ProgressModel` compared before anything is built, intents built once, post at most once per second (3 s while a page of the app shows), wake lock renewed once a minute, one `allViews()` snapshot per 150 ms |
| S2 | KeyboardProbe: `getWindowVisibleDisplayFrame` = IPC to the window manager on API 29, polled every 100 ms while an address field has focus | DONE (mild): 300 ms once the keyboard sat still for a second. TABLET |
| S3 | Omnibox suggestions: two `LIKE '%q%'` scans per key, 7 rows rebuilt (about 35 views) per bind | PARTLY DONE: 70 ms debounce; row reuse OPEN |
| S4 | `KestrelGeckoView` called `onTouchEventForDetailResult` (allocates a result and crosses into native) for EVERY touch event of a drag, 60-120/s [V bytecode] | DONE: only the touch-down of a possible pull asks for the detail |
| S5 | Tab grid torn down and rebuilt on every tab event (progress, title, URL, back/forward) while open | DONE: one rebuild per looper turn (keying cards by tab id and updating in place is still OPEN) |
| S6 | Downloads screen bound every row (incl. finished ones) 4x/s, `setText` rebuilds the layout even for equal text | DONE: only unfinished rows, text only when changed |
| S7 | Neon glow layers destroyed/re-created around every touch (`setLayerType(NONE)` on DOWN, HARDWARE again 1.5 s after release; a fast fling outlasts the hold) | OPEN (TABLET; part of the owner's neon complaint, do not touch blind) |
| S8 | Minor: PullChip GONE <-> VISIBLE (lays out `content` at each pull) | DONE (INVISIBLE). Progress line redrawn at every 1 % | DONE (3 % steps). Menu grid/sheets/tab grid built from scratch on every open (one-time hitch); FrameMetrics listener + watchdog always on | OPEN |

## Checked, no action (both reviewers)
Custom Views allocate nothing per frame; `updateUi()` coalesces within a looper turn; every Prefs write is `apply()`; history/session/thumbnail/download-store writes are on IO threads; the post-load capture waits 3 s
without touch; shield-count refresh capped at 4/s; `FilterSet.check` has no regex/sort/boxing (bounded work: MAX_URL 2048, MAX_GRAM_EVALS 3000); `Psl.siteOf` has a 256-slot cache.

## How to prove it on the tablet (cheap)
Time `DownloadService.refresh()`, `typingTick`, `TabGrid.refresh()` and the shield bridge round trip with `elapsedRealtimeNanos` and log anything over 4 ms into Health (Diagnostics), or record a Perfetto trace
(sched, binder_driver, gfx) while a download runs and a page is flung. Log `sendNativeMessage` RTT p50/p95 and verdict-cache hit rate from `background.js` before and after D1.

## D1 plan (alpha13), in order
1. Spike (30 min on the emulator): can the background page `fetch('file:///data/data/<pkg>/files/...')`? If not, chunked Port transport (base64 256 KB messages, once per list update, cached in `storage.local`).
2. KFS2 writer in `:core` (little-endian, 4-byte aligned sections; v1 stays readable) + golden test: v1 and v2 verdicts equal on the corpus.
3. `engine.js` (~300 lines: `Hash.h64` on 32-bit limbs, token/gram bucket probe, `evalRule`/glob/domain/party, request-type map) + `web-tests/engine.test.js` against a `corpus.tsv` emitted by a `:tools` task (100 % equal).
4. New `background.js`: synchronous blocking listener, per-tab page host/config from ONE message per `main_frame` (ads switch, per-host overrides, epoch), batched counters, fail open until loaded, kill switch back to the native path.
5. Measure RTT/hit-rate/handler time before and after on the tablet; keep the native path as the fallback until the numbers are in.
