# CHECKPOINT 438 — read me first, then TASKS.md

**Written:** 2026-11-11T18:37:07Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `d5d97a5` (this checkpoint is the commit after it)

## Just done
U1 cause found + fixed in code: Android needs 10% of the partition free AFTER any install (plus the app's staged APK + unpacked libs + dexopt); measured on the emulator (passes at 1050 MiB, fails at 1000; fresh install fails at the same point; installd 'Failed to free up 624066560'). Updater model rewritten (UpdateLogic.storagePlan, 12 tests green), NEEDS_SPACE shows reserve/need/free + 'Delete old installers', Diagnostics Storage line, release notes hint, research/UPDATE_STORAGE.md

## Do this next
Finish the emulator check of the 'Delete old installers' button (a kestrel-g-9.9.9 (1).apk download was running in the emulator, filler /data/local/tmp/big.bin keeps free at ~880 MiB), then bump to alpha8 (geckoapp versionCode 107 / 0.2.0-alpha8 + tools/sync-ext-version.sh), gate, ship.sh gecko, dispatch android.yml, record-release, reply to the owner: certificate is fine, 10% rule explained in plain words, what to free (about 1.4 GB on a 16 GB tablet; delete old Kestrel installers in Downloads), ask for Settings > Storage total/free

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  b39879a ckpt 427: Owner message after alpha7 recorded as TASKS U1: in-place updates never instal
  378cb59 ckpt 423: alpha7 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha7, gecko code 106, run 430722
  d87281f ckpt 419: alpha7 build DISPATCHED: Actions run 43072284547 (android.yml, ref ccr-a57c163
  5f068ba ckpt 418: gated kestrel-g-v0.2.0-alpha7 (gecko code 106) and pushed it: handoff self-tes
  903dc4e ckpt 413: R2 first pass DONE on the emulator: Gecko source says display-port multipliers
  6cbe9d9 ckpt 406: R3 pull-down-to-refresh DONE + emulator-verified (KestrelGeckoView pull tracki
  a2797c7 ckpt 400: R1 neon DONE on the emulator (system animations off): pulse no longer gated on
  dec2a93 ckpt 392: S1+S2 code done: updater storage plan (core UpdateLogic.storagePlan + tests; U
  f0bff4b ckpt 384: Owner request 7 recorded (S1 in-app update blocked by INSTALL_FAILED_INSUFFICI
  fcf63d9 ckpt 382: Owner request 6 recorded in TASKS.md (R1 neon not visible, R2 scrolling stutte
```

(10 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
