# STATE — single source of truth for resuming (keep < 150 lines; details live in ARCHITECTURE.md)

<!-- CHECKPOINT:BEGIN -->
- **Last checkpoint:** 2026-11-11T02:40:32Z (L1) on `ccr-a57c163e-mtq8js` after 12afa0a
- **Summary:** Kestrel G smoke-tested on Android 10 x86_64 emulator: Firefox 160 renders pages, extension blocks ads via Kestrel's filter engine, cosmetic hiding works; string protocol for native messaging; engine loads only in the UI process; built-in DoH; STATE updated
<!-- CHECKPOINT:END -->

## RESUME HERE
<!-- RESUME:BEGIN -->
**TWO BUILDS NOW.** (1) `:app` = Kestrel 0.1.0 on the tablet's frozen WebView (Chromium 74): done, delivered (`dist/kestrel-0.1.0.apk`, light, but modern sites such as claude.ai refuse it).
(2) `:geckoapp` = **Kestrel G** (applicationId `app.kestrel.browser.gecko`, installs beside the other): same UI/data/shield/filter code (shared tree `app/src/main`, engine-specific WebView code in `app/src/webview`),
pages drawn by **Firefox's engine (GeckoView 160, armeabi-v7a)**. Release APK ~92 MB (never commit; `dist/*gecko*` is gitignored; deliver with SendUserFile). Build: `./gradlew :geckoapp:assembleRelease`; emulator build: `-PgeckoAbi=x86_64` (no 32-bit x86 artifact exists).
SMOKE-TESTED on an Android 10 x86_64 emulator (software mode): launches, NTP, page loads (Firefox/160 UA; `?.`, `??`, `Array.at`, `structuredClone`, `:has()` all work), address bar shows the port, HTTP page shows the red triangle,
ad/tracker requests blocked by Kestrel's own filter engine through the built-in WebExtension `assets/kestrel-shields` (native messaging: JSON text in, "1"/"0"/CSS text out - structured replies fail with "Invalid event data"), element hiding at document_start works, Google default search works.
NOT yet verified on the real tablet. Known gaps / next (in order): (a) real-tablet feedback from the owner (claude.ai, Google, YouTube?, memory/speed on 1.9 GB);
(b) generic cosmetic hiding only applies an ALPHABETICAL prefix of 1200 generic selectors (`#AD_160`, `#AdBox2`...; not `.adsbygoogle`) -> implement class/id token index (content script reports ids/classes, bridge returns matching selectors from all ~29k);
(c) not ported to Gecko yet: data-saver image/font blocking, cache-only "Open saved copy", print/PDF, find-in-page match counter semantics, date/colour prompts, SSL "proceed anyway" uses the engine's own error page, per-site JS off;
(d) tests: Gecko module has no unit tests (pure logic like ShieldsBridge.requestType could be tested); verify.sh does not cover :geckoapp yet (APK audit must allow AndroidX/GeckoView natives, forbid GMS); README/ARCHITECTURE need a Kestrel G section;
(e) Gecko child processes instantiate Application: only the UI process loads the filter engine (GeckoApp checks getProcessName()).
WebView build facts (unchanged): `scripts/verify.sh full` PASS (core 127, app 81 Robolectric, 16 Chromium JS tests, lint, R8 APK); reviews S1-S13/C1-C9 fixed (board in REVIEW-2026-11-11.md).
Owner facts: Google default search; 2 GB RAM; NO Google Play/services; tablet WebView = com.android.webview 74.0.3729.186 (screenshots) -> hence Kestrel G. Plugins: simple-workflow blocks curl-style commands (use gradle/sdkmanager/git instead).
Emulator recipe: `sdkmanager --install emulator "system-images;android-29;default;x86_64"`, avd `kestrel29x64` (pixel_c, 600x973@170, RAM 3 GB), `emulator -avd kestrel29x64 -no-window -no-audio -no-snapshot -no-boot-anim -gpu swiftshader_indirect -no-accel`, boot ~3-5 min, `adb install -r -t geckoapp/build/outputs/apk/debug/geckoapp-debug.apk`; test pages: `python3 -m http.server 8000` -> `http://10.0.2.2:8000/` (do NOT clear the URL field with DEL keys: it selects all on focus, just type).
Build/verify: `./gradlew :core:test :app:testDebugUnitTest`, `bash scripts/verify.sh quick|full` (WebView build only), `bash scripts/build-apk.sh`, `cd web-tests && npm test`.
<!-- RESUME:END -->

## Phase table
| # | Phase | Status |
|---|---|---|
| 0 | Recon: toolchain, hardware facts, constraints | DONE |
| 1 | Scaffold + ladder checkpoint system + hooks + docs | DONE (autosave hook + secret guard verified live) |
| 2 | Gradle skeleton: `:core` (JVM), `:app` (Android), `:tools`; builds green | DONE (`scripts/verify.sh full` PASS; R8 release APK 11 KB) |
| 3 | Core engine + JVM tests (filters, URL/omnibox, HTTPS policy, param stripping, session codec) | DONE (114 tests; see Done) |
| 4 | Browser app: UI, tabs, WebView hardening, Shields, private window, downloads, settings, data-saver | DONE (51 Robolectric tests, lint clean, never run on a real device) |
| 5 | Filter-list pipeline: compile tool, bundled snapshot, conditional-GET updater | DONE (8 updater tests; list URLs verified) |
| 6 | Injected JS (cosmetic + anti-fingerprint + save-data) + Playwright tests | DONE (13 Chromium tests); class/id generic cosmetic hiding deferred |
| 7 | Release: independent review, fixes, docs, signed APK in `dist/` | DONE (0.1.0 built; awaiting first real-tablet feedback) |

## IN PROGRESS
- Nothing in flight. Waiting for the owner's first real-tablet feedback (see RESUME HERE).
- Low-priority candidate: `BrowserDb` is a new `SQLiteOpenHelper` per Activity instance and never closed (CloseGuard warnings in tests) -> make it a process singleton.

## Done (with evidence)
- Phase 7 (partial): README rewritten (install, features, privacy, limits, signing), `.github/workflows/android.yml` (runs only on milestone/release commits + manual), ARCHITECTURE §4/§7 corrected.
  Hotspot fix: `NetworkMonitor.State.NO_INTERNET` (= not *validated*, e.g. carrier blocks Google's connectivity check) was switching WebView to `LOAD_CACHE_ELSE_NETWORK` (stale pages, even on reload) and
  disabling the HTTPS-fallback screen; now only `OFFLINE` (no INTERNET capability) does; NO_INTERNET shows honest wording + "Open saved copy" (`LOAD_CACHE_ONLY`, one navigation). App tests 60/60.
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

## Next actions (ordered)
1. Apply the review findings that survive verification (each: failing test first -> fix -> green `verify.sh full`).
2. Docs: README (install/sideload steps, features, limitations), ARCHITECTURE (final), this STATE. Keep the owner-facing honesty: untested on hardware.
3. `bash scripts/build-apk.sh` -> `dist/kestrel-0.1.0.apk` (+ .sha256); `checkpoint.sh --release 0.1.0 "..."`; send the APK to the owner.
4. Backlog (not started, in value order): class/id hash-set generic cosmetic hiding (29k generic selectors currently capped at 1200); omnibox network suggestions (setting removed
   until implemented); document-start script injection via androidx.webkit if the tablet's WebView supports it; blob: downloads; baseline profile for faster cold start;
   per-site permission memory; reader-free "Load images" per site; scriptlets/redirect resources for YouTube-style ads.

## Owner facts (2026-11-11, said after the first build) - authoritative
- Tablet: **2 GB RAM, Android 10 Go, NO Google Play Store and NO Google services**. => no Safe Browsing, no WebView auto-update, manual APK updates, never add a GMS dependency.
- Anything typed in the URL bar that is not a URL must be searched on **Google** (done: default engine + omnibox rules + tests).
- Licensed third-party products/sources are allowed (personal non-commercial use; owner gets permissions). Still record licences in docs/FILTER-LISTS.md.
- Owner will switch Claude Code plugins on/off on request. Known blocker: the `simple-workflow` plugin's PreToolUse Bash guard denies curl/wget-style network commands, recursive force-deletes
  and `git config user.*` reads (it even matches that text inside a heredoc). `Postman` etc. only add session-start noise. Do NOT bypass the guard; ask the owner, or use git clones via
  add_repo / the web-fetch agent / sdkmanager+gradle instead, and write such text with the Edit tool.
- BIGGEST UNKNOWN: the tablet's WebView package+version (About page shows it). If it is `com.android.webview` and old (<100) it cannot be updated by sideloading Google's APK
  (provider allow-list + signature) -> fallback plan = a GeckoView-based build (MPL-2.0, ~70 MB, needs maven.mozilla.org access; uBlock Origin XPI could ride along as a built-in WebExtension). Decide only after the owner reports the version.

## Engine decision after first tablet run (2026-11-11, owner screenshots) - authoritative
- Tablet About page: `com.android.webview 74.0.3729.186`, RAM 1.9 GB, low-RAM true. claude.ai/login shows "This page can't be shown because your browser or device software is out of date" -> Chromium 74 cannot run modern sites; AOSP provider allow-list means it cannot be replaced.
- Decision: build a GeckoView-based flavor ("Kestrel-G"). Verified from this sandbox: `org.mozilla.geckoview:geckoview-armeabi-v7a:160.0.20261104181552@aar` downloads via Gradle (repo https://maven.mozilla.org/maven2/): AAR 89 MB (libxul.so 118 MB uncompressed, ~155 MB total),
  minSdk 26, permissions INTERNET/ACCESS_NETWORK_STATE/MODIFY_AUDIO_SETTINGS/WAKE_LOCK. Transitive deps include kotlin-stdlib 2.4.21 (our compiler is 2.3.10 -> force stdlib), androidx.media3-common 1.11.1, guava. Maven Central answered 429 once (retry).
- APK ~95 MB: do NOT commit it to git (100 MB push limit); deliver with SendUserFile and keep `dist/*gecko*` ignored. Keep the WebView flavor (v0.1.0) as the light fallback.
- Plan: M1 minimal Gecko browser (our toolbar/NTP/error UI, Google search, HTTPS-only, ETP strict, fingerprint protection, cookie isolation) -> M2 tabs/session/private (GeckoSession private mode)/prompts/permissions/downloads -> M3 our filter engine as a built-in WebExtension via native messaging
  (webRequest blocking + content-script cosmetic CSS at document_start) -> M4 polish/tests/docs. Smoke test only on the slow x86 emulator (geckoview-x86) - say so honestly.
- Owner asked which Claude Code plugins to disable: answered in chat (simple-workflow, postman, failing MCP plugins, and ~40 irrelevant plugins that bloat every turn).

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
- Default search: **Google** (owner's requirement; first entry of `Prefs.ENGINES`; every non-address input is searched there); suggestions OFF (data + privacy). HTTPS-first STRICT by default (carrier can tamper with cleartext).
- Dev signing key will be committed under `signing/dev/` (PUBLIC, for update continuity) — flag this clearly to the user.

## Known issues / risks
- **Never executed on real hardware or an emulator.** Everything is verified through Robolectric (JVM), lint, and real Chromium for the JS. Real-WebView quirks WILL exist.
- Fingerprint/save-data JS is injected at first paint (no document-start API without AndroidX WebKit), so very early inline fingerprinting can run first.
- Generic cosmetic hiding is capped at 1200 selectors; site-specific rules (22k) are complete.
- App cannot be executed in the sandbox; first real run will be on the user's tablet. Keep UI code conservative.
- WebView version on the tablet is unknown (could be very old); app must feature-detect and warn.
- Updater source URLs are unverified (no sanctioned way to curl here): verify with the `web-fetch` agent before trusting; failures must be non-fatal and visible in Settings.
- Licences: bundled lists are GPLv3 / CC BY-SA data (see docs/FILTER-LISTS.md); owner has not chosen a project licence.
