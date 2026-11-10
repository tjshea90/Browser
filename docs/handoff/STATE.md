# STATE — single source of truth for resuming (keep < 150 lines; details live in ARCHITECTURE.md)

<!-- CHECKPOINT:BEGIN -->
- **Last checkpoint:** 2026-11-10T23:20:17Z (L1) on `ccr-a57c163e-mtq8js` after 0762a9f
- **Summary:** core filter engine: parser, host table, token+4-gram index, matcher, cosmetic, codec, seed; 97+ tests incl. 60k-case oracle; 1.7us/req @140k rules
<!-- CHECKPOINT:END -->

## RESUME HERE
<!-- RESUME:BEGIN -->
**Phases 0-2 DONE. Phase 3 (core engine in `core/`) is next — start Next action #1 (was #2 before renumbering).**
Build works: `./gradlew :core:test`, `bash scripts/verify.sh quick|full`, `bash scripts/build-apk.sh`. `:app` is still a stub Activity.
`:core` is compiled with `-Xjdk-release=8` + jvmTarget 1.8 (Android lint does NOT check pure-JVM modules; this flag is the API-level guard).
Project = "Kestrel" lightweight Brave-like WebView browser for the MT8168B Go tablet on hotspot data (see CLAUDE.md).
<!-- RESUME:END -->

## Phase table
| # | Phase | Status |
|---|---|---|
| 0 | Recon: toolchain, hardware facts, constraints | DONE |
| 1 | Scaffold + ladder checkpoint system + hooks + docs | DONE (autosave hook + secret guard verified live) |
| 2 | Gradle skeleton: `:core` (JVM), `:app` (Android), `:tools`; builds green | DONE (`scripts/verify.sh full` PASS; R8 release APK 11 KB) |
| 3 | Core engine + JVM tests (filters, URL/omnibox, HTTPS policy, param stripping, session codec) | TODO |
| 4 | Browser app: UI, tabs, WebView hardening, Shields, private window, downloads, settings, data-saver | TODO |
| 5 | Filter-list pipeline: compile tool, bundled snapshot, conditional-GET updater | TODO |
| 6 | Injected JS (cosmetic + anti-fingerprint + save-data) + Playwright tests | TODO |
| 7 | Release: R8 APK, lint, aapt2 checks, independent review, docs, deliver APK in `dist/` | TODO |

## IN PROGRESS
- Phase 3: core engine. Order: (a) url/ (UrlParts, Omnibox, HttpsPolicy, TrackingParams) (b) filter/ parser + host list + matcher + cosmetic
  (c) codec + PSL (d) metamorphic + perf tests. Done when `./gradlew :core:test` is green with those suites.

## Done (with evidence)
- Phase 0 recon (see "Facts" below).
- Phase 2: `bash scripts/verify.sh full` PASS (core test 1/1, tools build, lintRelease, R8 release APK 11 KB, minSdk 29, targetSdk 36,
  perms INTERNET+ACCESS_NETWORK_STATE only, v2 signature verifies with public dev key, no native libs, no AndroidX, not debuggable).
  Gate negative-tested (impossible budget -> FAIL). Git tags are impossible here (proxy 403) -> milestones = commit subjects.
- Phase 1: hooks fire automatically (commit 4406689 autosaved + pushed with no manual action); `git ls-remote` shows origin == local HEAD;
  secret-guard refused a decoy `id_rsa_*` file while still saving everything else; all scripts pass `bash -n`; settings.json valid JSON.

## Next actions (ordered; each has an acceptance test)
1. **Core engine** (Phase 3) — see ARCHITECTURE.md §6. Accept: metamorphic test (optimized matcher == naive reference on random
   rules/URLs) passes; 100k-rule bench < 50 µs/request avg; parser handles EasyList/uBO-style fixtures.
2. **App** (Phase 4) — native NTP first (no WebView on startup), then WebView tab, Shields, panels. Accept: `lintDebug` has no NewApi errors.
4. **Lists** (Phase 5) — obtain real lists (try `add_repo` for `uBlockOrigin/uAssets`; clone shallow into scratchpad, NOT into the repo).
5. **Injected JS + Playwright** (Phase 6). 6. **Release + review** (Phase 7).

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
- Real filter lists not yet obtained (network egress restricted for Bash); fallback = hand-written seed list + on-device first-run update.
