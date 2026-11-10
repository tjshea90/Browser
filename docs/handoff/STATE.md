# STATE — single source of truth for resuming (keep < 150 lines; details live in ARCHITECTURE.md)

<!-- CHECKPOINT:BEGIN -->
- **Last checkpoint:** (none yet)
<!-- CHECKPOINT:END -->

## RESUME HERE
<!-- RESUME:BEGIN -->
**Phase 1 (scaffold + checkpoint system) is finishing; Phase 2 (Gradle skeleton) is next.**
Do this first: `bash scripts/resume.sh`, then start Next action #1 below. Nothing is built yet beyond docs + scripts.
Project = "Kestrel" lightweight Brave-like WebView browser for the MT8168B Go tablet on hotspot data (see CLAUDE.md).
<!-- RESUME:END -->

## Phase table
| # | Phase | Status |
|---|---|---|
| 0 | Recon: toolchain, hardware facts, constraints | DONE |
| 1 | Scaffold + ladder checkpoint system + hooks + docs | IN PROGRESS (files written; first push pending) |
| 2 | Gradle skeleton: `:core` (JVM), `:app` (Android), `:tools`; builds green | TODO |
| 3 | Core engine + JVM tests (filters, URL/omnibox, HTTPS policy, param stripping, session codec) | TODO |
| 4 | Browser app: UI, tabs, WebView hardening, Shields, private window, downloads, settings, data-saver | TODO |
| 5 | Filter-list pipeline: compile tool, bundled snapshot, conditional-GET updater | TODO |
| 6 | Injected JS (cosmetic + anti-fingerprint + save-data) + Playwright tests | TODO |
| 7 | Release: R8 APK, lint, aapt2 checks, independent review, docs, deliver APK in `dist/` | TODO |

## IN PROGRESS
- Phase 1: pushing the scaffold (scripts/, .claude/settings.json, CLAUDE.md, docs/). Done when `checkpoint.sh` prints `CHECKPOINT OK`.

## Done (with evidence)
- Phase 0 recon (see "Facts" below).

## Next actions (ordered; each has an acceptance test)
1. **Gradle skeleton** — `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`, wrapper (8.14.3),
   modules `:core` (kotlin-jvm), `:app` (com.android.application; applicationId `app.kestrel.browser`; minSdk 29; target/compile 36),
   `:tools`. Accept: `./gradlew :core:test :app:assembleDebug` succeed; APK < 1 MB; no AndroidX in `:app` deps.
2. **Core engine** (Phase 3) — see ARCHITECTURE.md §6. Accept: metamorphic test (optimized matcher == naive reference on random
   rules/URLs) passes; 100k-rule bench < 50 µs/request avg; parser handles EasyList/uBO fixtures.
3. **App** (Phase 4) — native NTP first (no WebView on startup), then WebView tab, Shields, panels. Accept: `lintDebug` has no NewApi errors.
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
