# STATE — single source of truth for resuming (keep < 150 lines; details live in ARCHITECTURE.md)

<!-- CHECKPOINT:BEGIN -->
- **Last checkpoint:** 2026-11-11T00:15:44Z (L1) on `ccr-a57c163e-mtq8js` after 5e75d12
- **Summary:** hotspot: unvalidated Wi-Fi no longer forces stale cache-first loads; Open saved copy action; https fallback only suppressed when offline; 9 new tests (mutation-checked); docs+CI trigger limited to milestones
<!-- CHECKPOINT:END -->

## RESUME HERE
<!-- RESUME:BEGIN -->
**Phases 0-6 DONE: a complete browser exists and passes `bash scripts/verify.sh full` (core 117 tests + app 51 Robolectric tests + 13 Chromium JS tests + lint + R8 APK ~2.3 MB).**
**Phase 7 (release) is in progress:** (1) independent security + code reviews were requested; apply their verified findings, (2) refresh README/ARCHITECTURE/STATE,
(3) `bash scripts/build-apk.sh` -> `dist/kestrel-<version>.apk`, (4) final L3 checkpoint, (5) tell the owner plainly: NEVER RUN ON REAL HARDWARE (no emulator in the sandbox).
Build/verify: `./gradlew :core:test :app:testDebugUnitTest`, `bash scripts/verify.sh quick|full`, `bash scripts/build-apk.sh`, `cd web-tests && npm test`.
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
| 7 | Release: independent review, fixes, docs, signed APK in `dist/` | IN PROGRESS |

## IN PROGRESS
- Phase 7: independent security review + code review (fresh agents, no anchoring); then apply VERIFIED findings only (reproduce first, add a test), refresh docs, build the APK.

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

## Next actions (ordered)
1. Apply the review findings that survive verification (each: failing test first -> fix -> green `verify.sh full`).
2. Docs: README (install/sideload steps, features, limitations), ARCHITECTURE (final), this STATE. Keep the owner-facing honesty: untested on hardware.
3. `bash scripts/build-apk.sh` -> `dist/kestrel-0.1.0.apk` (+ .sha256); `checkpoint.sh --release 0.1.0 "..."`; send the APK to the owner.
4. Backlog (not started, in value order): class/id hash-set generic cosmetic hiding (29k generic selectors currently capped at 1200); omnibox network suggestions (setting removed
   until implemented); document-start script injection via androidx.webkit if the tablet's WebView supports it; blob: downloads; baseline profile for faster cold start;
   per-site permission memory; reader-free "Load images" per site; scriptlets/redirect resources for YouTube-style ads.

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
- **Never executed on real hardware or an emulator.** Everything is verified through Robolectric (JVM), lint, and real Chromium for the JS. Real-WebView quirks WILL exist.
- Fingerprint/save-data JS is injected at first paint (no document-start API without AndroidX WebKit), so very early inline fingerprinting can run first.
- Generic cosmetic hiding is capped at 1200 selectors; site-specific rules (22k) are complete.
- App cannot be executed in the sandbox; first real run will be on the user's tablet. Keep UI code conservative.
- WebView version on the tablet is unknown (could be very old); app must feature-detect and warn.
- Updater source URLs are unverified (no sanctioned way to curl here): verify with the `web-fetch` agent before trusting; failures must be non-fatal and visible in Settings.
- Licences: bundled lists are GPLv3 / CC BY-SA data (see docs/FILTER-LISTS.md); owner has not chosen a project licence.
