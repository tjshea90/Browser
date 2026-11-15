# CHECKPOINT 2407 — read me first, then TASKS.md

**Written:** 2026-11-15T17:35:53Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `5d3fd3bb` (this checkpoint is the commit after it)

## Just done
alpha48 RELEASED: tag kestrel-g-v0.3.0-alpha48 (commit 281c48f4, APK 94.7 MB, certificate verified), release run 43740388928 all jobs green (test-js, test-jvm, build-gecko), BUILDLOG.md recorded (commit 5d3fd3bb); TASKS ticked: RX4-E1, RX4-X2, FS1d (emulator part), VC1b/c, LP1b/c

## Do this next
1) answer the owner in plain words with the Release link https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha48 on its own line (not run on the tablet: the lab used an Android 10 x86_64 emulator with software GL), re-list open items; 2) hour soak 43740398387 ends about 18:35 container clock: download artifacts, write research/SOAK_RUN1.md, fix cheap findings (alpha49 only if worth it), then delete heartbeat trig_01Y8CFHKVb6hmQmEEM78p6gv

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  96a8ccce ckpt 2406: TASKS: VC1b/VC1c/LP1b/LP1c ticked with the lab evidence (run 43739450975); r
  fbbd7efc ckpt 2405: lab vc run 3 (43739450975, commit ecbdecc3) all green on the REAL engine inc
  2211e3fd ckpt 2404: gated kestrel-g-v0.3.0-alpha48 (gecko code 149) and pushed it: handoff self-
  b619f02e ckpt 2403: pre-ship: alpha48: videos with no controls now get Kestrel's own: tap a vide
  ecbdecc3 ckpt 2402: lab vc run 2 (43738479064, commit b5fd15c4) on the REAL Firefox engine: VC1 
  b5fd15c4 ckpt 2401: lab vc run 1 (43737466605) read: LP1 long press menu on all four lab pages a
  0acf028c ckpt 2400: lab driver: hotspot shaping without the emulator delay option (it made hacke
  4e233ebe ckpt 2399: VC1/LP1/RV1-RV9 verified: Kotlin suite (core + app + geckoapp tests + geckoa
  31e0d580 ckpt 2398: RV3 (updater spoof: fromUpdater needs the app-private download kind), RV4 (r
  a61cff67 ckpt 2397: RV1 (site orientation lock = Android constants, verified in the GeckoView 16
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
