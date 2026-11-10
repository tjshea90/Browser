# Kestrel Browser

A very light, fast, private Android browser tuned for a **MediaTek MT8168B / Android 10 Go tablet** (low-RAM, 600x973 @170 dpi) that
reaches the internet **only through a phone hotspot**. Brave-style Shields — ad/tracker blocking, HTTPS upgrade, cookie and
fingerprint protection, private window — with none of the bloat (no news, AI, rewards, wallet, VPN, sync, telemetry).

> **Status:** under construction. See `docs/handoff/STATE.md` for exactly what is done and what is next.
> The app has not yet been run on real hardware (the build sandbox has no emulator) — expect first-run bugs.

Rendering uses the tablet's **Android System WebView** (Chromium); everything Brave-like is implemented in this app. Details and
trade-offs: `docs/ARCHITECTURE.md`. Hardware analysis: `docs/HARDWARE.md`.

## Build
```
./gradlew :core:test            # engine tests (JVM, fast)
./gradlew :app:assembleRelease  # APK (see scripts/build-apk.sh)
```
Install on the tablet: copy the APK from `dist/` and sideload it (allow "install unknown apps" for your file manager/browser).

## For Claude sessions
Read `CLAUDE.md` first, then `docs/handoff/STATE.md`. Work is auto-saved to GitHub by hooks (`scripts/autosave.sh`); see `docs/handoff/PROTOCOL.md`.

License: not yet chosen by the owner. Third-party filter lists keep their own licenses (see `docs/FILTER-LISTS.md` once added).
