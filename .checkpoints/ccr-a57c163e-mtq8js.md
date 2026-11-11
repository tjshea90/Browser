# CHECKPOINT 413 — read me first, then TASKS.md

**Written:** 2026-11-11T17:37:16Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `d01f2d0` (this checkpoint is the commit after it)

## Just done
R2 first pass DONE on the emulator: Gecko source says display-port multipliers are scaled by viewport px/250 (1.5 = +16% = ~60 px here), so Scroll feel now defaults to Smoother (skate 4.0, stationary 3.0, velocity bias 1.0, touch wait 300, expiry 3 min), Smoothest stronger, Standard = engine values; Diagnostics gets 'Compare 3 feels' + 27 engine prefs read back; research/SCROLL.md R2 update

## Do this next
Verify S1 floor test (rangeserver 8001 + STATUS_FAILURE_STORAGE broadcast via adb root) and the 'X MB of Y MB' download card; then gate (:core:test :app:testDebugUnitTest :app:compileDebugKotlin :geckoapp:compileDebugKotlin :app:lintDebug), bump geckoapp versionCode 106 / 0.2.0-alpha7 + tools/sync-ext-version.sh, ship.sh gecko, dispatch android.yml, record-release, reply to owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  6cbe9d9 ckpt 406: R3 pull-down-to-refresh DONE + emulator-verified (KestrelGeckoView pull tracki
  a2797c7 ckpt 400: R1 neon DONE on the emulator (system animations off): pulse no longer gated on
  dec2a93 ckpt 392: S1+S2 code done: updater storage plan (core UpdateLogic.storagePlan + tests; U
  f0bff4b ckpt 384: Owner request 7 recorded (S1 in-app update blocked by INSTALL_FAILED_INSUFFICI
  fcf63d9 ckpt 382: Owner request 6 recorded in TASKS.md (R1 neon not visible, R2 scrolling stutte
  4ac5081 ckpt 376: alpha6 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha6, gecko code 105, run 430556
  69d37b2 ckpt 372: gated kestrel-g-v0.2.0-alpha6 (gecko code 105) and pushed it: handoff self-tes
  3c8f9fe ckpt 371: pre-ship: alpha6: a page that Android frees to save memory now comes back wher
  1d4be6f ckpt 369: Q2 work for alpha6 (code compiles, emulator-tested: Q2a recovery + Scroll test
  6339191 ckpt 352: alpha5 SHIPPED + RECORDED (run 42989432755 green, kestrel-g-v0.2.0-alpha5, APK
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
