# CHECKPOINT 2635 — read me first, then TASKS.md

**Written:** 2026-11-17T16:26:09Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `056ea433` (this checkpoint is the commit after it)

## Just done
LAB7 lab run DISPATCHED ONCE (android.yml app=lab lab_suite=aot lab_profile=standard lab_net=full lab_minutes=3 on ref ccr-e89b9dfe-pl3lk5 at 16:3xZ; phases update + breadth). DO NOT DISPATCH IT AGAIN: find its run id with list_workflow_runs (event workflow_dispatch, branch ccr-e89b9dfe-pl3lk5, perPage 1) and wait. COORDINATION: alpha66 (versionCode 167, 0.3.0-alpha66) belongs to lane ccr-5531c47b-hl1gzo (Swoop keyboard link) - it merged my alpha65 tip and gated alpha66; my branch merged theirs (sync) so build.gradle.kts here already says 167. This lane's release is alpha67 = versionCode 168, only AFTER alpha66 is out (tag kestrel-g-v0.3.0-alpha66 on git ls-remote --tags origin); never ship the alpha66 tag from here.

## Do this next
1) find the run id of the aot lab run, poll list_workflow_jobs every few minutes in a background loop (the run needs ~90 min: update phase ~20 min, breadth ~50 min, plus 3 release builds); 2) when done read the artifact emulator-lab-text (list_workflow_run_artifacts, actions_get download_workflow_run_artifact, python3 -I tools/emu/fetch_artifact.py '<url>' <empty dir under scratchpad>) -> AOT-REPORT.md, profile-builds.txt; 3) write research/LAB7_RESULTS.md and decide per research/LAB7_INSTALL_COMPILE.md section 3 (ship the catch-all HL**;->**(**)** line if the rule holds; build the post-update warm-up only if the first launch after an update is >15 pct slower than steady state AND the warm-up removes it); 4) meanwhile prepare the code for the likely winner: baseline-prof.txt catch-all line + verify-apk.sh assertion + OwnerRulesTest source guard + tools/profile-to-baseline.py note

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  eaa4e46d ckpt 2634: LAB7a+b lab code written and dry-run against a fake adb (aotlab.py breadth_c
  a2313194 ckpt 2634: gated kestrel-g-v0.3.0-alpha66 (gecko code 167) and pushed it: handoff self-
  60f8deca ckpt 2633: alpha65 SHIPPED and RECORDED (run 44120758574 all 7 jobs green, Release kest
  f7d26151 ckpt 2632: alpha65 run is android.yml run 44120758574 (run_number 216, started 16:00:17
  0fa23623 ckpt 2631: alpha65 DISPATCHED ONCE (compile at install = LAB6: release script publishes
  d32b8cf2 ckpt 2630: gated kestrel-g-v0.3.0-alpha65 (gecko code 166) and pushed it: handoff self-
  a6159180 ckpt 2629: OWNER SAID GO on LAB6 (compile at install through the updater). Recorded in 
  20e28803 ckpt 2628: LAB5 PASS COMPLETE. alpha64 shipped 11:52Z (https://github.com/tjshea90/Brow
  cda703be ckpt 2627: alpha64 SHIPPED + RECORDED (Release kestrel-g-v0.3.0-alpha64, run 4407646379
  e5e2e612 ckpt 2626: alpha64 DISPATCHED ONCE at 11:42Z: android.yml run 44076463792 (app=gecko fu
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
