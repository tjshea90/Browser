# CHECKPOINT 441 — read me first, then TASKS.md

**Written:** 2026-11-11T18:43:10Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `2f46e61` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.2.0-alpha8 (gecko code 107) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha8: finds why updates would not install over the old version - Android only finishes an install while a tenth of the tablet's storage stays free afterwards (about 1.4 GB free on a 16 GB tablet), which uninstalling first quietly fixed. The Updates screen now tells you the exact amount to free before it downloads anything, offers to delete old Kestrel installers from Downloads, Diagnostics shows the storage numbers"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.2.0-alpha8), then:  bash tools/record-release.sh kestrel-g-v0.2.0-alpha8 "alpha8: finds why updates would not install over the old version - Android only finishes an install while a tenth of the tablet's storage stays free afterwards (about 1.4 GB free on a 16 GB tablet), which uninstalling first quietly fixed. The Updates screen now tells you the exact amount to free before it downloads anything, offers to delete old Kestrel installers from Downloads, Diagnostics shows the storage numbers"
NEVER ship kestrel-g-v0.2.0-alpha8 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e3285db ckpt 438: U1 cause found + fixed in code: Android needs 10% of the partition free AFTER 
  b39879a ckpt 427: Owner message after alpha7 recorded as TASKS U1: in-place updates never instal
  378cb59 ckpt 423: alpha7 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha7, gecko code 106, run 430722
  d87281f ckpt 419: alpha7 build DISPATCHED: Actions run 43072284547 (android.yml, ref ccr-a57c163
  5f068ba ckpt 418: gated kestrel-g-v0.2.0-alpha7 (gecko code 106) and pushed it: handoff self-tes
  903dc4e ckpt 413: R2 first pass DONE on the emulator: Gecko source says display-port multipliers
  6cbe9d9 ckpt 406: R3 pull-down-to-refresh DONE + emulator-verified (KestrelGeckoView pull tracki
  a2797c7 ckpt 400: R1 neon DONE on the emulator (system animations off): pulse no longer gated on
  dec2a93 ckpt 392: S1+S2 code done: updater storage plan (core UpdateLogic.storagePlan + tests; U
  f0bff4b ckpt 384: Owner request 7 recorded (S1 in-app update blocked by INSTALL_FAILED_INSUFFICI
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
