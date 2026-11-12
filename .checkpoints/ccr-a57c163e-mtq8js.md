# CHECKPOINT 945 — read me first, then TASKS.md

**Written:** 2026-11-12T06:58:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `35343b2` (this checkpoint is the commit after it)

## Just done
Y2d DONE incl. Android glue: PhoneBridge (pairing code, gateway discovery, debug.kestrel.phone override, status/diag/test-link texts), DownloadController offload+routing+merged rows+notes, Settings 'Download on my phone' section (mode, pair, test link), Diagnostics line, Action.DOWNLOAD_HERE; app tests 97 + lint + both compiles green

## Do this next
Y2e: new Gradle module :courier (Android application, package app.kestrel.courier, applicationId app.kestrel.courier, minSdk 29 targetSdk 36, depends on :core only; copy the signing/versionCode conventions from geckoapp/build.gradle.kts and settings.gradle.kts include) with CourierService (foreground, specialUse), CourierActivity (one screen: status line, pairing code big with Show again/New code, Ready switch, battery-exemption button, live job list, free space), notification, AndroidNetGate, interface lister for PeerGuard, wake lock while jobs run, engine wiring exactly like core/src/test/.../CourierHarness.kt Phone (Engine+StagingSinks+JsonRecStore+UrlConnectionFetcher(Ssrf.guard())+CourierHost+CourierServer, engine.restore(); host.load(); engine.resumeInterrupted(); host.tick() each minute); then Y2j emulator proof with BOTH apps in one emulator (setprop debug.kestrel.phone 127.0.0.1:8765, local media server on 10.0.2.2:8000 needs the SSRF guard off in a debug-only switch because the origin is a private address); then Y2g CI plumbing. Design = research/PHONE_OFFLOAD.md 4.2, 6.1. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  46aaf8d ckpt 938: Y2d steps 1+2 DONE (pure core): SocketHttp, LanFetcher, CourierClient, PhoneBa
  b33092b ckpt 930: Y2d step 1 DONE: SocketHttp (cleartext-safe socket HTTP client), LanFetcher (p
  a042f23 ckpt 923: Y2c DONE (phone-side host logic + staging files + JSON store + SSRF guard; fet
  e084b8b ckpt 892: Y2b DONE: core/courier package (Json, Sign+Pairing+ReplayGuard+AuthThrottle, H
  89585c0 ckpt 874: alpha18 SHIPPED and recorded (kestrel-g-v0.3.0-alpha18, run 43164931160 green,
  1569d02 ckpt 869: gated kestrel-g-v0.3.0-alpha18 (gecko code 117) and pushed it: handoff self-te
  9932748 ckpt 868: pre-ship: alpha18: the updater now always tries to install (it no longer stops
  879b10f ckpt 867: Y1 DONE (updater always tries: no storage gate in download()/install(), floor2
  ceaa63c ckpt 860: owner message 12 written into TASKS (Y1 updater must always try, Y2 phone offl
  eadd5fc ckpt 856: post-alpha17 in the tree (NOT shipped): Make MP4 for finished .ts rows (Stream
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
