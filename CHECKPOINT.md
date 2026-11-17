# CHECKPOINT 2637 — read me first, then TASKS.md

**Written:** 2026-11-17T16:29:11Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `e40a5b70` (this checkpoint is the commit after it)

## Just done
LAB7 CORRECTED lab run DISPATCHED ONCE: android.yml run 44126148207 (run_number 219, queued 16:28:23Z, head e40a5b70 = ckpt 2636, app=lab lab_suite=aot lab_profile=standard lab_net=full lab_minutes=3). The first lab run 44125632537 was cancelled on purpose (see ckpt 2636). DO NOT DISPATCH ANOTHER aot RUN UNTIL THIS ONE IS READ. Expected length ~100-120 min (builds: 2 debug + 3 release, update phase ~25 min, breadth ~50 min). Poll with list_workflow_jobs 44126148207 (small), never get_workflow_run.

## Do this next
wait for run 44126148207 (check list_workflow_jobs about every 25 min; a background sleep is the timer); then list_workflow_run_artifacts 44126148207, download emulator-lab-text via actions_get download_workflow_run_artifact, python3 -I tools/emu/fetch_artifact.py '<url>' <empty dir under /tmp/claude-0/-home-user/9ce0a565-d773-5bbd-9051-9693a437ee59/scratchpad/lab7>, read AOT-REPORT.md + profile-builds.txt + update-health/*.txt, write research/LAB7_RESULTS.md, decide (rule: research/LAB7_INSTALL_COMPILE.md section 3). alpha66 is the OTHER lane's (versionCode 167); ship this lane's result as alpha67 (versionCode 168) after alpha66 is out

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e40a5b70 ckpt 2636: LAB7 lab run 44125632537 CANCELLED ON PURPOSE and replaced by a corrected on
  3206d5dd ckpt 2635: LAB7 lab run DISPATCHED ONCE (android.yml app=lab lab_suite=aot lab_profile=
  eaa4e46d ckpt 2634: LAB7a+b lab code written and dry-run against a fake adb (aotlab.py breadth_c
  a2313194 ckpt 2634: gated kestrel-g-v0.3.0-alpha66 (gecko code 167) and pushed it: handoff self-
  60f8deca ckpt 2633: alpha65 SHIPPED and RECORDED (run 44120758574 all 7 jobs green, Release kest
  f7d26151 ckpt 2632: alpha65 run is android.yml run 44120758574 (run_number 216, started 16:00:17
  0fa23623 ckpt 2631: alpha65 DISPATCHED ONCE (compile at install = LAB6: release script publishes
  d32b8cf2 ckpt 2630: gated kestrel-g-v0.3.0-alpha65 (gecko code 166) and pushed it: handoff self-
  a6159180 ckpt 2629: OWNER SAID GO on LAB6 (compile at install through the updater). Recorded in 
  20e28803 ckpt 2628: LAB5 PASS COMPLETE. alpha64 shipped 11:52Z (https://github.com/tjshea90/Brow
```
