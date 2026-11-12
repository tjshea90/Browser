# CHECKPOINT 967 — read me first, then TASKS.md

**Written:** 2026-11-12T07:45:33Z · **tests:** 1 RED: test_handoff.sh (0 green)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `f30c803` (this checkpoint is the commit after it)

## Just done
pre-ship: first Courier release: the small app for your PHONE. It downloads files for the tablet's Kestrel G over the phone's own connection and hands them over the hotspot wifi. Install it on the PHONE, switch on Ready for the tablet, then type the code it shows in Kestrel G > Settings > Download on my phone > Pair with the phone.

## Do this next
ship.sh gates and releases this (courier)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/build.gradle.kts

## Last ten checkpoints
```
  354d25f ckpt 960: Y2g (CI plumbing for the third APK) written: appinfo.sh/ship.sh/record-release
  c7820e2 ckpt 957: Y2e BUILT (not yet shipped): module :courier (CourierRuntime/Service/Activity/
  b6b44e0 ckpt 945: Y2d DONE incl. Android glue: PhoneBridge (pairing code, gateway discovery, deb
  46aaf8d ckpt 938: Y2d steps 1+2 DONE (pure core): SocketHttp, LanFetcher, CourierClient, PhoneBa
  b33092b ckpt 930: Y2d step 1 DONE: SocketHttp (cleartext-safe socket HTTP client), LanFetcher (p
  a042f23 ckpt 923: Y2c DONE (phone-side host logic + staging files + JSON store + SSRF guard; fet
  e084b8b ckpt 892: Y2b DONE: core/courier package (Json, Sign+Pairing+ReplayGuard+AuthThrottle, H
  89585c0 ckpt 874: alpha18 SHIPPED and recorded (kestrel-g-v0.3.0-alpha18, run 43164931160 green,
  1569d02 ckpt 869: gated kestrel-g-v0.3.0-alpha18 (gecko code 117) and pushed it: handoff self-te
  9932748 ckpt 868: pre-ship: alpha18: the updater now always tries to install (it no longer stops
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
