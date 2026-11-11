# STATE — single source of truth for resuming (keep < 150 lines; details live in ARCHITECTURE.md)

<!-- CHECKPOINT:BEGIN -->
- **Last checkpoint:** 2026-11-11T00:01:06Z (L1) on `ccr-a57c163e-mtq8js` after 8310637
- **Summary:** filter updater (conditional GET, safety invariants, JobScheduler) + 8 updater tests; list URLs verified via web-fetch (cookie URL corrected); 48 app tests green
<!-- CHECKPOINT:END -->

## RESUME HERE
<!-- RESUME:BEGIN -->
**Phases 0-3 DONE (scaffold, Gradle, full core engine with real EasyList/uBO snapshot). Phase 4 (the Android app in `app/`) is next.**
`:app` is still the stub `MainActivity`. Everything it needs from `:core` exists and is tested: `FilterSet.check()`, `Omnibox`, `HttpsPolicy`,
`TrackingParams`, `RedirectUnwrapper`, `UserAgents`, `ShieldSettings/SiteShieldStore/DataSaver`, `SessionCodec`, `Psl`, `FilterSetCodec`.
Assets already in place: `app/src/main/assets/filters/default.kfs` (4.3 MB) + `psl.txt`.
Build/verify: `./gradlew :core:test` (114 tests), `bash scripts/verify.sh quick|full`, `bash scripts/build-apk.sh`.
Start Phase 4 with Next action #1. Update this block + the phase table at every milestone.
<!-- RESUME:END -->

## Phase table
| # | Phase | Status |
|---|---|---|
| 0 | Recon: toolchain, hardware facts, constraints | DONE |
| 1 | Scaffold + ladder checkpoint system + hooks + docs | DONE (autosave hook + secret guard verified live) |
| 2 | Gradle skeleton: `:core` (JVM), `:app` (Android), `:tools`; builds green | DONE (`scripts/verify.sh full` PASS; R8 release APK 11 KB) |
| 3 | Core engine + JVM tests (filters, URL/omnibox, HTTPS policy, param stripping, session codec) | DONE (114 tests; see Done) |
| 4 | Browser app: UI, tabs, WebView hardening, Shields, private window, downloads, settings, data-saver | TODO |
| 5 | Filter-list pipeline: compile tool, bundled snapshot, conditional-GET updater | TODO |
| 6 | Injected JS (cosmetic + anti-fingerprint + save-data) + Playwright tests | TODO |
| 7 | Release: R8 APK, lint, aapt2 checks, independent review, docs, deliver APK in `dist/` | TODO |

## IN PROGRESS
- Phase 4 (app): nothing written yet. Order: (a) manifest+theme+Palette+icons (b) Engine holder (load kfs+psl on bg thread) (c) native NTP + toolbar + omnibox
  (d) WebView factory/clients + Shields interception (e) TabManager+hibernation+session (f) panels: tabs, shields, menu, find, history/bookmarks, settings
  (g) downloads, permissions, dialogs, fullscreen video, private process (h) NetworkMonitor + error screens (i) filter updater. Checkpoint after each letter.

## Done (with evidence)
- Phase 3 (core, 114 tests green): url/ (UrlParts, NetHosts, Omnibox, HttpsPolicy, TrackingParams, RedirectUnwrapper, UserAgents), filter/ (Hash, RequestType,
  Psl, RuleParser, CosmeticSelectors, CosmeticIndex, FilterSet, FilterSetBuilder, FilterSetCodec, SeedLists), shield/ (ShieldSettings, SiteShieldStore, DataSaver),
  session/ (SessionCodec). Evidence: 60k-case differential test vs a regex oracle; 7/7 deliberate matcher mutants killed (script: see JOURNAL);
  perf 1.7 us/request at 100k host + 40k pattern rules (was 450 us before the 4-gram fallback index); codec rejects 300/300 single-bit flips + truncations;
  REAL lists (EasyList, EasyPrivacy, uBO filters/privacy/badware/unbreak, EasyList cookies) compile in 250 ms to 4.3 MB; DefaultListsRegressionTest: 16/16 ad+tracker
  URLs blocked, 0 false positives on 16 major sites + 18 essential third-party resources (fonts, CDNs, reCAPTCHA, Stripe, PayPal, Maps).
- Phase 0 recon (see "Facts" below).
- Phase 2: `bash scripts/verify.sh full` PASS (core test 1/1, tools build, lintRelease, R8 release APK 11 KB, minSdk 29, targetSdk 36,
  perms INTERNET+ACCESS_NETWORK_STATE only, v2 signature verifies with public dev key, no native libs, no AndroidX, not debuggable).
  Gate negative-tested (impossible budget -> FAIL). Git tags are impossible here (proxy 403) -> milestones = commit subjects.
- Phase 1: hooks fire automatically (commit 4406689 autosaved + pushed with no manual action); `git ls-remote` shows origin == local HEAD;
  secret-guard refused a decoy `id_rsa_*` file while still saving everything else; all scripts pass `bash -n`; settings.json valid JSON.

## Next actions (ordered; each has an acceptance test)
1. **App shell (Phase 4a-c)**: real manifest (INTERNET, ACCESS_NETWORK_STATE, location/camera/mic declared-not-granted, VIEW http/https + WEB_SEARCH intent filters,
   network_security_config allowing cleartext, `configChanges` list, allowBackup=false), `Palette` light/dark, vector icons (verify path data by rendering), launcher icon
   (original, NOT Brave's lion), programmatic UI: toolbar (back/fwd/reload, shield chip with count, URL pill, tabs count, menu), progress bar, native NTP (search box +
   top sites, no WebView yet). Accept: assembleRelease + lintRelease green, APK < 1 MB without assets; startup path creates no WebView.
2. **Engine + WebView (4d)**: `EngineHolder` (bg load of kfs from filesDir else assets else SeedLists; PSL), `WebViewFactory` (hardened settings, UA, cookies),
   `BrowserWebViewClient.shouldInterceptRequest` (Shields), `shouldOverrideUrlLoading` (HTTPS upgrade, param strip, redirect unwrap, intent: sanitising), error screens.
3. **Tabs (4e)**: TabManager (max 2 live WebViews on low-RAM), hibernation via saveState, SessionCodec persistence (atomic temp+rename), onTrimMemory, onRenderProcessGone.
4. **Panels + features (4f-h)**: see ARCHITECTURE.md sections 4,5,7,8. 5. **Updater (Phase 5)** 6. **Injected JS + Playwright (Phase 6)** 7. **Release + review (Phase 7)**.
   Also pending from earlier: class/id hash-set generic cosmetic hiding (Phase 6), verify updater URLs via the `web-fetch` agent (curl is blocked by a hook).

## Facts (Phase 0)
- Device (hardware_.txt, kept in `docs/hardware/`): MT8168B, Mali-G52 MC2, Android 10 Go API 29, patch 2021-07-05, kernel 4.14.141,
  low_ram=true, heap class 128 MB, 600x973 @170 dpi, font scale 1.2, unencrypted, single user, `sys_mssi_t_32_ago` (32-bit image).
- User constraint: the tablet reaches the internet ONLY via a phone hotspot -> treat as metered/flaky; HTTPS-first; data-frugal.
- Sandbox: JDK 21, Gradle 8.14.3 (+wrapper dist cached), AGP 8.13.2, Kotlin 2.3.10, SDK platform 36, build-tools 35/36, ANDROID_HOME=/opt/android-sdk.
  Cached: androidx.profileinstaller 1.4.0, startup 1.1.1, JUnit 4.13.2, Robolectric jars (NOT android-all). NOT cached: androidx.webkit, R8 artifact.
- No emulator, no system images, no /dev/kvm. Playwright 1.63 + Chromium preinstalled (`/opt/pw-browsers`).
- Plugin hook (simple-workflow) blocks Bash curl-style egress and `git config user.*` reads — do not bypass.
- Remote: origin = https://github.com/tjshea90/Browser ; branch `ccr-a57c163e-mtq8js` ; no upstream until first push.

## Decisions (short; rationale in ARCHITECTURE.md)
- Engine = system WebView (Chromium). Brave features implemented in our app layer. UI = framework Views, no AndroidX UI libs.
- Native new-tab page (no WebView at startup); one live WebView (+1 for opener) and hibernation of the rest via saveState.
- Private window = separate process `:private` with `WebView.setDataDirectorySuffix`, FLAG_SECURE, nothing persisted.
- Pure-host rules stored as sorted 64-bit hashes; other rules token-indexed; compiled to binary `.kfs`; lists refreshed weekly (conditional GET).
- Default search: Brave Search; suggestions OFF (data + privacy). HTTPS-first STRICT by default (carrier can tamper with cleartext).
- Dev signing key will be committed under `signing/dev/` (PUBLIC, for update continuity) — flag this clearly to the user.

## Known issues / risks
- App cannot be executed in the sandbox; first real run will be on the user's tablet. Keep UI code conservative.
- WebView version on the tablet is unknown (could be very old); app must feature-detect and warn.
- Updater source URLs are unverified (no sanctioned way to curl here): verify with the `web-fetch` agent before trusting; failures must be non-fatal and visible in Settings.
- Licences: bundled lists are GPLv3 / CC BY-SA data (see docs/FILTER-LISTS.md); owner has not chosen a project licence.
