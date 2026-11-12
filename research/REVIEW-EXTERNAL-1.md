# REVIEW-EXTERNAL-1 - outside deep review of Kestrel G (speed, page load, scrolling, downloads) for the MT8168B tablet

Written 2026-11-12 by a separate Claude session (another account, model Fable 5.2) that the owner asked to "do deep research to improve the browser and coding".
Read against the repo at commit `da89ce9` (checkpoint 1103) and re-checked against `a6028f3` (checkpoint 1257, the head when this was finished; branch `ccr-a57c163e-mtq8js`). Line numbers are those of `a6028f3`. **Read-only: nothing in the repo was changed, nothing was pushed, nothing ran on the tablet or an emulator.**

## 0. For the Claude session that receives this file

1. Save it as `research/REVIEW-EXTERNAL-1.md` (your own `research/SPEED4.md`, the LinkMeter note, already exists; this file builds on it in section 5). It contains no absolute home paths and no shell network commands, so the write hooks should accept it. Task ids use the prefix `EX-`, which no other file uses.
2. Per `CLAUDE.md`: copy section 13 (the `[ ]` boxes) into `TASKS.md` first, run `bash tools/ckpt.sh`, then code. Work the ladder in section 1 top-down; every row names its file, its evidence and its proof.
3. It does NOT repeat `GECKO_SPEED`, `GECKO_TUNING_2`, `HW_SPEED`, `SCROLL`, `SPEED2`, `SPEED3`, `SPEED4`, `TYPING`, `DOWNLOADS`, `DOWNLOAD_MANAGERS`, `PERF_KOTLIN`, `PERF_JS`. I read their decisions and looked only for (a) defects in today's code, (b) levers those reports did not find, (c) open `[N]` items I could close with harder evidence.
4. Every pref name below was checked the strict way of `GECKO_TUNING_2` (whole-line match in `strings -a -n4 libxul.so`, or a `pref()` line) against the exact artifact the app ships. Still add each new name to `gecko-prefs.txt` only together with `tools/check-gecko-prefs.sh`.

**How the evidence was gathered (new compared with earlier reports):** this session fetched the shipped engine itself, `org.mozilla.geckoview:geckoview-armeabi-v7a:160.0.20261104181552` from Mozilla's Maven repository (`libxul.so` = 118,043,120 bytes, the size your notes record), and read the C++/JS **source at the git tag `FIREFOX_160_0_RELEASE`** file by file, unsummarised. Earlier reports read Firefox `main` or went through a summarising fetch tool; where they marked something `[N]` for that reason, section 9 gives the exact 160 lines.

Tags: **[V-AAR]** verified in that AAR (`strings`, `javap -c -v`, files inside `omni.ja`) - **[V-SRC]** read in the 160 release source, `file:line` given - **[V-DOC]** official documentation page read on 2026-11-12 - **[V-REPO]** read in this repo at `a6028f3` - **[E]** my estimate or inference - **[N]** not verified. Source keys `S1..` are in section 12.

**In plain words (for the owner):** the earlier research was right that the Firefox engine has no hidden "turbo" switch. What this review adds: two real bugs (the Wi-Fi lock that should keep downloads fast with the screen off does nothing on Android 10, and the "Warm up the top suggestion" setting does nothing at all in this engine version), a tap delay of up to a quarter of a second on desktop-style pages that one setting removes, a new image format (JPEG XL) the engine now asks websites for although this CPU decodes it slowly, a built-in "load the next page early" feature that Mozilla ships switched off, a test that may show the phone helper's proxy is faster than the Fast Wi-Fi VPN for the browser, and proof for several things earlier reports could only guess.

## 1. The ladder: what to do, in this order

Effort S = under an hour, M = half a day, L = more. Each row's proof (how to show it worked without adb) is in the section named in the last column.

| # | Change | Where | Expected effect on this tablet | Effort / risk | Evidence, section |
|---|---|---|---|---|---|
| EX-1 | **Fix the Wi-Fi lock**: use `WIFI_MODE_FULL_HIGH_PERF` on every API level (today API 29+ gets `WIFI_MODE_FULL`, a documented no-op) | `app/.../download/DownloadService.kt` `keepAwake()` | Screen-off downloads stop falling into Wi-Fi power save: steadier speed, fewer stalls [E]. This is the promise of TASKS V1 | S / none | [V-DOC S1] [V-REPO], 2.1 |
| EX-2 | **Make "Warm up the top suggestion" real**: `GeckoWebExecutor.speculativeConnect()` returns without doing anything once the engine is running; dispatch the call onto the Gecko thread, or remove the setting | `BrowserActivity.preconnect()` (line 1147) | The setting (on by default) starts working: DNS + TCP + TLS are done while the owner still types [E 2-3 round trips per typed navigation] | S-M / medium (uses a non-API class; proof needed on the emulator) | [V-AAR] [V-SRC S8], 2.2 |
| EX-3 | **Remove the tap delay**: `doubleTapZoomingEnabled(false)` + a Settings switch "Double-tap to zoom" (default off) | `GeckoEngine.create()` builder | On pages wider than the screen (Desktop site mode, non-responsive sites) every tap is answered up to ~220 ms sooner; no change on mobile-friendly pages | S / low (double-tap zoom gone, pinch stays) | [V-SRC S3-S5] [V-AAR], 3.1 |
| EX-4 | **Stop asking for JPEG XL**: `image.jxl.enabled = false` | `gecko-prefs.txt` | Servers that negotiate formats send WebP/AVIF/JPEG instead of JXL, which this build decodes with a Rust decoder on 4 x A53 [E on cost] | S / none visible | [V-SRC S6] [V-AAR], 3.2 |
| EX-5 | **Measure the Fast Wi-Fi path before trusting "the Courier rests"**: under the VPN, compare pages and a 50 MB download (a) through LinkMeter's VPN and (b) through the Courier CONNECT proxy at the phone's private address | `core/.../net/NetProfile.kt`, Link test | Possibly the largest single gain under Fast Wi-Fi: path (b) uses the tablet kernel's TCP instead of a userspace TCP engine on the weak CPU [E]. Could also be zero: measure | M / low (a switch) | [V-REPO], 5 |
| EX-6 | **Resolve link hosts ahead**: `dom.prefetch_dns_for_anchor_https_document = true` | `gecko-prefs.txt` | The DNS lookup for a link is already done when it is tapped: one round trip less per cross-site navigation [E 50-150 ms here] | S / privacy note (lookups for links never tapped) | [V-SRC S2] [V-AAR], 3.3 |
| EX-7 | **Wi-Fi out of power save while the browser is in front** (Speed lab switch; the Link test decides the default) | new small `WifiBoost` object, `BrowserActivity.onResume/onPause` | Lower and steadier round-trip time on the hotspot hop, mostly for the first packets after a pause [E]; costs battery | S / low | [V-DOC S1], 4.1 |
| EX-8 | **No zoom animation when a field is focused**: `inputAutoZoomEnabled(false)` (A/B) | `GeckoEngine.create()` builder | Removes an animated zoom + repaint at the moment typing starts (the project rule is "no animations"); small fields stay small | S / low | [V-AAR], 3.4 |
| EX-9 | **Download threads at nice 9 instead of 10 while the UI is visible** | `DownloadController.threadPrio()` | Your own H1/H2 finding applied to downloads: nice 10 drops the threads into Android's background group; nice 9 still yields to the UI by weight but may use idle cores. Matters at Fast Wi-Fi speeds, not at 5 Mbit/s [E] | S / low | [V-REPO], 6.2 |
| EX-10 | **Click-to-load for heavy third-party embeds** (video and social iframes) | built-in extension + a Shields row | The largest remaining page-load lever on a CPU-bound device: an embed's scripts never run until tapped [E]. Listed as "Aggressive" in ADBLOCK_ENGINE; this makes it a normal Shields row | M / medium (needs a per-site off) | 4.3 |
| EX-11 | **Keyboard tracking by event, not by polling** | `ui/KeyboardProbe.kt`, `BrowserActivity.typingTick` | Removes 3-10 window-manager calls a second on the UI thread while the address field is focused; the dock follows the keyboard without the 100 ms step | S-M / low | [V-REPO], 2.4 |
| EX-12 | **Speculation Rules prefetch (experimental)**: dry run first, then `dom.speculation_rules.enabled = true` as a Speed lab switch | `gecko-prefs.txt` / Speed lab | On sites that publish speculation rules the next same-site page is fetched when the finger goes down. The code is in 160 but enabled in Nightly only | S / medium (pre-release feature) | [V-SRC S7], 3.5 |
| EX-13 | **Gapless windows for the Gecko fetcher** (request window N+1 shortly before N ends) | `core/.../download/Engine.segmentLoop` | Removes one idle round trip per 4 MiB window: about 1.5 % at 5 Mbit/s, 7-9 % at 30 Mbit/s [E arithmetic] | M / medium (planner invariants) | 6.3 |
| EX-14 | **AVIF only when the link is slow**: drop `image/avif` from the image Accept header under the Fast Wi-Fi profile, if the decode lab says so. Must be a USER-branch pref | `GeckoEngine.applyNetwork` | Less decode CPU when bytes are cheap; more bytes. Only with numbers | S / low | [V-SRC S6], 3.2 |
| EX-15 | **Warm the connection to likely next pages** (top search results; a link under the finger) through the fixed path of EX-2. Never with an injected `<link rel=preconnect>` | extension content script -> app | 2-3 round trips saved on a result tap [E]; about 0.1 s for a touched link [E] | M / privacy trade-off, off in private tabs | [V-SRC S8, S9], 4.2 |
| EX-16 | Small A/B rows for the Speed lab: `network.buffer.cache.size` 16384 -> 32768, `gfx.downloadable_fonts.fallback_delay` 3000 -> 500 | Speed lab | Unknown; cheap to try | S / low | [V-AAR], 3.6 |
| EX-17 | Owner routine, no code: leave the tablet charging with the screen off overnight about once a week | - | Android runs storage TRIM and background app compilation only when idle and charging | - | [V-DOC S10], 7 |

Not in the ladder on purpose: anything already decided in the earlier reports (section 10 lists what I re-checked and would leave alone).

## 2. Defects in today's code

### 2.1 The Wi-Fi lock is a no-op on the tablet (EX-1)
`DownloadService.keepAwake()` creates the lock with `if (Build.VERSION.SDK_INT >= 29) WifiManager.WIFI_MODE_FULL else WifiManager.WIFI_MODE_FULL_HIGH_PERF`, and the comment says HIGH_PERF is deprecated from API 29. The platform documentation says the opposite [V-DOC S1]:

| Mode | Status | What it does |
|---|---|---|
| `WIFI_MODE_FULL` | deprecated in API 29: "non-functional and will have no impact" | nothing |
| `WIFI_MODE_FULL_HIGH_PERF` | deprecated only in API 34, where a request is silently turned into a low-latency lock | keeps Wi-Fi out of power save for low packet latency; stays in force with the screen off or the app in the background; only while connected; no effect if the hardware lacks support |
| `WIFI_MODE_FULL_LOW_LATENCY` (API 29) | current | only with the screen on AND the app in the foreground; the page warns that throughput and scanning may be reduced |

So on the Android 10 tablet (API 29) the service takes the one mode that does nothing, and Wi-Fi power save stays on during a screen-off download. Fix (one expression, plus the comment):
```kotlin
@Suppress("DEPRECATION") // deprecated only from API 34, where the system maps it to LOW_LATENCY by itself (WIFI_MODE_FULL is the no-op: research/REVIEW-EXTERNAL-1.md 2.1)
(applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "kestrel:download")
```
On Android 14+ (the same APK on a newer device) the lock then only works in the foreground with the screen on; nothing better exists there. Proof: Diagnostics line `Wi-Fi lock: HIGH_PERF held=<bool>`; owner test: the same 30 MB file once with the screen on and once locked, compare the average speed on the Downloads screen (before the fix the locked run should be slower or burstier [E]; if it is not, this tablet's driver keeps the radio awake by itself and the fix is only hygiene).

### 2.2 "Warm up the top suggestion" does nothing in GeckoView 160 (EX-2)
The chain, each link verified:
1. [V-REPO] `BrowserActivity.preconnect()` (line 1147) calls `GeckoEngine.executorOrNull()?.speculativeConnect("https://$host/")` on the `Io.exec` thread; `Prefs.preconnect` is true by default.
2. [V-AAR] `GeckoWebExecutor.speculativeConnect(String)` only calls `GeckoThread.speculativeConnect(uri)`, which is `queueNativeCallUntil(State.PROFILE_READY, GeckoThread.class, "speculativeConnectNative", uri)`.
3. [V-AAR] `NativeQueue.queueNativeCallLocked`: if the state is already reached it calls `invokeMethod(...)` at once, on the calling thread; only earlier calls are queued and later flushed on the Gecko thread.
4. [V-AAR] the native `speculativeConnectNative` is annotated `@WrapForJNI(stubName="SpeculativeConnect")` with no `dispatchTo`. Compare `GeckoWebExecutor.nativeFetch` and `nativeResolve`: both carry `dispatchTo="gecko"`, so they hop to the Gecko thread by themselves.
5. [V-SRC S8] `widget/android/nsAppShell.cpp:122-129`: the native starts with `if (!NS_IsMainThread()) { return; }`; the comment explains that an unqueued call means Gecko is already loaded "and we don't really care about speculative connections at this point".

Result: once the engine's profile is ready, a call from the UI thread or any app thread is dropped. Only calls issued BEFORE that point are executed. The typed-suggestion warm-up always happens later, so it never connects. `GECKO_SPEED` decision 5 / section 3.2 and `GECKO_TUNING_2` summary item 4 rest on this API and need the same correction.

Ways out:
- **(A) Dispatch onto the Gecko thread** (works with the 160 classes [V-AAR]: `org.mozilla.gecko.util.XPCOMEventTarget.mainThread()` is `public static`, `execute(Runnable)` is public, `GeckoThread.speculativeConnect(String)` is `public static`):
```kotlin
// GeckoEngine.kt - GeckoView 160 drops speculativeConnect unless it arrives on the Gecko thread (research/REVIEW-EXTERNAL-1.md 2.2)
fun speculativeConnect(url: String) {
    try {
        val gecko = org.mozilla.gecko.util.XPCOMEventTarget.mainThread() ?: return
        gecko.execute { try { org.mozilla.gecko.GeckoThread.speculativeConnect(url) } catch (e: Throwable) { /* only a head start */ } }
    } catch (e: Throwable) { /* engine not up yet, or the internal class moved in a newer GeckoView */ }
}
```
  Both classes live in the internal `org.mozilla.gecko` package, not in the GeckoView API: a version bump may move them. A compile error is the signal; add "speculativeConnect still compiles and still connects" to the monthly bump checklist (TASKS line 19). What the native then does [V-SRC S8, lines 131-145]: it builds a content principal from the TARGET address and calls the IO service's `SpeculativeConnect`; the IO service skips speculative connects while a proxy is configured [V-SRC S9], so under the Courier PAC nothing happens, by design.
- **(B) Calls before the profile is ready do work.** At cold start, directly after `GeckoRuntime.create()`, issue `speculativeConnect` for the search engine's origin and, when the app was launched by a link or will load a restored tab, for that origin. They are queued and run on the Gecko thread. This needs no internal class and is worth doing even if (A) is rejected.
- **(C)** If neither is wanted, remove the Settings switch: a switch that does nothing is worse than none.

Proof on the emulator (the rig of SPEED2 6.1): a local TLS server that logs every ClientHello. Type until the suggestion appears. Today: no ClientHello before Enter. With (A): one ClientHello at suggestion time and none at Enter (the navigation reuses the connection; this second half also settles the partition question of 4.2).

Related [V-SRC S12, `widget/android/WebExecutorSupport.cpp:543-546`]: `GeckoWebExecutor.resolve()` does reach Gecko from any thread, but it resolves with default origin attributes; whether a later navigation hits that cache entry is [N] (network state is keyed by site).

### 2.3 Keyboard probe is polled (EX-11)
[V-REPO] `BrowserActivity.typingTick` (lines 1051-1064) calls `keyboardTop()` every 100 ms while the keyboard moves and every 300 ms once it rests, as long as the address field has focus; on Android 10 each look is `getWindowVisibleDisplayFrame` on the probe popup, a call into the window manager from the UI thread (the thread that also feeds Gecko's vsync and the keyboard path, FREEZE.md).
The probe popup is itself `adjustResize`, so Android re-lays it out whenever the keyboard appears, changes height or leaves. Listen for that instead:
```kotlin
// ui/KeyboardProbe.kt
var onChange: (() -> Unit)? = null
private val layoutListener = android.view.ViewTreeObserver.OnGlobalLayoutListener { onChange?.invoke() }
// attach(), after showAtLocation(...):   v.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
// detach(), before dismiss():            view?.viewTreeObserver?.removeOnGlobalLayoutListener(layoutListener)
// API 30+: window.decorView.setOnApplyWindowInsetsListener { v, insets -> onChange?.invoke(); v.onApplyWindowInsets(insets) }
```
`BrowserActivity`: `kbProbe.onChange = { applyKeyboard(keyboardTop()) }`, and `typingTick` becomes a 1 s safety net (or goes away once the emulator shows the callback fires for show, hide and a height change such as the emoji panel). [N] that the zero-width popup receives a global-layout pass on this Android 10 build; it should, because the visible frame the code reads today only changes through that relayout. Effect: small but on the exact thread and moment the owner complained about; the dock also lands with the keyboard instead of up to 100 ms later.

### 2.4 Statements in the repo that the evidence now contradicts (fix them when the file is touched)
- `DownloadService.kt` comment "HIGH_PERF is deprecated from API 29" (2.1).
- `GECKO_SPEED` decision 5 and 3.2, `GECKO_TUNING_2` item 4, Settings text of "Warm up the top suggestion" (2.2).
- `NetProfile.kt` header: "a second relay on top of the VPN only adds a hop, never speed" is an untested assumption that SPEED2 section 7 itself contradicts (section 5 below).
- `PERF_JS` J11 and TASKS R2b ("passive-by-default touch listeners"): the engine already does this for the usual case (section 9, row 1). No app work is needed there.
- `setPriorityHint`: recommended in six research files, used nowhere in the code. Either add it (3.7) or strike it.

## 3. Engine levers the earlier reports did not have (all names exist in the shipped 160)

### 3.1 The tap delay on pages wider than the screen (EX-3)
What the 160 source does:
- [V-SRC S3] `layout/base/ZoomConstraintsClient.cpp:188-189`: `mAllowDoubleTapZoom = mAllowZoom && StaticPrefs::apz_allow_double_tap_zooming()`; lines 261-268 switch it off again when the CSS viewport, scaled to device pixels, is not wider than the screen.
- [V-SRC S4] `gfx/layers/apz/src/AsyncPanZoomController.cpp:3372-3383` (`OnSingleTapUp`): the tap is sent to the page at finger-up ONLY when double-tap zoom is not allowed (or `touch-action` forbids it); otherwise the code waits for `OnSingleTapConfirmed`.
- [V-SRC S5] `gfx/layers/apz/src/GestureEventListener.cpp:655-672`: that confirmation is a timer of `apz.max_tap_time` (300 ms in the 160 `StaticPrefList.yaml`) counted from the START of the touch.
So on any page whose layout is wider than the screen, a tap that lasts 80 ms reaches the page at 300 ms: about 220 ms of pure waiting, per tap. Affected: every page in "Desktop site" mode, pages with no viewport tag, pages with a fixed wide viewport. Not affected: `width=device-width` pages (most mobile sites).
Fix: `GeckoRuntimeSettings.Builder.doubleTapZoomingEnabled(false)` [V-AAR: the method exists; the class manages `apz.allow_double_tap_zooming` with default true]. Use the typed setter, not `gecko-prefs.txt`: it is a GeckoView-managed name (GECKO_SPEED gotcha c). Pinch zoom is untouched (`mAllowZoom` is separate). Offer it as Settings > "Double-tap to zoom", default OFF, live through `runtime.settings`.
Proof: a "Tap lab" page in `assets/diag` with `<meta name="viewport" content="width=1200">` and a button that stamps `touchend` and `click`: the gap is about 300 ms minus the tap length before, a few ms after. Run it on the emulator first; it is deterministic, not a feel question.

### 3.2 Image formats: JPEG XL is advertised and decodable in 160; AVIF cannot be switched off (EX-4, EX-14)
- [V-SRC S6] `netwerk/protocol/http/nsHttpHandler.cpp:221-238` builds the image Accept header as `image/avif,` + (`image/jxl,` when built with JXL and `image.jxl.enabled`) + `image/webp,image/png,image/svg+xml,image/*;q=0.8,*/*;q=0.5`.
- [V-SRC S2] `image.jxl.enabled` is `true` at the 160 tag. [V-AAR] the shipped armv7 `libxul.so` contains `nsJXLDecoder` and `jxl-rs` strings, so the decoder is really in the build.
- [V-AAR] `image.avif.enabled` does not exist (your MISSING list is right): AVIF decoding cannot be disabled by pref.
Why it matters here: CDNs choose the format from this header. JXL and AVIF are smaller but cost more CPU per pixel than JPEG or WebP, and image decode is pure CPU in Gecko (SCROLL Q5). I found no decode numbers for the Rust JXL decoder on ARMv7 [N], which is the reason to switch it off rather than tune it: nothing on this device benefits from a format that only saves bytes when the alternative is already AVIF.
- **Now:** `image.jxl.enabled = false` in `gecko-prefs.txt`. It is `mirror: always`, and the handler rebuilds the header when it changes (lines 1974-1990), so it is also a live switch. Read-back in Diagnostics; one logged `Accept` header of an image request (extension `onSendHeaders`, debug only) proves the header.
- **Later, with numbers:** removing AVIF from the header. Trap [V-SRC S6, lines 1977-1990]: `image.http.accept` is honoured only when it has a USER value (`Preferences::HasUserValue`). A line in `gecko-prefs.txt` or a `PREF_BRANCH_DEFAULT` set is silently ignored. Use `PREF_BRANCH_USER` with the value `image/webp,image/png,image/svg+xml,image/*;q=0.8,*/*;q=0.5` and `clearGeckoUserPref` to leave the mode (the one name where the "never the USER branch" rule of GECKO_SPEED must be broken, so keep it in one function). `<picture>` sources are chosen by decoder support, not by the header, and stay AVIF.
- Decision rule [E]: a 100 KB JPEG against a 60 KB AVIF saves 64 ms of transfer at 5 Mbit/s but only 11 ms at 30 Mbit/s. If the decode lab (section 8) shows AVIF costing more than about 50 ms extra per typical image, AVIF loses under the Fast Wi-Fi profile and roughly ties under the cap. So tie the header to `NetProfile` (Fast Wi-Fi: no AVIF; capped hotspot: default), never to a global default.

### 3.3 DNS for links before they are tapped (EX-6)
[V-SRC S2] at the 160 tag: `dom.prefetch_dns_for_anchor_https_document` = false, `dom.prefetch_dns_for_anchor_http_document` = true, `network.dns.disablePrefetchFromHTTPS` = false, `network.dns.prefetch_via_proxy` = false. So on https pages (nearly all) the engine does not resolve the hosts of links ahead of time, and one pref turns that on.
Cost: lookups for links that are never tapped (the resolver sees them; bytes are negligible). Under the Fast Wi-Fi profile lookups go to the phone's resolver in one Wi-Fi hop; with DoH they ride the open DoH connection; behind the Courier PAC nothing is prefetched (`prefetch_via_proxy` false), which removes the objection recorded in SPEED2 6.3. Make it follow the "Save data" switch (off when a saver level is on).
Proof: Diagnostics > Page loads, `domainLookupEnd - domainLookupStart` of a first visit reached by tapping a link (near 0 when it worked). [N] whether the prefetched entry is reused across the site-keyed DNS cache for a top-level navigation: this measurement answers it; if the lookup time does not drop, delete the line.

### 3.4 Zoom-to-input (EX-8)
[V-AAR] GeckoView manages `formhelper.autozoom` (default true; `geckoview-prefs.js` also sets it true) and `greprefs.js` has `apz.zoom-to-focused-input.enabled` true: focusing a small field zooms the page to it, with an animation. `GeckoRuntimeSettings.Builder.inputAutoZoomEnabled(false)` exists [V-AAR]. For AA3-2 (typing lag) this is the cheapest A/B there is: it removes a zoom animation, a repaint and a keyboard resize that all start in the same 300 ms. Risk: a small field on a desktop-style page stays small until the owner pinches.

### 3.5 Speculation Rules prefetch exists in 160, switched off (EX-12)
[V-SRC S7]:
- `dom/base/SpeculationRules.cpp` implements prefetch candidates with eagerness levels: immediate when the rules are processed, eager / moderate after hover timers, conservative on `PointerDown` (lines 466-472). `dom/base/nsContentSink.cpp:242-251` reads the `Speculation-Rules` response header, `dom/script/ScriptLoader.cpp:2012-2044` parses `<script type="speculationrules">`.
- `StaticPrefList.yaml` (160): `dom.speculation_rules.enabled` = Nightly only; `same_origin_only` true; `max_records_per_document` 20; `max_body_bytes` 5242880; `record_expiry_ms` 300000; `wait_timeout_ms` 2000; `eager_hover_delay_ms` 10; `moderate_hover_delay_ms` 200; `prefetch.dry_run` false, described there as the control branch of Mozilla's own prefetch experiment.
Meaning: a navigation prefetch that is native, same-site only and capped is compiled into the shipped engine, and Mozilla is measuring it. On a site that publishes rules, the next page's HTML is requested when the finger goes down (or earlier), which on a 60-100 ms round-trip link is most of the wait before the first byte.
How to trial it without betting the browser on pre-release code (your own standard in GECKO_TUNING_2 3.4 for the Nightly-only script cache): Speed lab switch, default OFF. Step 1 `enabled=true` + `prefetch.dry_run=true` (candidates are recorded, nothing is fetched) for a few days of normal use; step 2 real prefetch; keep it only if Page loads shows faster same-site navigations and nothing breaks. All nine pref names EXIST in the AAR. Risks: pre-release code path, extra requests (bounded by the caps above), sites that count a prefetch as a view. [N] which sites the owner uses publish rules; WordPress core has shipped them by default since 2025 [N, from memory].

### 3.6 Two cheap A/B rows (EX-16)
- `network.buffer.cache.size`: [V-AAR] `greprefs.js` says 32768 but GeckoView's own `geckoview-prefs.js` halves it to 16384 (count 24). On a link that now reaches 30 Mbit/s, 32768 halves the number of buffer hand-offs per megabyte in the parent process. Unknown effect; one line.
- `gfx.downloadable_fonts.fallback_delay`: [V-AAR] 3000 ms (`_short` 100). Text set in a web font stays invisible for up to 3 s while the font file queues behind everything else on a slow link. GECKO_SPEED listed 1000 as an experiment; on this link 500 is the more useful first value (text appears in the fallback font, then swaps).

### 3.7 `setPriorityHint` (optional)
[V-AAR, `modules/GeckoViewContent.sys.mjs:263-268`] the call sets `remoteTab.priorityHint`; [V-SRC S2] `dom.ipc.processPriorityManager.enabled` is true and `backgroundGracePeriodMS` is 3000 on Android. With one content process and `setActive(true)` on the visible tab the hint changes nothing while a page is in front. Its only use here is to keep the content process at foreground priority while the page is covered by the Home overlay or the tab grid, or plays audio from the background. Three lines in `show()`: `PRIORITY_HIGH` for the selected tab's session, `PRIORITY_DEFAULT` for the one it replaces. Low value; do it or delete it from the research files.

## 4. New techniques for the app and the built-in extension

### 4.1 Wi-Fi out of power save while the browser is in front (EX-7)
In 802.11 power save the tablet's radio sleeps between beacons and the phone buffers frames for it; drivers wake the radio for a short while after traffic. A tap after a few seconds of reading therefore starts from a sleeping radio [E]. `WIFI_MODE_FULL_HIGH_PERF` is documented to keep Wi-Fi out of power save for low packet latency [V-DOC S1] and, unlike the low-latency mode, is not documented to cost throughput.
```kotlin
// net/WifiBoost.kt - a hint only: the radio stays awake while the browser is in front (research/REVIEW-EXTERNAL-1.md 4.1)
object WifiBoost {
    private var lock: WifiManager.WifiLock? = null
    @Suppress("DEPRECATION")
    fun set(ctx: Context, on: Boolean) {
        try {
            if (on) {
                val l = lock ?: (ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)
                    ?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "kestrel:front")?.also { it.setReferenceCounted(false); lock = it }
                if (l != null && !l.isHeld) l.acquire()
            } else lock?.let { if (it.isHeld) it.release() }
        } catch (e: Exception) { /* never worth a crash */ }
    }
}
```
`BrowserActivity.onResume`: `WifiBoost.set(this, prefs.wifiBoost)`; `onPause`: `WifiBoost.set(this, false)`. `WAKE_LOCK` is already in the geckoapp manifest [V-REPO].
Proof before it becomes a default: Link test, 50 round trips to the gateway (the phone) with a 1.5 s pause between them so the radio can doze, once with and once without the lock; compare the median and the 90th percentile. Under about 5 ms difference: delete the feature. Battery cost goes into the Settings sentence.

### 4.2 Warming connections for navigations (EX-15), and why not `<link rel="preconnect">`
Needs EX-2 path (A). Three facts shape the design:
- [V-SRC S13, `docshell/base/nsDocShell.cpp:12788-12823`] Gecko's own link warm-up (`OnOverLink`) builds the principal from the LINK's address, with the comment that a cross-origin navigation needs the origin of the URL, not of the current page. Network state is keyed by site, so a `<link rel="preconnect">` injected into page A opens a socket in A's partition [E] that a top-level navigation to B does not use. **Do not inject preconnect links for navigations**; use the app-level call, whose principal is the target [V-SRC S8].
- That hover path is gated on https pages by `network.predictor.enable-hover-on-ssl` (exists [V-AAR], false in 160 [V-SRC S2]). On a touch screen hover is set at the tap itself, so it gives no head start. Not worth switching on.
- [V-SRC S8 lines 148-162, S6 lines 2423-2433] when the app goes to the background, Gecko closes its idle connections (and sends itself a heap-minimize memory pressure). Every return to the browser starts with cold connections.
Triggers, by value:
1. **On return to the front** after more than a few seconds away: warm the origin of the tab in front (one call in `onResume`).
2. **Search results**: on the default search engine's result page the content script sends, once at idle, the origins of the first three result links over the existing native port; the app warms them. Gain: DNS + TCP + TLS, 2-3 round trips, at the tap [E]. Cost: those three sites see a TLS handshake from the phone's address before any tap, so it belongs under the existing "Warm up" switch and is off in private tabs.
3. **Finger down on a cross-site link** (head start = the length of the tap, about 0.1 s [E]):
```js
// content.js, top frame only (ES5). Passive + capture: Gecko never waits for a passive touch listener (section 9, row 1).
(function () {
  if (window.top !== window) return;
  var sent = {}, n = 0;
  window.addEventListener('touchstart', function (e) {
    var t = e.target, hops = 0;
    while (t && hops++ < 6 && !(t.nodeType === 1 && t.tagName === 'A' && t.href)) t = t.parentNode;
    if (!t || t.nodeType !== 1 || t.tagName !== 'A') return;
    var u; try { u = new URL(t.href, location.href); } catch (x) { return; }
    if (u.protocol !== 'https:' || u.origin === location.origin || sent[u.origin] || n >= 4) return;
    sent[u.origin] = 1; n++;
    try { browser.runtime.sendMessage({ t: 'warm', o: u.origin }); } catch (x) { }
  }, { capture: true, passive: true });
})();
```
Limits in the app: one warm-up per origin per 30 s, four per page, none in private tabs (GECKO_SPEED 3.2), none while `pagesViaProxy` (the engine skips them behind a proxy anyway [V-SRC S9]).

### 4.3 Click-to-load for heavy embeds (EX-10)
Third-party video and social frames are the most expensive things left on a page once ads and trackers are gone: each brings its own player scripts, which run on the single content-process main thread whether or not the owner ever presses play [E; I have no measured figure for this CPU]. uBlock Origin ships this idea as a redirect resource, the `click2load.html` your ADBLOCK_ENGINE note mentions.
Design for the built-in extension (fits D1 and the `redirect=` plumbing AA3-3b is building):
- A short built-in host+path list, matched only for `sub_frame` requests that are third-party to the page: YouTube and youtube-nocookie `/embed/`, `player.vimeo.com`, Dailymotion `/embed/`, Facebook `/plugins/`, Instagram `/embed`, TikTok `/embed`, Spotify `/embed`, SoundCloud `/player`, Disqus `/embed`, Google Maps `/maps/embed`.
- `onBeforeRequest` answers `{ redirectUrl: browser.runtime.getURL('embed.html') + '#' + encodeURIComponent(d.url) }`. `embed.html` is an extension page (it must be added to `web_accessible_resources`; the manifest has none today [V-REPO]) showing a dark box, the host name and "Tap to load". The tap tells the background script to allow that address in that tab once, then does `location.replace(originalUrl)`.
- Shields row "Embeds wait for a tap", default ON, per-site OFF in the existing Shields store, global OFF in Settings.
- [N] to prove on the emulator: a frame redirected to a `moz-extension:` page renders inside a third-party page under GeckoView 160; the allow-once flag survives the reload of the frame; a page that sizes the frame by script still looks right.
- Measure with Page loads on a page with three video embeds: `loadEventEnd` and the request count, before and after.

### 4.4 Reader view (optional, L)
Mozilla's Readability.js (Apache-2.0 [N: licence and size from memory]) injected only when the owner taps a "Reader" menu row costs nothing until used and turns the heaviest article pages into plain text and images. Lower priority than everything above; record the licence next to the filter lists if it is added.

## 5. The Fast Wi-Fi path: one more run for the SPEED4 test (EX-5)
Facts: [your SPEED4, read in LinkMeter] the VPN routes public addresses only, so private addresses stay outside it; the tablet side is a TCP engine in the app (`TunStack`), the phone side a relay. [V-REPO] `NetProfile.effective()` returns `helperMode = 0, pagesViaProxy = false` under any VPN, and its header says a second relay "only adds a hop, never speed". SPEED2 section 7 already noted that the Courier proxy keeps working under the VPN because private addresses bypass it.
Put together [E]: with Fast Wi-Fi on, Gecko -> Courier CONNECT proxy at the phone's private address does not enter the tunnel at all. It is one relay INSTEAD of the tunnel, not on top of it: tablet kernel TCP -> Wi-Fi -> Courier -> the phone's own stack. What differs from the tunnel for the browser's traffic: no per-packet work of a TCP engine written in Java on the four A53 cores (every packet through the VPN descriptor, state machines and copies in the app). SPEED4's arithmetic shows the tunnel's windows are not the limit; it says nothing about its CPU cost on this tablet, and nobody has a number.
So add a third run to the SPEED4 section 5 test (same three sites, same 50 MB file): (1) LinkMeter off, (2) Fast Wi-Fi on, **(3) Fast Wi-Fi on and pages + downloads through the Courier proxy**. For (3) a Network-lab switch "Under Fast Wi-Fi keep the phone helper" lets `NetProfile.effective` keep `helperMode` / `pagesViaProxy` when `vpn` is true. Decision: if (3) beats (2) by more than about 15 % on page times or MB/s, the tunnel profile should prefer the proxy for the browser while the VPN keeps serving every other app; if not, delete the switch. Either way the header comment should then say "measured". It needs the Courier running on the phone, so even a win is the owner's choice (AA2 keeps the Courier off by default).
On SPEED4's known unknown 4 (connections opened before Fast Wi-Fi came up): two engine behaviours make the usual order of actions safe [V-SRC]. (a) `nsHttpHandler.cpp:2423-2433`: when the app goes to the background Gecko closes its idle connections; the owner has to leave Kestrel to switch Fast Wi-Fi on in LinkMeter, so the warm connections are gone before they return. (b) `nsHttpHandler.cpp:2404-2422`: on a network link change Gecko prunes dead connections, closes those without traffic after `network.http.network-changed.timeout` (5 s [V-AAR]) and clears its list of hosts excluded from HTTP/3. [N] whether GeckoView's `GeckoNetworkManager` (it registers a `NetworkCallback` [V-AAR]) reports a VPN coming up as a link change. The SPEED4 5.4 test still confirms it, but no new mechanism should be built first.

## 6. Downloads: additions to M1-M10 (nothing here repeats DOWNLOAD_MANAGERS)

- **6.1 The Wi-Fi lock** (2.1, EX-1). The only confirmed defect in the download path.
- **6.2 Thread priority while the browser is visible (EX-9).** [V-REPO] `DownloadController.threadPrio()` (line 101) returns `THREAD_PRIORITY_BACKGROUND` (nice 10) while `ProcessLife.uiVisible`. Your HW_SPEED H1 established that nice 10 and above also moves a thread into Android's background scheduling group (few cores, small share), which is why Gecko's threads were lifted. The same reasoning applies here: use `Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE` (nice 9) for the control thread and the workers. They stay in the foreground group, still lose every contest with the UI (scheduler weight 137 against 1024 for a normal thread [E, Linux weight table]) but may use cores the UI leaves idle. No effect at 5 Mbit/s; it matters when the tunnel delivers 3-5 MB/s through TLS and three workers. K7 (the rate limit while a page loads) stays as the real protection. Proof: one download with the browser in front and the page idle, MB/s before and after.
- **6.3 Gapless windows (EX-13).** [V-REPO] `Engine.segmentLoop` requests the next window only after `pump` has returned, so every window boundary costs one idle request round trip. The Gecko fetcher's windows are 4 MiB, the plain fetcher's 8 MiB. Arithmetic [E] for a 100 ms round trip: at 5 Mbit/s a 4 MiB window lasts 6.7 s (1.5 % lost); at 30 Mbit/s it lasts 1.1 s (8 % lost). First step, almost free: write the gap into the job's ring log (time from the last byte of window N to the first byte of N+1). If the owner's Fast Wi-Fi run shows the gap: either (i) under the tunnel profile raise the Gecko window to 8 MiB and hold that fetcher to two workers (the Java-side queue is unbounded after the first read [V-AAR], so the worst case is window x workers of heap), or (ii) request window N+1 when N has less than about two round trips of data left, on a helper task, and hand the opened response to the worker. (i) is one constant; (ii) touches the planner's invariants and needs the T0 fake server first.
- **6.4 Socket reuse across windows (your open measurement, G7).** [E, from the OkHttp-2-based implementation as I remember it; not re-read] Android's `HttpURLConnection` hands the socket back to its pool at the moment the response body has been read to its end; a `disconnect()` after that is harmless, a `disconnect()` before that closes the socket. [V-REPO] `pump` returns at the end of a window with the body fully read and `quiet()` then calls `abort()` and `close()`, so windows that end normally should already reuse their socket, and a window cut short by a planner split correctly does not. A JVM or Robolectric test proves nothing here (a different `HttpURLConnection`); count accepted sockets on a local server from the emulator: expected one per worker, not one per window.
- **6.5 Facts about the Gecko fetcher that DOWNLOAD_MANAGERS left open.** [V-AAR] `GeckoInputStream`: `Support.resume()` is called once, on the first read, and `appendBuffer` only queues: there is no continuing back-pressure, so the bounded windows are right. [V-SRC S12] `WebExecutorSupport.cpp:392` sets request headers with merge = false (an `Accept-Encoding: identity` replaces the default); lines 460-491: the request runs with the system principal and the regular cookie jar unless `FETCH_FLAGS_ANONYMOUS` or `FETCH_FLAGS_PRIVATE` is given, so the profile's cookies go out as the code comment claims. [N] the reported final address after redirects.
- **6.6 Read coalescing** (part of M9, not urgent): `body.read` on Android returns one buffer segment or TLS record at a time, so the 64 KiB buffer is rarely filled and every read is one `pwrite`. Filling the buffer while `body.available() > 0` cuts system calls severalfold; at 5 MB/s that is still only a few percent of one core [E]. Do it with M9.
- **6.7 Expectation for the M5 benchmark on the capped hotspot** [E]: a carrier cap is a policer on the sum of all flows; a second connection can win back some of the sawtooth a single flow loses to the policer but never passes the cap, and the third adds nothing. With `growGain` 1.15 the hill-climb will usually and correctly stay at one or two.

## 7. Outside the app (EX-17, owner routine)
[V-DOC S10] Android's periodic storage TRIM is an idle job: the framework comment describes it as depending on the device being awake, idle and charging in the night window, and forces it at the next reboot when it has been skipped for long. Background compilation of apps from their usage profiles has the same idle + charging condition (your HW_SPEED, H9). A tablet that is used on battery and then switched off gets neither: flash writes slow down as the eMMC fills with untrimmed blocks, and app code stays interpreted or JIT-compiled.
Routine: about once a week leave the tablet on the charger overnight with the screen off. H9's baseline profile only pays after such a night. If adb is ever available (a computer; possibly a phone acting as USB host [N]) three one-time commands are worth it [N, from memory]: `cmd package compile -m speed -f app.kestrel.browser.gecko`, `sm fstrim`, and `dumpsys gfxinfo app.kestrel.browser.gecko` after a scroll. Nothing above depends on adb.

## 8. Measurements to add
| Lab | What it shows | Decides |
|---|---|---|
| Screen-off download run (owner) | average speed locked vs unlocked | EX-1 |
| ClientHello log on the emulator rig | whether a warm-up connects, and whether the navigation reuses it | EX-2, EX-15 |
| Tap lab page (viewport 1200 px, `touchend` -> `click` gap) | the 300 ms timer, before and after | EX-3 |
| Decode lab (one photo as JPEG / WebP / AVIF / JXL in `assets/diag`, `img.decode()` timed five times each) | ms per megapixel per format on the real CPU | EX-4, EX-14 |
| Page loads: lookup time of a first visit reached through a link | whether link DNS prefetch reaches the navigation | EX-6 |
| Link test with 1.5 s pauses, lock on / off | round-trip median and 90th percentile | EX-7 |
| Three-way Fast Wi-Fi run (section 5) | tunnel vs proxy for the browser | EX-5 |
| Window gap in the download log | lost time per window | EX-13 |

## 9. Open questions of earlier reports that the 160 source or the AAR now settle

| # | Was open in | Answer | Evidence |
|---|---|---|---|
| 1 | PERF_JS J11, TASKS R2b, SPEED3 6.3 ("force passive touch listeners", "needs a ten-minute check on the tablet") | Only NON-passive `touchstart`, `touchmove` and wheel listeners count as scroll-blocking. And Gecko 160 already treats `touchstart` / `touchmove` / wheel listeners as passive by default when they sit on the window, the document, the root element or the body. So `touchend` listeners (guard-main.js) never delay a scroll, and J11 is already done by the engine for the usual case; what remains are listeners that explicitly pass `passive:false` or sit on inner elements, which an extension cannot safely rewrite | [V-SRC S11] `dom/events/EventListenerManager.cpp:2262-2277` and `878-914` |
| 2 | GECKO_TUNING_2 section 6 ("integer-GB gate at 1.9 GB") | The gate compares whole gigabytes with `<=`, under the comment "Be conservative on machines with 2GB or less": the tablet takes the on-demand delazification branch | [V-SRC S14] `dom/script/ScriptLoader.cpp:5376-5380` |
| 3 | DOWNLOAD_MANAGERS open measurement 5 (does the Gecko fetcher honour our headers and send cookies) | Headers replace the defaults (merge = false); system principal; regular cookie jar unless the anonymous or private flag is set | [V-SRC S12] `widget/android/WebExecutorSupport.cpp:392`, `460-491` |
| 4 | `GeckoFetcher.kt` comment ("no back-pressure") | Correct in 160: one `resume()` at the first read, then the native side fills the Java queue freely | [V-AAR] bytecode of `GeckoInputStream.read` / `appendBuffer` |
| 5 | HW_SPEED ("`processPrelaunch.lowmem_mb` 4096 limits preallocation") | At or below 4096 MB the number of spare content processes is 1, not 0: `warmUp()` is not fighting a gate | [V-SRC S15] `dom/ipc/PreallocatedProcessManager.cpp:172-177` |
| 6 | GECKO_SPEED decision 5, GECKO_TUNING_2 item 4 (app preconnect) | `speculativeConnect` is dropped once the engine runs unless it arrives on the Gecko thread (2.2) | [V-AAR] [V-SRC S8] |
| 7 | SPEED4 5.4 (old connections after a network change) | Link change: prune + close connections without traffic after 5 s. App to background: idle connections closed, plus a heap-minimize memory pressure | [V-SRC S6] `nsHttpHandler.cpp:2404-2433`; [V-SRC S8] `nsAppShell.cpp:148-162` |
| 8 | several ("is there a newer GeckoView API for speed") | The API changelog up to its newest heading (v161) adds nothing for speed after v150 (runtime no longer starts a child process by itself: `warmUp()` is required, which the app calls). v144 added `GeckoSession.flushSessionState()`, unused in the app: calling it for the front tab in `onPause` makes the saved tab state current before Android can kill the process (K1/K2 territory) | [V-DOC S16] [V-REPO] |
| 9 | "compression dictionaries", "speculation rules" as possible technologies | `network.http.dictionaries.enable` and `dom.speculation_rules.enabled` are both Nightly-only in 160; the second is offered as an experiment in 3.5, the first has no user-visible gain worth a pre-release switch | [V-SRC S2] |

A consequence of row 7 worth knowing: every time the owner leaves the browser, Gecko empties what it can (decoded images, and as far as I know the back/forward page cache, which listens to memory pressure [N, not re-read]). A Back press after an app switch reloads from the network or the disk cache; the Back picture of alpha22 is the right cover for it, and trigger 1 of section 4.2 removes the handshake part.

## 10. Checked and found right: leave alone
- Build: R8 with the extra flags, `armeabi-v7a` only, legacy packaging for the 118 MB library, `profileinstaller` only [V-REPO `geckoapp/build.gradle.kts`].
- Download engine: one shared descriptor with positional writes, preallocation with fallbacks, the checkpoint order (snapshot, sync, then save) [V-REPO `Engine.checkpoint`, `MediaStoreSinks`]. The single engine-wide lock is not a bottleneck at these rates (a few hundred short acquisitions a second [E]): do not split it.
- `engine.js`: 32-bit integer maths with `Math.imul`, no BigInt [V-REPO]. Right for a 32-bit JIT.
- `network.http.pacing.requests.*` (on, 80 per second, burst 10 [V-AAR]), `network.http.speculative-parallel-limit` (20 in GeckoView), `nglayout.initialpaint.delay` (5): defaults are sensible; no evidence for a change.
- `network.predictor.enable-hover-on-ssl`: exists, useless on touch (4.2).
- Everything GECKO_SPEED / GECKO_TUNING_2 rejected stays rejected; I found nothing that reverses one of those verdicts.

## 11. Limits of this review
- No number in this file was measured on the tablet or an emulator. Effects marked [E] are arithmetic or mechanism, not observation.
- I read the engine's code paths named above, not the whole engine; "exists" for a pref means the strict name check, "default" means the 160 `StaticPrefList.yaml` or the shipped pref files.
- I read the verdict sections of DOWNLOADS, DOWNLOAD_MANAGERS, PHONE_OFFLOAD, ADBLOCK_ENGINE, VIDEO_DOWNLOADER, FREEZE and ENGINES, not every line; SPEED4 and TYPING arrived while this was being written and were read in full. If something here contradicts a line I missed, the repo's own measured result wins.
- I did not read LinkMeter's code; section 5 rests on your SPEED4 notes.
- The partition statements in 4.2 and 3.3 (which cache or socket a navigation reuses) are [E]; the ClientHello and lookup-time proofs settle them.

## 12. Sources
Base for S2-S9 and S11-S15: `https://raw.githubusercontent.com/mozilla-firefox/firefox/FIREFOX_160_0_RELEASE/` + the path given.
- S1 Android `WifiManager` reference: https://developer.android.com/reference/android/net/wifi/WifiManager
- S2 `modules/libpref/init/StaticPrefList.yaml`
- S3 `layout/base/ZoomConstraintsClient.cpp` - S4 `gfx/layers/apz/src/AsyncPanZoomController.cpp` - S5 `gfx/layers/apz/src/GestureEventListener.cpp`
- S6 `netwerk/protocol/http/nsHttpHandler.cpp`
- S7 `dom/base/SpeculationRules.cpp`, `dom/base/nsContentSink.cpp`, `dom/script/ScriptLoader.cpp`
- S8 `widget/android/nsAppShell.cpp` - S9 `netwerk/base/nsIOService.cpp`
- S10 AOSP, "Be increasingly aggressive about fstrim if it isn't being run": https://android.googlesource.com/platform/frameworks/base/+/4f868ed and `MountServiceIdler.java` in the same tree
- S11 `dom/events/EventListenerManager.cpp` - S12 `widget/android/WebExecutorSupport.cpp` - S13 `docshell/base/nsDocShell.cpp` - S14 `dom/script/ScriptLoader.cpp` - S15 `dom/ipc/PreallocatedProcessManager.cpp`
- S16 GeckoView API changelog: https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/doc-files/CHANGELOG
- S17 the artifact: `https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview-armeabi-v7a/160.0.20261104181552/geckoview-armeabi-v7a-160.0.20261104181552.aar` (`classes.jar`, `jni/armeabi-v7a/libxul.so`, `assets/omni.ja`)
- S18 Bugzilla 1629111 (the tap delay exists whenever double-tap zoom is allowed): https://bugzilla.mozilla.org/show_bug.cgi?id=1629111

## 13. Block for TASKS.md (copy as is)
```
## External review 2026-11-12 (research/REVIEW-EXTERNAL-1.md) - work top-down
- [ ] EX-1 Wi-Fi lock: WIFI_MODE_FULL_HIGH_PERF on every API level in DownloadService.keepAwake (WIFI_MODE_FULL is a no-op from API 29); fix the comment; Diagnostics line; owner screen-off run
- [ ] EX-2 "Warm up the top suggestion" never connects in GeckoView 160: (A) dispatch GeckoThread.speculativeConnect on the Gecko thread via XPCOMEventTarget.mainThread(), prove with the ClientHello rig; (B) queue the search origin + launch origin before the profile is ready; or (C) remove the switch. Correct GECKO_SPEED 5/3.2, GECKO_TUNING_2 item 4, Settings text
- [ ] EX-3 doubleTapZoomingEnabled(false) + Settings "Double-tap to zoom" (default off); Tap lab page in assets/diag
- [ ] EX-4 image.jxl.enabled = false in gecko-prefs.txt (+ check script, read-back, one logged image Accept header in debug)
- [ ] EX-5 Network lab switch "Under Fast Wi-Fi keep the phone helper" + third run of the SPEED4 section 5 test; then correct the NetProfile header either way
- [ ] EX-6 dom.prefetch_dns_for_anchor_https_document = true (off with Save data); keep only if the lookup time of link navigations drops
- [ ] EX-7 WifiBoost (HIGH_PERF lock while in front) as a Speed lab switch; Link test with 1.5 s pauses decides the default
- [ ] EX-8 inputAutoZoomEnabled(false) as an A/B for AA3-2
- [ ] EX-9 download control thread and workers at nice 9 (not 10) while the UI is visible
- [ ] EX-10 click-to-load embeds: embed.html (web_accessible_resources), sub_frame redirect for the built-in embed list, allow-once per tab, Shields row default on, per-site off; emulator proof; Page loads before/after
- [ ] EX-11 KeyboardProbe.onChange from a global-layout listener; typingTick becomes a 1 s safety net or goes
- [ ] EX-12 Speed lab: speculation rules, dry run first (dom.speculation_rules.enabled + prefetch.dry_run), then real; default off
- [ ] EX-13 log the gap between download windows; if it shows under Fast Wi-Fi: 8 MiB Gecko windows with 2 workers under the tunnel profile, or pre-opened windows
- [ ] EX-14 Decode lab (JPEG/WebP/AVIF/JXL); only if AVIF is slow: image.http.accept without avif on the USER branch under the Fast Wi-Fi profile, cleared on leaving it
- [ ] EX-15 (after EX-2) warm the front tab's origin on resume; top three search results at idle; finger-down on a cross-site link (passive listener -> native port); limits; off in private tabs
- [ ] EX-16 Speed lab rows: network.buffer.cache.size 32768; gfx.downloadable_fonts.fallback_delay 500
- [ ] EX-17 tell the owner: tablet on the charger overnight, screen off, about once a week (storage TRIM + app compilation only run then)
- [ ] EX-18 small: session.flushSessionState() for the front tab in onPause; setPriorityHint(PRIORITY_HIGH) for the selected tab or strike it from the research files; fix the stale statements listed in section 2.4
```
