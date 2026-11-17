# CHECKPOINT 2636 — read me first, then TASKS.md

**Written:** 2026-11-17T16:28:11Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `c7973046` (this checkpoint is the commit after it)

## Just done
LAB7 lab run 44125632537 CANCELLED ON PURPOSE and replaced by a corrected one (the update phase kept the app up only 20 s per launch, but Gecko writes its start-up cache some time after start-up, so the first-vs-later comparison would have been blind): aotlab.py update_check now keeps the app up --settle-s 70 s per launch, samples the start-up cache files at 20 s and at the end, and saves the Health lines of every launch of round 1 (update-health/*.txt in the artifact). Dry-run against a fake adb passes.

## Do this next
dispatch the corrected lab run ONCE (android.yml app=lab lab_suite=aot lab_profile=standard lab_net=full lab_minutes=3), record its run id here, wait, read artifact, write research/LAB7_RESULTS.md

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M .github/workflows/emu-lab.yml

## Last ten checkpoints
```
  3206d5dd ckpt 2635: LAB7 lab run DISPATCHED ONCE (android.yml app=lab lab_suite=aot lab_profile=
  eaa4e46d ckpt 2634: LAB7a+b lab code written and dry-run against a fake adb (aotlab.py breadth_c
  a2313194 ckpt 2634: gated kestrel-g-v0.3.0-alpha66 (gecko code 167) and pushed it: handoff self-
  60f8deca ckpt 2633: alpha65 SHIPPED and RECORDED (run 44120758574 all 7 jobs green, Release kest
  f7d26151 ckpt 2632: alpha65 run is android.yml run 44120758574 (run_number 216, started 16:00:17
  0fa23623 ckpt 2631: alpha65 DISPATCHED ONCE (compile at install = LAB6: release script publishes
  d32b8cf2 ckpt 2630: gated kestrel-g-v0.3.0-alpha65 (gecko code 166) and pushed it: handoff self-
  a6159180 ckpt 2629: OWNER SAID GO on LAB6 (compile at install through the updater). Recorded in 
  20e28803 ckpt 2628: LAB5 PASS COMPLETE. alpha64 shipped 11:52Z (https://github.com/tjshea90/Brow
  cda703be ckpt 2627: alpha64 SHIPPED + RECORDED (Release kestrel-g-v0.3.0-alpha64, run 4407646379
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
