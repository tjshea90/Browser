# CHECKPOINT 423 — read me first, then TASKS.md

**Written:** 2026-11-11T18:00:21Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `f5254fd` (this checkpoint is the commit after it)

## Just done
alpha7 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha7, gecko code 106, run 43072284547 green, APK 92.6 MB on the Release); PullTracker extracted to :core with 14 JVM tests and re-verified on the emulator; docs updated

## Do this next
Reply to the owner (done in chat). Then pick the next job: D1 (in-extension blocking engine, research/ADBLOCK_ENGINE.md section 3 P0: engine.js + background.js, KFS reader in JS, differential corpus vs the Kotlin engine) OR the P5/P6 polish (cold-start Home overlay glitch, tab-grid swipe-to-close, Settings restyle) - wait for the owner's alpha7 feedback first (Compare 3 feels numbers, whether the neon/pull/scroll changes show on the tablet). Not started.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M docs/ARCHITECTURE.md

## Last ten checkpoints
```
  d87281f ckpt 419: alpha7 build DISPATCHED: Actions run 43072284547 (android.yml, ref ccr-a57c163
  5f068ba ckpt 418: gated kestrel-g-v0.2.0-alpha7 (gecko code 106) and pushed it: handoff self-tes
  903dc4e ckpt 413: R2 first pass DONE on the emulator: Gecko source says display-port multipliers
  6cbe9d9 ckpt 406: R3 pull-down-to-refresh DONE + emulator-verified (KestrelGeckoView pull tracki
  a2797c7 ckpt 400: R1 neon DONE on the emulator (system animations off): pulse no longer gated on
  dec2a93 ckpt 392: S1+S2 code done: updater storage plan (core UpdateLogic.storagePlan + tests; U
  f0bff4b ckpt 384: Owner request 7 recorded (S1 in-app update blocked by INSTALL_FAILED_INSUFFICI
  fcf63d9 ckpt 382: Owner request 6 recorded in TASKS.md (R1 neon not visible, R2 scrolling stutte
  4ac5081 ckpt 376: alpha6 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha6, gecko code 105, run 430556
  69d37b2 ckpt 372: gated kestrel-g-v0.2.0-alpha6 (gecko code 105) and pushed it: handoff self-tes
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
