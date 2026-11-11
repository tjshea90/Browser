# CHECKPOINT 427 — read me first, then TASKS.md

**Written:** 2026-11-11T18:05:06Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `1cea733` (this checkpoint is the commit after it)

## Just done
Owner message after alpha7 recorded as TASKS U1: in-place updates never install ('always uninstall first'), says free space was not the issue. Facts: same signing cert on all releases (checked), libs extracted at install (compressed in APK, 131.7 MB arm32) so in-place peak is about 2x a fresh install; emulator storage sweep running (scratchpad storage_sweep.sh/.log)

## Do this next
Read scratchpad storage_sweep.log (in-place vs fresh free-space thresholds), then decide the fix: smaller install peak (drop own APK copy after streaming it into the session, check what UpdateManager keeps), Back up/Restore feature, better updater diagnostics (show Android's raw status + free/allocatable numbers); ask the owner for Settings > Storage numbers and the exact error of a manual install

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  378cb59 ckpt 423: alpha7 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha7, gecko code 106, run 430722
  d87281f ckpt 419: alpha7 build DISPATCHED: Actions run 43072284547 (android.yml, ref ccr-a57c163
  5f068ba ckpt 418: gated kestrel-g-v0.2.0-alpha7 (gecko code 106) and pushed it: handoff self-tes
  903dc4e ckpt 413: R2 first pass DONE on the emulator: Gecko source says display-port multipliers
  6cbe9d9 ckpt 406: R3 pull-down-to-refresh DONE + emulator-verified (KestrelGeckoView pull tracki
  a2797c7 ckpt 400: R1 neon DONE on the emulator (system animations off): pulse no longer gated on
  dec2a93 ckpt 392: S1+S2 code done: updater storage plan (core UpdateLogic.storagePlan + tests; U
  f0bff4b ckpt 384: Owner request 7 recorded (S1 in-app update blocked by INSTALL_FAILED_INSUFFICI
  fcf63d9 ckpt 382: Owner request 6 recorded in TASKS.md (R1 neon not visible, R2 scrolling stutte
  4ac5081 ckpt 376: alpha6 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha6, gecko code 105, run 430556
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
