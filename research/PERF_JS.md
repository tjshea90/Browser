# perf-js - JavaScript that runs inside pages and inside the engine (Kestrel G extension + WebView injected script)

Read-only review, 2026-11-12. Nothing was run on a device, nothing in the repo was changed, no tests or builds were run.
Evidence tags: **[read]** = verified in code/data in this repo. **[guess]** = estimate from operation counts or from Gecko behaviour I could not check offline. Numbers marked [guess] were not measured; the tablet is 4x Cortex-A53 and every "ms" below is an op-count estimate.
The only data I computed (read-only, inline Python over `app/src/main/assets/filters/default.kfs`) is tagged **[read-data]**.
Tests in `web-tests/` run headless Chromium with a FAKE `browser` object (content.test.js:22-25, background.test.js:55-72): they prove logic, never Gecko cost.

## 0. Where the cost lands (read this first)

* The extension's background page runs in the Gecko PARENT process, on its main thread: `GeckoEngine.kt:49 .extensionsProcessEnabled(false)`; ADBLOCK_ENGINE.md decision 7; FREEZE.md cause 7. So every ms of background JS is a ms of the thread that dispatches network events for all tabs. [read]
* Every `sendNativeMessage` is served on the Android UI thread first (ShieldsBridge.kt:42-52: "GeckoView delivers native messages on the Android UI thread"), then handed to a worker. The UI thread also feeds Gecko's vsync (FREEZE.md s2). Fire-and-forget messages are therefore not free. [read]
* Content scripts run in the single content process (`dom.ipc.processCount = 1`, gecko-prefs.txt) on the same main thread as the page's own JS. [read]

**Verdict.** There is no high-severity item. By APZ's definition nothing in these scripts delays scrolling: no `touchstart` / `touchmove` / `wheel` / `scroll` listener, no MutationObserver (Gecko build), no `setInterval`, no `requestAnimationFrame`, no layout read in a loop (grep over all five files [read]). The per-request engine path is small. What costs is multiplicative: per frame, per XHR response, per distinct media URL, per idle scan. I also found one real functional bug with a performance flavour (B1: the element-hiding cache is never invalidated).

## 1. Inventory: when each file runs and what it does on every page

| File | When | Work on every page | Cost on 4xA53 |
| - | - | - | - |
| `manifest.json` | content_scripts: `content.js`, matches http(s), `run_at document_start`, `all_frames true`, `match_about_blank false` (line 11). Background: `engine.js` + `background.js`, persistent MV2 page | - | - |
| `content.js` (122 lines) | every http(s) frame, document_start; scan is idle | 5 IIFEs: (1) `css` message (L7) then a `<style>` element; (2) capture `loadedmetadata` listener on document (L40); (3) guard relay: READY + POP capture listeners (L51-52) and a `guard` message (L55); (4) media scan: `requestIdleCallback(scan,{timeout:3000})` after DOMContentLoaded and again `load`+2.5 s (L93-95); (5) capture `contextmenu` on window (L104) | At start: 2 IPC messages, 6 listeners, one sandbox per frame [guess]. Scan: see J4 |
| `guard-main.js` (164 lines) | MAIN world, document_start, all_frames, also about:blank / srcdoc / data: / blob: (background.js:219-221) | Replaces `window.open` and `HTMLFormElement.prototype.submit`; window capture listeners `click`, `auxclick`, `touchend`, `pointerup` (L139-142); 1 READY event | ~100 us of script per frame once compiled [guess]; per tap: 3 ancestor walks; per link tap: forced layout (J7) |
| `background.js` (335 lines) | parent main thread, always | One blocking `onBeforeRequest` for http/https/ws/wss, all types (L259-275); non-blocking `onHeadersReceived` with `responseHeaders` for 6 types (L303-328); 3 `onMessage` listeners; 4 one-shot timers, no intervals | Per request ~25-50 us [guess]; per XHR/other response ~30-60 us [guess] (J2) |
| `engine.js` (462 lines) | called from the blocking listener | Hash walk of the host suffixes, token/gram bucket probes, glob on matches. Allocation-light: no per-request Map/array except `pageHashes()` | 5.2 us avg on x86 V8 (ADBLOCK_ENGINE.md s1.2), so 25-50 us on A53 [guess]; ~5-8 ms per 150-request page |
| `app/.../js/shields.js` (193 lines, WebView build only) | main frame only, by `evaluateJavascript` at `onPageCommitVisible` (BrowserWebViewClient.kt:97-101) and a marker check at `onPageFinished` (L110-111) | Canvas/audio/hardwareConcurrency hooks (lazy cost); `pn` block adds a MutationObserver (L162-192) | Observer only when Data saver is not OFF; `Prefs.kt:38` default is OFF, so by default it is NOT installed [read] |

Static facts about the lists (what the engine and the CSS carry) [read-data]:
* Network: 99,318 host rules, 20,926 pattern rules (20,695 in token buckets, 138 in the 4-gram table, 93 token-less); largest token bucket 78 rules. Of the 93 token-less rules evaluated on every request, 89 are options-only (empty pattern) and 92 carry a domain list, max 146 entries, so a 3rd-party script request touches 32 such rules and 46 domain entries in total. They almost never reach `matchPattern`. The engine image is ~2.6 MB (REVIEW-V7 P1), all typed-array views over one ArrayBuffer.
* Cosmetic: 29,204 generic selectors; only the first 1,200 are used (`DEFAULT_GENERIC_LIMIT`, CosmeticIndex.kt:71) and all 1,200 are `#id` selectors (~18.7 KB of CSS), hash-bucketed by Stylo [guess on internals]. Site groups: 28,914, median 1 selector, max 88 (~4.8 KB). Across all site selectors: 1,730 use `:has(`, 4,391 attribute-substring, 3,205 are not id/class/tag-bucketable. So matching cost of the injected CSS is small; the cap of 1,200 is not a performance necessity.

## 2. Message traffic (how often scripts talk to the background page / the app)

| From -> to | Message | When | Volume |
| - | - | - | - |
| content -> bg | `css` | document_start, every frame | 1 per frame |
| content -> bg | `guard` | document_start, every frame | 1 per frame |
| content -> bg | `dom` | scan finds a media address | <= 40 per frame per page (L68-71), unbatched |
| content -> bg | `vid` | `loadedmetadata` of a new (src,height,duration) | 1 per media element |
| content -> bg | `pop` | every trusted click (`tap`), every blocked pop-up/redirect/overlay/form, long-press on video | 1 per tap |
| bg -> app | `cfg` | each `main_frame`; refresh when the answer is > 60 s old and requests flow | ~1 per page |
| bg -> app | `css` | cache miss on `(top|frame, host)`; NO in-flight dedupe (J5) | 1 per new frame host (+ duplicates) |
| bg -> app | `media` / `vid` / `pop` | each sniffed URL / each content message | see J3 |
| bg -> app | `cnt` | 500 ms after the first block | <= 2/s |
| bg -> app | `st` | 5 s after the first judged request | <= 0.2/s |
| bg -> app | `img` | list changed (rare) | ~11 chunks of 256 KB, `setTimeout(0)` between |

Per-request path: zero messages once the engine is loaded (tested: background.test.js:102-120). Each bg -> app message is one Android UI-thread task.

## 3. Scroll- and touch-delaying listeners

None in the extension. The complete list of listeners our code adds to window/document [read]:
`loadedmetadata` (capture, document), READY, POP (capture, document), `DOMContentLoaded`, `load`, `contextmenu` (capture, window) in content.js; `click`, `auxclick`, `touchend`, `pointerup` (capture, window), CFG (capture, document) in guard-main.js. None is `touchstart`, `touchmove`, `wheel`, `mousewheel`, `scroll`. `click`/`auxclick` call `preventDefault` and must stay non-passive; `touchend`/`pointerup` never do (J7). The page's own non-passive handlers are the real source of the scroll-start wait (SCROLL.md R2 update, TASKS R2b); see J11.

## 4. Memory held [read unless noted]

* Engine: one ~2.6 MB ArrayBuffer, zero-copy views (engine.js:153-161). Transient peak while a new image is pulled: buffer + base64 chunk strings + IndexedDB clone, ~8-10 MB, only on a list change. IndexedDB copy (background.js:73-83) means no pull at launch.
* Caches: `hostOfPage` 200, `siteCache` 256 (per engine), `cfg*` 300, `verdicts` 4,000 (legacy path only), `tabTop` 60, `sniffed` 40 tabs x 300 URLs (<= ~1.8 MB), `cssCache` 200 entries. `cssCache` holds the same 18.7 KB generic CSS once per top host: up to 200 x ~19 KB = ~4 MB (+ site rules) that is never freed (J8). Everything else is small.
* Per content-script frame: one `<style>` of 0-24 KB in the top document only.

## 5. Findings, ranked (high first; there is no high)

### J1 - medium - Two document_start scripts and two messages in EVERY frame
Evidence: manifest.json:11 (content.js, all_frames); background.js:219-221 (guard-main.js in MAIN world, also about:blank/srcdoc/data:/blob:); content.js:7 and :55 send `css` and `guard` in each frame, plus 11 listeners (6 in content.js, 5 in guard-main.js) and one READY/CFG event round trip (content.js:48-56, guard-main.js:155,163). [read]
Cost: Gecko creates a sandbox per extension per window and runs document_start scripts before the frame's parser continues [guess, from how document_start is implemented]; with the first `browser.runtime` use per context adding API setup [guess]. I estimate 1-3 ms per frame on A53, so 20-120 ms of content-process main-thread time on an ad-heavy page with 20-40 live http(s) frames (the $subdocument-blocked ones never load, so they cost nothing). No measurement exists anywhere in the repo (AUDIT-2026-11 s2: "cost on the tablet [N]").
Cheaper:
 1. One message per frame: content.js sends `{k:'hello', h, top}`, the background answers `{css, on, top}` (merge the handlers at background.js:196-217 and :279-290). Saves one IPC + one promise per frame and removes the READY/CFG ordering dance (content.js:48-56).
 2. For non-top frames, skip the CSS half when the frame is tiny (`innerWidth*innerHeight < ~2500`: tracking pixels, cookie-sync frames); keep guard + long-press.
 3. Count first: add `frames` (number of `hello`s) to the `st` message so Diagnostics shows frames per page and the ms the bg spent answering; decide on (2) with numbers.
Risk: (1) low, keep the READY re-push; (2) a player frame that starts at 0x0 and is resized later would miss its site rules (not the guard).
Tests: none for the wiring. content.test.js covers only the long-press; guard.test.js fires the CFG event itself (guard.test.js:36-38). Add a test that loads content.js + guard-main.js with a fake runtime and asserts exactly one message per frame and that CFG reaches the guard.

### J2 - medium - Media sniffer asks for `responseHeaders` on every XHR/fetch/other/sub_frame/main_frame/media response
Evidence: background.js:294 `MEDIA_TYPES`, :303-328 handler, header array loop with `toLowerCase()` per header (L309); registered with `['responseHeaders']` for http/https. [read] REVIEW-V7 P10 lists it as OPEN ("gain unverified"). The extension is in the parent process (s0), so Gecko builds the header array (15-25 small objects) on the parent main thread for each response before the handler can exit; early returns in the handler (L304, L306, L314) do not save that [guess on cost]. SPAs like Proton Mail (the owner's report) make 100+ XHRs per load: ~3-10 ms per page [guess], in bursts while other requests are held.
Cheaper (choose after the counters in s8 show the hit rate):
 a. Zero-risk micro (trims the handler, not Gecko's header build): `if (d.tabId < 0) return;` (nothing to attribute it to); compare `h[i].name.length` (12/13/14) before `toLowerCase()` so most header names are never lower-cased.
 b. Two listeners: (i) `types ['media','object']`, all URLs, with `responseHeaders` (the media element's own loads: few); (ii) `types ['xmlhttprequest','other','sub_frame','main_frame']` with `urls` patterns for the extensions (`*://*/*.m3u8*`, `.mpd`, `.mp4`, ...). Gecko then builds headers only for matching URLs.
 c. Or register (ii) without patterns only for a tab where content.js has seen a `<video>/<audio>` (`filter.tabId`), unregister at `main_frame`.
Risk: (b) misses files that a page fetches through extensionless XHR URLs with a media Content-Type; (c) misses responses that arrive before the player is known. (a) none.
Tests: none. background.test.js:69 stores `listeners.headers` and never calls it.

### J3 - medium [guess on how many sites] - The sniffer reports every distinct URL: segmented sites send one message per segment
Evidence: key is `url|sizeKnown` (background.js:316); `SEGMENT` only excludes by file extension (L297,306), `MEDIA_CT` accepts any `video/` or `audio/` type (L296). Sites that fetch segments by byte range in the QUERY (`...mp4?bytestart=..&byteend=..`, `videoplayback?range=..`) make a new URL per segment. Each becomes a native `media` message (UI-thread task), then `VideoFlow.seen` -> `Io.ui` -> `MediaList.add` (VideoFlow.kt:40-43; O(24) scan, key = URL, MediaList.kt:53-65) and `refreshDownloadButton`; the 24-entry cap (MediaList.kt:45) then evicts real candidates. `SNIFFED_PER_TAB = 300` only bounds the dedupe set, not the messages (L301,324). [read for the mechanism; whether a given site does this: guess]
Cheaper:
 1. Drop small known sizes in JS: Kotlin never shows a FILE under 100 KB (`MediaList.usable`, MIN_FILE = 100 KB, MediaList.kt:86,102). If `size >= 0 && size < 102400` and the type is plain `video/*|audio/*` (not mpegurl/dash), do not send.
 2. Normalise the dedupe key: origin + path + the query minus {range, bytestart, byteend, start, end, sq, seg, segment, rn, rbuf, offset}. Cap distinct URLs per host+path per page (e.g. 8).
Risk: low for (1) (the dropped ones are invisible today); (2) could hide a second real video served from the same script path: keep the cap generous.
Tests: none in web-tests; core has MediaList tests but not this flow.

### J4 - medium-low - DOM media scan: heavy parts run twice per frame, with avoidable allocations
Evidence (content.js:61-97) [read]:
 * Runs after DOMContentLoaded (idle, `timeout:3000`) and AGAIN at `load`+2.5 s (L94-95). With `timeout:3000` it is forced to run after 3 s even if the thread never idles, i.e. possibly in the middle of a heavy script or a fling.
 * Each run: 3 whole-document queries (L75, L77, L79). `querySelectorAll('a[href]')` materialises the NodeList of ALL anchors though only the first 600 are read (L80).
 * Inline scripts (L81-90): `scripts[i].textContent` is read BEFORE the `> 200000` size check (L84-85), so a 1-2 MB inline JSON/state blob is copied into a string (UTF-16 in the DOM -> JS string, 2-6 MB transient) only to be skipped [guess on copy details]. For every script under the limit: three `replace` passes (L87) plus the FIND regex over up to 300 KB total, even when the text mentions no media extension at all.
 * `report()` sends one `dom` message per address, <= 40 per frame (L68-71).
 Estimated 5-15 ms per scan on a mid-size page (3,000 elements, 400 anchors, 150 KB of inline script) [guess], x2, plus smaller scans in every ad frame.
Cheaper:
 1. Before `textContent`: `var tn = s.firstChild; if (!tn || s.childNodes.length !== 1 || tn.length > 200000) continue;` (CharacterData.length is O(1)).
 2. Prefilter: `if (!/\.(?:m3u8|mpd|mp4|webm|mkv|m4v|mov|mp3|m4a|aac|ogg|opus|flac)/i.test(t)) continue;` before the three `replace` calls and FIND (the escaped forms still contain the extension).
 3. Use `document.links` (live collection, no NodeList for 3,000 anchors).
 4. Second scan (at load) only the cheap parts (media elements + meta tags); anchors and inline scripts only once.
 5. In frames where the first query (`video, audio, source[src]`) is empty and the frame is not the top, stop there.
 6. Send the found addresses as ONE `dom` message with an array (today content.js:71 sends one message per address and background.js:203-206 forwards each with its own `send`).
Risk: (1)-(3) none (same results). (4) a player config injected after DCL by a late inline script is found by the user's play (sniffer) instead. (6) needs the same change in background.js.
Tests: none (content.test.js has no scan case). Add one: inline JSON with a slash-escaped (backslash-u002f) m3u8 address, og:video, anchor to .mp4, 41 addresses (cap 40), assert message count.

### J5 - low-medium - UI-thread traffic that can be avoided: duplicate `css` requests, `css` for every iframe host, unbatched `dom`/`vid`
Evidence [read]:
 * `css` handler (background.js:279-290) checks the cache then calls `send`; there is no in-flight map like `cfgWait` (L23). Ten frames from one ad host that start together cause ten native `css` messages and ten Kotlin `cosmeticCss` calls before the first reply fills the cache.
 * Every new iframe host costs one native `css` message although ~99% of hosts have no site rules (site groups: 28,914 hashes; median 1 selector) [read-data]. The list image (KEX1) excludes cosmetics on purpose (ExtImage.kt:11-12).
 * `dom` (<= 40/frame) and `vid` go one message each.
Cheaper:
 1. In-flight dedupe: `cssWait.get(key)` -> same promise (5 lines, copies `askCfg`).
 2. Ship the 28,914 site-group hashes (231 KB) as one more KEX1 section ("only append", ExtImage.kt:22) and answer "no site rules" locally; only hosts with rules, and top frames (generic CSS), go native.
 3. Batch `dom` / `vid` per frame per tick.
Risk: (1) none. (2) image version bump; both halves ship in one APK. (3) low.
Tests: none for css. engine.test.js would need a hash-vector case for the new section.

### J6 - low - Every navigation makes the first sub-resources wait for a native round trip
Evidence: background.js:262-264 deletes the host's cfg at each `main_frame` and :272 returns a Promise until the new answer arrives. [read] The answer normally beats the first bytes over the hotspot, but a cached or fast same-host load (Back, revisit) can issue its render-blocking CSS/JS before it, and the reply path is UI thread -> worker -> UI-thread completion [guess: 10-50 ms when the UI thread is busy].
Cheaper: stale-while-revalidate - keep the previous mode for the new load and apply the fresh answer when it lands; the 60 s TTL (L273) already accepts that staleness for Settings changes. A Shields switch on the page reloads, so only the first requests of that one reload could use the old mode. The planned design (ADBLOCK_ENGINE.md s2: the app pushes settings over a Port) removes the question entirely.
Risk: for one reload after a toggle the first few requests follow the old switch (the tests at background.test.js:155-183 encode today's exact behaviour and would have to change).
Tests: covered (those two tests would fail on purpose).

### J7 - low - guard-main.js: non-cancelling listeners left non-passive; click-time layout and a whole-document query
Evidence [read]:
 * `touchend` and `pointerup` (guard-main.js:141-142) are non-passive capture listeners on window, but `onTap` never calls `preventDefault`. I believe Gecko counts only touchstart/touchmove (and wheel) as APZ-aware [guess; I could not read Gecko 160], so this is insurance, not a known stall.
 * `isOverlay` (L68-83) runs `getBoundingClientRect` + `getComputedStyle` on every trusted click on a link: a synchronous layout flush inside the click, before navigation. Usually the layout is clean; on a page that is animating/lazy-loading it moves the next frame's layout work in front of the tap [guess].
 * `overPlayer` (L56-67) runs `querySelectorAll('video, iframe, canvas, [class*="player" i], [id*="player" i]')` over the whole document, but only for a cross-site link with no text (a banner), so rare.
Cheaper: `addEventListener('touchend', f, {capture:true, passive:true})` and the same for `pointerup`; or drop `touchend` (Gecko 160 has pointer events). Read geometry only after the cheap checks (`a.target`, `crossSiteLink`) have passed.
Risk: none for passive. Dropping `touchend` changes `tap.t` timing by a few ms.
Tests: guard.test.js (20 cases) uses `page.mouse`, which fires pointerup+click, so it would catch a broken pointerup/click path; `touchend` is never exercised (no `touchscreen.tap`).

### J8 - low - Element-hiding cache duplicates the generic CSS per host; CSP risk of the inline `<style>`
Evidence: background.js:278-290 stores `site + generic` per host (cap 200, FIFO): ~4 MB of duplicated generic CSS [read-data for the 18.7 KB]. content.js:9-12 inserts a `<style>` element, which is part of the page DOM, enumerable by page scripts, and parsed per top document. [read] Whether a page's `style-src` CSP blocks a content-script-created `<style>` in Gecko 160 is unknown to me [guess]; a user-origin stylesheet is not subject to page CSP.
Cheaper: have Kotlin return the generic part once (or flag "generic applies") and keep it as ONE string in the background; or register the generic CSS once with `contentScripts.register({css:[{code}], cssOrigin:'user', ...})` as ADBLOCK_ENGINE.md s2 plans (parsed once per process, no message, no DOM node). Test a page with `Content-Security-Policy: style-src 'self'` on the emulator.
Risk: registered CSS races the first visit (uBO falls back to executeScript, ADBLOCK_ENGINE.md s2).
Tests: none.

### J9 - low - Small per-request/instrumentation costs
 * `pageHashes()` (engine.js:210-227) recomputes the page host's suffix hashes (about 5 x 15 `h64` steps + an array) for every request that reaches a domain-restricted rule, i.e. nearly every request (92 of the 93 token-less rules have domain lists) [read-data]. The page host is the same for the whole page: memoise by string (`if (pageHost === lastHost) reuse`). ~3-5 us per request on A53 [guess].
 * `decide()` takes two `performance.now()` per check plus Math and `markStats()` (background.js:243-247). Sample every 8th check; the average stays valid.
 * Gram loop (engine.js:342-349) probes once per URL position (up to 2,048) for only 138 gram rules; long analytics URLs (300-2,000 chars) cost 15-60 us on desktop-class [guess]. A tiny first-byte bitmap would skip most positions.
Risk: none; all covered by engine.test.js (2,200-request verdict equality, bounded-work case, informational speed line at :105-114).

### J10 - low by default, medium for canvas-heavy pages (WebView build) - shields.js
Evidence: `MAX_PIXELS = 4000000` (shields.js:67); `toDataURL`/`toBlob` build a second canvas, `drawImage`, `getImageData`, farble, `putImageData` for every call between 256 and 4 MP (L91-118). A 3 MP photo canvas means ~12 MB canvas + ~12 MB ImageData + two GPU read-backs, tens to hundreds of ms on the Mali [guess] and a memory spike in the renderer. The MutationObserver (L178-190) runs `querySelectorAll('video,audio')` for every added element, but only when Data saver is not OFF (default OFF: Prefs.kt:38, ShieldSettings.kt:121). [read]
Cheaper: lower `MAX_PIXELS` to ~250,000 (fingerprinting canvases are small; photo/editor canvases are content); in the observer skip leaf nodes (`n.firstElementChild === null`) and use two `getElementsByTagName` calls instead of the comma selector.
Risk: a fingerprinting script that draws on a canvas larger than 500x500 would read an unperturbed canvas.
Tests: shields.test.js covers farbling, "tiny canvases are left alone", and observer behaviour (media tamed when added later); nothing for large canvases or cost.

### J11 - opportunity (TASKS R2b), not a defect - force passive on document-level touch/wheel listeners
> **CLOSED 2026-11-12 (research/REVIEW-EXTERNAL-1.md section 9 row 1, read in the 160 source `dom/events/EventListenerManager.cpp`):** Gecko 160 already treats `touchstart` / `touchmove` / wheel listeners on the window, the document, the root element or the body as passive by default, and only non-passive ones delay a scroll; `touchend` listeners never do. No app or extension work is needed for the usual case.
The only real source of scroll-start delay is the page's own non-passive document-level `touchstart`/`touchmove`/`wheel` handlers (ad and analytics scripts): `apz.content_response_timeout` is 300 ms at the default "Smoother" feel (SCROLL.md R2 update). SCROLL.md says the pref `dom.event.default_to_passive_touch_listeners` does not exist in 160; it does not say whether the behaviour is hard-wired [guess either way]. guard-main.js already runs in the MAIN world at document_start in every frame, so ~10 lines can do what Chrome's intervention does: wrap `EventTarget.prototype.addEventListener`; when `type` is touchstart/touchmove/wheel/mousewheel, the target is window/document/documentElement/body, and the options carry no explicit `passive`, add `passive:true`. Cost ~0.2 us per addEventListener call.
Risk: pages that call `preventDefault` there (custom swipe, pull-to-refresh libraries) lose it; Chrome has shipped the same behaviour since 2017.
Check first with a 10-line page on the tablet: a non-passive document `touchstart` that busy-waits 200 ms, then fling; if scroll start is not delayed, Gecko already does it and nothing is needed. The existing scrolltest page (geckoapp/src/main/assets/scrolltest) is the place for it.

## 6. Bugs

### B1 - medium - `cssCache` is never invalidated, and it caches "no CSS" answers
Evidence [read]: background.js:279-290 caches every reply, including `''`, under `t|host`/`f|host`; nothing clears or refreshes it (no hook at `main_frame` L260-266, none in `askCfg`, TTL only for cfg). Kotlin answers `''` when the lists are not loaded yet (`Engine.peek() ?: return ""`, ShieldsBridge.kt:202) or when Shields/cosmetic is off for the host (L204).
Consequences: (a) cold start: the first page's `css` can arrive before the lists load, `''` is cached for that host and element hiding stays off for it until 200 other hosts push it out or the app process dies (the `cfg` path handles this with a provisional generation 0, L36; `css` has no equivalent). (b) Turning Shields OFF for a site you already visited leaves its element hiding ON (the reload hits the cached positive answer); turning it back ON, or switching cosmetic on, can leave it OFF. (c) A list update never reaches cached hosts.
Cheaper: tie each entry to the cfg answer: store the host's `cfgAt` (or a counter bumped when `askCfg` sees a changed mode/generation/flags) with the CSS and refetch on mismatch; never cache `''` while the generation is 0. A cheaper variant is `cssCache.delete('t|'+h)` at each `main_frame` (one native `css` per navigation of a top document). Better: put a "cosmetic applies" bit into the `cfg` flags (ShieldsBridge.kt:136) so the background answers `''` for Shields-off sites without any message.
Risk: low. Tests: none (background.test.js has no css case; add: toggle cfg to `0`, navigate, expect no CSS).

### B2 - low - `tabTop` evicts the oldest-INSERTED tab, not the least recently used
Evidence: background.js:261 `tabTop.set(id,url); if (size > 60) delete(first key)`. `Map.set` on an existing key keeps its original position, and nothing removes closed tabs (no `tabs.onRemoved`). After 60 other tab ids (hibernation re-creates sessions, so ids burn fast) a long-lived tab's entry is dropped although it is in use. [read]
Effect: for that tab `guard` falls back to the frame's own host (`topHost` empty, so an embedded player's "same site" is judged against itself, L201), and `dom`/`pop`/sniffer attribute to the frame URL (L204,210,326), so counters and the pop-up notice can miss the tab. Fix: `tabTop.delete(id); tabTop.set(id,url)`, plus `browser.tabs.onRemoved` cleanup. Risk none. Tests: none.

### B3 - low-medium - guard-main.js runs in about:blank/srcdoc frames, content.js does not
Evidence: manifest.json:11 `match_about_blank:false`, background.js:220 `matchAboutBlank:true, matchOriginAsFallback:true`. [read] In those frames nobody forwards the guard's `note()` events (only content.js listens, content.js:52): blocked-pop-up reports vanish (no notice, no counter), the "Shields off for this site" flag never arrives so the guard stays ON, and `topHost` stays '' so `pageHost()` is `location.hostname` = '' and `sameSite()` is false for any non-AUTH target. Ad creatives built with `document.write` into blank iframes are exactly this case. Pop-ups there are still refused, silently.
Cheaper: for `about:`/`data:`/`blob:` frames have `note()` dispatch on `window.parent.document` inside try/catch (same origin as the creator), or set `match_about_blank:true` for content.js (costs J1 per blank frame). Risk: the first is cross-frame access inside a try; the second adds per-frame cost. Tests: none (guard.test.js installs both scripts with addInitScript, which hides the wiring).

### B4 - low [guess] - the guard is registered at runtime, asynchronously
Evidence: background.js:218-222 `contentScripts.register(...).then(guardState='page guard on')`. [read] The first page opened right after launch (a VIEW intent) can load before the promise resolves and run unguarded; VIDEO_DOWNLOADER.md:81 already prefers a static manifest entry and notes it is untested for GeckoView 160. A static `content_scripts` entry with `"world":"MAIN"` (Firefox >= 128) removes the race and the `guardState` string. Test on the emulator with a cold start straight into a pop-up page. Tests: none possible in web-tests.

### B5 - low [guess] - Diagnostics `slowest` / `over 2 ms` probably read in whole milliseconds
`stats.maxUs` and `slow` come from `performance.now()` in an extension page (background.js:243-246). With `privacy.reduceTimerPrecision` on (GECKO_SPEED s4 notes a 1 ms clamp) and an extension principal that is not the system principal, each check measures 0 or ~1000 us; the AVERAGE stays roughly right because of jitter, but "slowest" and "over 2 ms" (the numbers that decide whether the legacy path is deleted, D1-DESIGN.md "Kill switch") may be meaningless. Cheap check on the tablet: if `mx` is always a multiple of 1000, it is clamped. Fix: time batches (e.g. 64 checks in a row, divide), or accept the average only.

## 7. Regression-test map

| Finding | Test that would catch a regression | Gap |
| - | - | - |
| J1 | none | wiring test with both scripts and a fake runtime |
| J2 | none (`listeners.headers` unused) | call the header listener with fixtures |
| J3 | none | same |
| J4 | none | scan fixture test |
| J5 | none | css message test (cache, in-flight, invalidation) |
| J6 | background.test.js:155-183 | would need to change on purpose |
| J7 | guard.test.js (20 cases, mouse paths) | `touchscreen.tap` case |
| J8 | none | CSP page on the emulator |
| J9 | engine.test.js (verdict equality, bounded work, speed line) | adequate |
| J10 | shields.test.js (behaviour) | no large-canvas or timing case |
| B1-B4 | none | all four |
Playwright runs Chromium, not Gecko: costs, Xray wrappers, parser blocking, and `world: MAIN` registration order can only be checked on the emulator/tablet.

## 8. What to measure first, and a cheap order of work

Add counters to the existing `st` message (they cost a few lines, no new traffic): `frames` (hello/css/guard answered), `hdr` (onHeadersReceived calls) and `hdrHit` (those passing the filter), `media` (messages sent), `dom` (messages sent), `css` (native calls) and `cssDup` (calls whose key was already in flight). One page load of an ad-heavy site and of Proton Mail would show which of J1-J5 matter on this tablet.
Order: B1 (10 lines, a real functional bug), J5.1 (5 lines), J4.1-J4.3 (6 lines), J7 (2 lines), B2 (2 lines), then the counters, then J1.1 / J2 / J3 with numbers in hand, then J11 after its 10-minute check.

## 9. Checked and fine (no action)

* No `setInterval`, rAF, polling, or permanent timers: the four timers (background.js:53,133,145,160) are lazy one-shots. [read]
* No MutationObserver or layout read in a loop in the Gecko extension; the only layout reads are at click and long-press (guard-main.js:60,70-71; content.js:107). [read]
* Blocking handler: one Map lookup per request for the page host and the mode; no per-request message after the engine loads; cached `hostOfPage`; image views are zero-copy; the image pull yields (`later(0)`) between chunks so the parent thread is not held > ~10 ms; the saved IndexedDB copy avoids a pull at launch. [read]
* Counter batching, cosmetic filtering and the pop-up guard are as designed (not re-reported).
