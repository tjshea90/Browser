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
- `configChanges` handled in-place so rotation / font-scale / dark-mode never destroys WebViews. No continuous animations in the chrome.

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
> 20 MB or unknown size on metered links. STRICT adds: images blocked until "Load images", web fonts blocked. Always: tabs never reload at startup; when offline use
`LOAD_CACHE_ELSE_NETWORK`; auto-retry the foreground tab when connectivity returns; error page distinguishes "not connected to Wi-Fi" from "connected but the hotspot has no
internet" (`NET_CAPABILITY_VALIDATED`); no search suggestions by default; weekly conditional (ETag) list refresh <~3 MB, size-capped, HTTPS only; data-used counter from `TrafficStats`.
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
