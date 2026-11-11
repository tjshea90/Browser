# STATE — single source of truth for resuming (keep < 150 lines; details live in ARCHITECTURE.md)

<!-- CHECKPOINT:BEGIN -->
- **Last checkpoint:** 2026-11-11T01:44:17Z (L1) on `ccr-a57c163e-mtq8js` after c4241aa
- **Summary:** dist: kestrel-0.1.0 APK candidate (verify full PASS: core 127, app 80, 13 Chromium, lint, R8); S7/S8 still to do
<!-- CHECKPOINT:END -->

## RESUME HERE
<!-- RESUME:BEGIN -->
**A complete browser exists and was RUN FOR THE FIRST TIME on an Android 10 x86 emulator (Chromium 74 WebView): it launches, lays out correctly at 600x973@170dpi, loads pages, blocks ads/trackers (shield count works).**
**Phase 7 (release) is almost done.** Both independent reviews are finished and triaged in `docs/handoff/REVIEW-2026-11-11.md` (board = single source for what is fixed).
DONE + pushed (core 127 tests, app 80 tests green): S1 intent: crash (reproduced on the emulator: FATAL NumberFormatException), S2 URL-truncation bypass, S3 address bar shows port + committed URL only, S4 poisoned-list guards + canary
check + retry marker, S5 token-dedupe CPU bound, S6 private-window wipe/autofill/downloads/config-changes, S9, S10, S11, S13, C1 redirect classification, C2 https upgrade loop/stale state, C3, C4, C5, C6, C7, C8, C9 (flush off UI thread, cold-start order).
Owner facts since first build: Google default search (done), 2 GB RAM, NO Google Play/services (done: wording, SSL hints, README "No Google Play" + "Updating the web engine").
REMAINING, in order: (1) `bash scripts/verify.sh full` (lint + R8 + 13 Chromium tests + APK audit) -> `bash scripts/build-apk.sh` -> `checkpoint.sh --release 0.1.0 -t "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>" -t "Claude-Session: https://claude.ai/code/session_01UWi8ZytCdyz642NMACyvi9" "..."` -> SendUserFile the APK;
(2) S7 (hide `__KST_CFG` seed from pages: pass config as IIFE argument; WeakSet instead of `canvas.__kstSkip`; web-tests must stay green) and S8 (downloads: show origin host in the prompt, treat server MIME `application/vnd.android.package-archive` as risky);
(3) re-test on the emulator (recipe in REVIEW doc; ~5 min boot; the emulator and http.server die when the worker restarts): intent crash page, `10.0.2.2:8000` shows the port, cosmetic hiding of `.adsbygoogle`/`#ad-banner` was NOT observed on Chromium 74 -> investigate;
(4) final message to the owner: honest about what was and was not run (emulator = Chromium 74 AOSP image, software mode, local pages only; never on the real tablet), ask for the tablet's WebView package+version (About page) and say the simple-workflow plugin's Bash guard is the only plugin that gets in the way.
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
- Phase 7: release build + remaining S7/S8 (see RESUME HERE).
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
