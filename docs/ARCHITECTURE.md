# Architecture & decisions — Kestrel

## 1. Goals / non-goals
**Goals:** fast, tiny, private, security-hardened browser for the MT8168B Android 10 Go tablet (`HARDWARE.md`) used over a phone hotspot.
Brave-like Shields: ad/tracker blocking, HTTPS upgrade, third-party-cookie blocking, fingerprint farbling, tracking-parameter
stripping, popup blocking, private window, per-site shield controls.
**Non-goals (deliberately absent):** news feed, sponsored tiles, rewards/BAT, wallet/web3, VPN, AI assistant, sync/accounts, Tor, IPFS,
playlist, speedreader, talk, extensions, telemetry of any kind.

## 2. Engine decision — system WebView (Chromium)
Considered: bundled GeckoView (60-80 MB APK, 300+ MB RAM: infeasible on a 128 MB-class Go device); bundled Chromium fork (needs a full
Chromium build, >100 GB, APK >100 MB: impossible here and heavy there); custom engine (cannot render "all HTML"). So: **Android System
WebView** (the same Blink/V8 that Brave is built on, but shared with the OS = zero engine footprint). Consequences, stated honestly:
page compatibility and engine security fixes follow the tablet's WebView version; the app shows it in About, warns on the NTP when old,
and every optional capability is feature-detected. Brave's own Rust adblock engine is replaced by a Kotlin engine (§6).

## 3. Modules
- `:core` (kotlin-jvm, package `app.kestrel.core`) — ALL testable logic, no Android imports: `filter/` (parser, host list, token index,
  matcher, cosmetic, binary codec, PSL), `url/` (parser, omnibox, HTTPS policy, tracking params, redirect unwrap, UA), `session/` (tab codec), `shield/` (policy model).
- `:app` (com.android.application, package `app.kestrel.browser`) — framework Views only, programmatic UI. Deps: kotlin-stdlib (+ optionally profileinstaller).
- `:tools` (kotlin-jvm CLI) — compiles lists to `.kfs`; same code path as the on-device updater.

## 4. Startup, threading, memory
- Cold start shows a **native new-tab page** immediately; WebView is created on idle / first navigation (WebView init is 100-300 ms on this class of SoC).
- UI thread: views + WebView calls only. One IO executor (SQLite, files, list compile). Filter engine loads on a background thread; `shouldInterceptRequest`
  (runs on WebView IO threads) waits <=1.5 s for the first load, then falls back to the tiny built-in seed engine (fail-closed, never fail-open silently).
- Max live WebViews = 2 on low-RAM (current + opener/most-recent); older tabs are hibernated (`saveState` -> Bundle + disk) and restored lazily with
  cache-first loading. `onTrimMemory` hibernates more; `onRenderProcessGone` recovers the tab; renderer priority: visible = IMPORTANT, hidden = WAIVED.
- `configChanges` handles rotation / screen-size / keyboard in place (WebViews survive). Dark-mode and font-scale changes deliberately recreate the Activity so the
  programmatic UI re-reads its colours/sizes: the session is saved in `onPause`, and `onCreate` restores it (only the visible tab loads, cache-first). No continuous animations in the chrome.

## 5. Shields (Brave feature -> mechanism)
| Feature | Mechanism |
|---|---|
| Ad + tracker blocking | `shouldInterceptRequest` + compiled filter set; neutered responses (1x1 gif / empty js / empty css / 204) with CORS-allow header |
| Cosmetic filtering | CSS injected at `onPageStarted` + `onPageCommitVisible` + `onPageFinished` (idempotent); site-specific + small curated generic set; later: class/id hash-set approach |
| HTTPS upgrade (default STRICT) | upgrade `http://` main-frame navigations; on failure show native warning screen; never for LAN/localhost/IP/single-label hosts |
| 3rd-party cookies | `CookieManager.setAcceptThirdPartyCookies(false)`, per-site override |
| Fingerprint protection | injected ES5 farbling (canvas/audio noise, hardwareConcurrency/deviceMemory clamp, plugins) seeded per site+session |
| Tracking params / redirectors | `shouldOverrideUrlLoading`: strip `utm_*`, `fbclid`, `gclid`...; unwrap known redirectors (google.com/url?q=, l.facebook.com/l.php?u=...) |
| Malware / phishing | WebView Safe Browsing (needs GMS; harmless if absent) + local URLhaus/uBO-badware hostlist (also blocks documents) |
| Popups | `onCreateWindow`: gesture-gated, otherwise blocked with a chip |
| Per-site JS off | `setJavaScriptEnabled(false)` + reload |
| Private window | separate process `:private`, `WebView.setDataDirectorySuffix("private")`, wiped at process start, `FLAG_SECURE`, IME no-personalized-learning, nothing persisted |
| Referrer / cache hygiene | Chromium default `strict-origin-when-cross-origin`; "Clear data on exit" option |

## 6. Filter engine (core/filter)
- **Syntax supported:** network rules `||` `|` `*` `^` `@@`; options `third-party|1p|3p|~third-party`, types (`script image stylesheet xhr subdocument font media
  websocket ping object other document`), `domain=a|~b`, `important`, `badfilter`; `@@..$document/$elemhide/$generichide`; hosts-file lists;
  cosmetic `##` and `#@#` (standard CSS only). **Skipped on purpose:** regex rules (ReDoS), `+js` scriptlets, `redirect=`, `csp=`, `removeparam=`,
  procedural cosmetics (`:has-text`, `:xpath`, `:upward`...), `match-case`.
- **Pure host rules** (`||host^`, optional 3p/important): sorted `LongArray` of 64-bit hashes + parallel `ByteArray` flags; lookup walks host suffixes (>=2 labels).
- **Other rules:** custom wildcard matcher (no `java.util.regex`), indexed by the *least frequent whole-URL token* (a pattern run counts only when both neighbours are
  non-alphanumeric/anchored); token-less rules go to a small fallback list. Block rules are evaluated first; exceptions only when a block matched; `$important` beats `@@`.
- **Semantics:** default types = all subresources, NOT main-frame (`$document` opts in); malware-category hosts also block documents. Third-party = different
  registrable domain (embedded PSL). Type inference in `shouldInterceptRequest`: `isForMainFrame` -> DOCUMENT; else by `Accept`, URL extension, `X-Requested-With`; unknown -> OTHER.
- **Binary `.kfs`:** magic+version, created-at, per-list meta (name, etag, last-modified, counts), arrays, CRC32. Loader verifies and falls back to the bundled snapshot.
- **Sanitising cosmetics:** conservative selector grammar; reject `{ } ; url( @import \`; separate CSS rules for modern pseudos so one bad selector can't void a group.
- **Perf targets:** <50 us average per request at 100k rules; engine heap < ~8 MB for the default lists.

## 7. Hotspot / data-frugal design
Data saver levels OFF / **STANDARD (default)** / STRICT. STANDARD: ad+tracker blocking, no autoplay, injected `preload=none`, `navigator.connection.saveData=true`, confirm downloads
> 20 MB or unknown size on metered links. STRICT adds: images blocked until "Load images", web fonts blocked. Always: tabs never reload at startup; auto-retry the foreground
tab when connectivity returns; no search suggestions by default; weekly conditional (ETag) list refresh <~3 MB, size-capped, HTTPS only; data-used counter from `TrafficStats`.
**Connectivity model (`NetworkMonitor`) — the rule is "never let a heuristic make browsing worse than the default":** `OFFLINE` = no network with `NET_CAPABILITY_INTERNET` at all
(hotspot off / out of range): only then are pages served cache-first (`LOAD_CACHE_ELSE_NETWORK`, saved copies open even if stale) and HTTPS-fallback logic suspended.
`NO_INTERNET` = Wi-Fi is up but Android has not *validated* it (`NET_CAPABILITY_VALIDATED` missing). That flag is also missing on a perfectly working hotspot whose carrier blocks
Google's connectivity-check URL, and for a few seconds after every reconnect, so it is used **only for wording** (error page: "Android reports no internet") and for the manual
"Open saved copy" action — pages still load live with normal HTTP caching. `ONLINE` = validated. Auto-retry of a failed foreground page fires when the state returns to `ONLINE`.
Secure DNS cannot be set inside WebView: Settings explains the system *Private DNS* option.

## 8. Security baseline
`allowFileAccess/ContentAccess/FileAccessFromFileURLs/UniversalAccess=false`; `MIXED_CONTENT_NEVER_ALLOW`; Safe Browsing on; SSL errors never auto-proceed (native warning, per-host
session exception); no `addJavascriptInterface` for web content; external `intent:`/custom-scheme launches sanitised (strip component/selector, require BROWSABLE) and confirmed;
only `http/https/about:blank` accepted from intents and the omnibox (`javascript:`/`file:`/`content:`/`data:` rejected); `android:allowBackup=false`; all components explicit `exported`;
downloads sanitised filenames, APK warning, cookies+UA+Referer forwarded to DownloadManager; JS dialogs custom with spam suppression; file chooser via system picker only.
Dev signing key (`signing/dev/`) is PUBLIC by design (update continuity for a personal sideload) — replace with a private key via `keystore.properties` for distribution.

## 9. Persistence & recovery
SQLite (framework) for history/bookmarks on the IO thread; session = atomic-write file (temp+rename) with checksum, corrupt -> start fresh; compiled filter set swapped atomically,
previous copy kept; every store tolerates truncation. History capped (default 90 days / 5000 rows). `Clear data on exit` optional.

## 10. Known limitations (be honest in the UI/docs)
No scriptlets (so no YouTube-ad-style blocking), approximate request-type inference, no DoH inside WebView, no `blob:` downloads in v1, no extensions, engine = whatever WebView
the tablet has, cosmetic filtering is best-effort (injection after commit). Not yet run on real hardware (no emulator in the build sandbox).

## Kestrel G UI shell and in-app updates (alpha4)
- **Layout** (`geckoapp/.../BrowserActivity.kt` `buildUi`): `root` FrameLayout -> `column` [`content` (GeckoView + Home + error overlays) / 2 dp progress / `BottomDock`], then overlays: find bar (top), omnibox
  suggestions (bottom-anchored above the address row), tab grid, bottom sheet host (menu, Shields, Updates), fullscreen video. Design rules: `docs/DESIGN.md`.
- **Keyboard**: while an address field is focused the window is `adjustNothing` (the page view never resizes). `ui/KeyboardProbe` finds the keyboard's top edge (Android 11+: IME insets; Android 10: a 0-width
  adjustResize popup window - under adjustNothing the app's own window cannot see the keyboard). `typingTick` polls it and lifts the dock / Home field with `translationY`. Verified on the Android 10 emulator.
- **Tabs**: regular and private tabs share one list (`tabs`) but never share UI: the dock/Home skin follows the tab in front (`applySkin`), the tab grid has two panes, new tabs from the dock follow the current
  mode, previews of private tabs stay in RAM (`ui/ThumbStore`). Home over a page = `Tab.showHome`.
- **Updater**: `core/update/UpdateLogic` (pure: which release, checksum, resume rules; `UpdateLogicTest`) + `app/update/UpdateManager` (Releases API with ETag, resumable download, SHA-256, PackageInstaller session)
  + `InstallResultReceiver` + `ui/UpdatePanel`. The tag prefix comes from `Flavor.updateTagPrefix`. A debug build can read a local feed: `adb shell setprop debug.kestrel.feed http://10.0.2.2:8000/releases.json`
  (the whole flow incl. pause/resume and the system installer was run that way on the emulator). Android only installs an APK signed with the installed app's certificate (`signing/CERT_SHA256`).

## Kestrel G: engine process death, scroll tests, memory facts (alpha6)
- **Engine process death** (`BrowserActivity.sessionGone`, called from `ContentDelegate.onKill` / `onCrash`): with one content process every live tab dies together. The first death in a quiet
  period (15 s) restores the tab's last `GeckoSession.SessionState` (history AND scroll position) behind `ui/RestoreOverlay` (the tab's last preview dimmed, "Page freed to save memory - bringing
  it back"; it goes away at `onFirstContentfulPaint`, after 20 s at the latest); a second death within 15 s reloads the plain URL (the state itself may be the cause); a third shows the error screen.
  Every recovery is logged with `memFacts()` (free RAM, threshold, newest trim level and its age) and counted (`gecko.kill`, `gecko.recover.state`, `mem.trim.<level>`).
- **Engine prefs v2** (`assets/gecko-prefs.txt`): the decoded-image cache is RAM / `image.mem.surfacecache.size_factor` (engine default 4 = about 475 MB here; `max_size_kb` is ignored on a 32-bit
  engine), so we set `size_factor = 20` (about 95 MB). Facts and evidence: `research/SCROLL.md`. **Scroll feel** (Settings, `Prefs.scrollFeel`, `GeckoEngine.applyScrollFeel`) switches the live apz.*
  prefs (pre-drawn margin, touch-handler wait, top fling speed); default = the engine's own values.
- **Measuring** (Diagnostics): `web/ScrollTest` drives the bundled offline page `assets/scrolltest/` with 32 swipes through the real touch path and reports compositor frames per second, gaps and a
  verdict; `GlInfo` reads the GPU vendor / renderer / driver through an EGL pbuffer; the pref read-back shows what the engine really has; compositor events (first composite, paint reset) are logged.
- **While the user scrolls** nothing of ours competes for frames: the neon pulse holds still while a finger is down (+1.5 s), the post-load preview capture (a GPU read-back) waits for 3 s without touch.

## Kestrel G: touch path, pull-to-refresh, scroll feel, neon rules (alpha7)
- **Touch path**: `web/KestrelGeckoView.onTouchEvent` sends every event to the engine through `onTouchEventForDetailResult` (same effect as `GeckoView.onTouchEvent`, plus an answer) and returns true like the
  stock view. The answer for the touch-DOWN says what the page does with the touch: `scrollableDirections` (bit 1 = can scroll up, bit 4 = can scroll down: 4 at the top of a page, 5 mid-page),
  `handledResult` (3 = the page's own script handles it) and `overscrollDirections` (3 = overscroll allowed). **Pull-down-to-refresh** starts only when the page cannot scroll up, its script does not handle the
  touch, overscroll is allowed, and the finger then moves down (mostly vertical, past the touch slop): `PullListener` reports the distance, `BrowserActivity.pullListener` moves `ui/PullChip` (a view over the
  page; the page itself never moves) at 0.6x the finger, arms at 64 dp of chip travel (haptic tick), and a release while armed reloads (`pullRefresh`: retry on an error page). Enabled by `pullEnabled`
  (Settings toggle; not on Home, error pages, fullscreen, sheets, the tab grid, during the Scroll test). The decision logic lives in the view, not in `:core`: a pure `PullTracker` with JVM tests is a follow-up.
- **Scroll feel** (`GeckoEngine.applyScrollFeel`, live `apz.*` prefs; default level 1 = Smoother): Gecko's display-port multipliers are scaled down by viewport pixels / 250 before use, so Firefox's 1.5 is only
  +16 % of the screen height on this tablet; Smoother paints about twice the screen around the finger (4.0 / 3.0), shifts it toward the scroll direction (`apz.velocity_bias` 1.0), keeps it 3 minutes after scrolling
  stops (`apz.displayport_expiry_ms`) and waits 300 ms instead of 600 ms for the page's touch handlers. Diagnostics > Compare 3 feels runs the Scroll test at all three levels. Evidence and caveats: `research/SCROLL.md` "R2 update".
- **Neon**: not gated on the system animation scale; holds still only for the keyboard being up (not "a field has focus"), the first 3 s of a load, a finger on the screen (1.5 s after a swipe, 0.35 s after a tap),
  battery-saver, app in the background, 2 min idle on a page, and while a full-screen page (Downloads, Settings, History, Diagnostics) covers it (`Neon.covered`: on the emulator the render thread went from 38 % to 3 %).
  `Neon.status()` is shown in Diagnostics. Details: `docs/DESIGN.md`.
