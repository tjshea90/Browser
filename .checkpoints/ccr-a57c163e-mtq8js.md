# CHECKPOINT 446 — read me first, then TASKS.md

**Written:** 2026-11-11T19:07:40Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `5540eb3` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha9: the update screen's storage numbers now match what Android really demands (measured: the new app is held twice while it installs, so an update needs a tenth of the storage plus about 0.37 GB free; on your tablet that is roughly 1.5 GB). Same fixes as alpha8: exact amount to free shown before anything is downloaded, delete old Kestrel installers from Downloads, storage numbers in Diagnostics

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/build.gradle.kts
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  e822130 ckpt 442: alpha8 build DISPATCHED: Actions run 43080438606 (android.yml, ref ccr-a57c163
  18ccff9 ckpt 441: gated kestrel-g-v0.2.0-alpha8 (gecko code 107) and pushed it: handoff self-tes
  e3285db ckpt 438: U1 cause found + fixed in code: Android needs 10% of the partition free AFTER 
  b39879a ckpt 427: Owner message after alpha7 recorded as TASKS U1: in-place updates never instal
  378cb59 ckpt 423: alpha7 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha7, gecko code 106, run 430722
  d87281f ckpt 419: alpha7 build DISPATCHED: Actions run 43072284547 (android.yml, ref ccr-a57c163
  5f068ba ckpt 418: gated kestrel-g-v0.2.0-alpha7 (gecko code 106) and pushed it: handoff self-tes
  903dc4e ckpt 413: R2 first pass DONE on the emulator: Gecko source says display-port multipliers
  6cbe9d9 ckpt 406: R3 pull-down-to-refresh DONE + emulator-verified (KestrelGeckoView pull tracki
  a2797c7 ckpt 400: R1 neon DONE on the emulator (system animations off): pulse no longer gated on
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
