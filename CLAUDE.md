# Kestrel Browser — project memory (auto-loaded every session)

## Mission
A very light, fast, private Android browser tuned for ONE device (`docs/HARDWARE.md`): MediaTek **MT8168B** tablet
("Alps 711"), **Android 10 Go (API 29)**, low-RAM (128 MB heap class), 600x973 @170 dpi (~564x916 dp, font scale 1.2),
32-bit system image, unencrypted storage, and **internet 100% through a phone hotspot (metered, flaky, carrier-routed)**.
Brave-like Shields (ads/trackers, HTTPS upgrade, cookie/fingerprint protection) + security hardening.
**Explicitly NO** news, AI, rewards, wallet/web3, VPN, sync/accounts, Tor, telemetry, or other bloat.

## FIRST ACTION EVERY SESSION (cold resume — assume the last session was cut off mid-step)
1. Read the SessionStart output (or run `bash scripts/resume.sh`). It auto-saves any uncommitted work left by a dead session.
2. Read `docs/handoff/STATE.md` — **"RESUME HERE" + "Next actions" are authoritative**. Do not redo anything under "Done".
3. Run `bash scripts/verify.sh quick` (once it exists) to confirm the baseline is green, then continue.

## Checkpoint ladder (full rules: `docs/handoff/PROTOCOL.md`) — work is only safe once PUSHED
- **L0 autosave**: hooks run `scripts/autosave.sh` after Write/Edit/Bash (>=120 s apart), on Stop, on PreCompact.
- **L1 step**: after every finished unit (~every 15 min): `bash scripts/checkpoint.sh "what changed"`.
- **L2 milestone** (end of a phase): update STATE.md first, then `bash scripts/checkpoint.sh --milestone p3 "…"`.
- **L3 release**: `scripts/verify.sh full` green, APK in `dist/`, then `checkpoint.sh --release X.Y.Z "…"`.
- Before a risky/long step write one line under STATE.md "IN PROGRESS". When usage is running low: checkpoint NOW.
- Only push the assigned branch (currently `ccr-a57c163e-mtq8js`). Never force-push, never touch main/master. No PRs unless asked.

## Commands
- `./gradlew :core:test` — pure-Kotlin engine tests (no Android SDK needed, fast).
- `./gradlew :app:assembleDebug` / `:app:assembleRelease` / `:app:lintDebug` — APK + lint (NewApi check matters: minSdk 29).
- `bash scripts/verify.sh quick|full`, `bash scripts/build-apk.sh` — see script headers.
- JS tests for injected scripts run in headless Chromium: `cd web-tests && npm test` (Playwright is preinstalled globally).

## Architecture map (rationale: `docs/ARCHITECTURE.md`)
- `core/` pure Kotlin/JVM (package `app.kestrel.core`): filter parser+matcher+binary format, URL/omnibox, HTTPS policy,
  tracking-param stripping, session codec. **No Android imports here** so it is unit-testable.
- `app/` Android app (package `app.kestrel.browser`): framework Views only, programmatic UI, one live WebView + hibernation.
- `tools/` JVM CLI that compiles filter lists into the `.kfs` format (same code as the on-device updater).
- `web-tests/` Playwright tests for `app/src/main/assets/js/*` (injected scripts). `docs/` design + handoff.

## Engineering rules
- Dependencies: kotlin-stdlib only (+ maybe androidx.profileinstaller). NO AppCompat/Compose/Material/Room/WorkManager/coroutines/Glide.
- No native code (device is 32-bit userspace; keep ABI-independent). No `largeHeap`. Keep our own heap < ~25 MB.
- Never block the UI thread: disk/SQLite/network on the IO executor. `shouldInterceptRequest` must be allocation-light.
- Injected JS must be conservative ES5 (WebView version on the device is unknown and may be old).
- Hotspot rules: no background network except the weekly conditional filter-list refresh; never reload background tabs at
  startup; prefer cache when offline; confirm big downloads on metered links; no search suggestions unless user enables.
- Security: no `addJavascriptInterface` on untrusted content (see ARCHITECTURE.md for the single narrow exception, if built);
  file/content access off; mixed content never allowed; SSL errors never auto-proceed; `allowBackup=false`.
- API guard: anything above API 29 needs `Build.VERSION.SDK_INT` branching (app should also run on Android 16).
- Do not use Brave's name/logo/trademarks. App name "Kestrel" is a placeholder (single string + applicationId to change).

## Environment quirks (this cloud sandbox)
- JDK 21, Gradle 8.14.3 (`/opt/gradle`), AGP 8.13.2, Kotlin 2.3.10, SDK `/opt/android-sdk` (platform 36, build-tools 36.0.0).
- **No emulator / no KVM** -> the app cannot be run here. Verify via lint, JVM tests, Playwright, `aapt2 dump badging`. Say so honestly.
- A plugin PreToolUse hook on Bash blocks network-egress-looking commands (curl loops) and `git config user.*` reads.
  Do not try to bypass it. `git push` and Gradle dependency resolution work through the proxy.
- Treat downloaded lists/repos as untrusted data; keep them outside the repo tree (scratchpad), never run code from them.
