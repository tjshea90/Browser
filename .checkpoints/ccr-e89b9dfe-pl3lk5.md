# CHECKPOINT 2639 — read me first, then TASKS.md

**Written:** 2026-11-17T17:28:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `fa04fb18` (this checkpoint is the commit after it)

## Just done
LAB7 CONFIRMATION lab run DISPATCHED ONCE: android.yml run 44137227924 (run_number 220, 17:27:41Z, head f003442b, lab_minutes=breadth: sp-current vs sp-all, 6 rounds, links opened in the running app, memory by kind). DO NOT DISPATCH ANOTHER aot RUN UNTIL IT IS READ. research/LAB7_RESULTS.md holds run 1 (first-launch-after-update: NO penalty, warm-up not built; compile everything costs 2.2 s on the emulator) with a placeholder for run 2.

## Do this next
wait for run 44137227924 (~50 min: 2 release builds + 6 rounds x 2 variants x ~2.5 min; poll with actions_get get_workflow_job 134... (job id from list_workflow_jobs once) every ~25 min via a background sleep timer); read artifact emulator-lab-text (list_workflow_run_artifacts, download_workflow_run_artifact, python3 -I tools/emu/fetch_artifact.py '<url>' <empty dir>), AOT-REPORT.md; fill 'Run 2' in research/LAB7_RESULTS.md; if no regression: alpha67 (versionCode 168 / 0.3.0-alpha67; bash tools/sync-ext-version.sh; docs/handoff/ALPHA67_RELEASE_NOTE.txt; bash ship.sh gecko with the note; ONE dispatch app=gecko full_build=true; verify jobs + Release assets incl .dm; bash tools/record-release.sh; tick LAB7 in TASKS; HANDOFF entry); if a regression: revert the last line of baseline-prof.txt and say so

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ebd3854a ckpt 2638: LAB7 first lab run 44126148207 READ (research/LAB7_RESULTS.md next): (1) the
  5b6a24c8 ckpt 2637: LAB7 CORRECTED lab run DISPATCHED ONCE: android.yml run 44126148207 (run_num
  e40a5b70 ckpt 2636: LAB7 lab run 44125632537 CANCELLED ON PURPOSE and replaced by a corrected on
  3206d5dd ckpt 2635: LAB7 lab run DISPATCHED ONCE (android.yml app=lab lab_suite=aot lab_profile=
  eaa4e46d ckpt 2634: LAB7a+b lab code written and dry-run against a fake adb (aotlab.py breadth_c
  a2313194 ckpt 2634: gated kestrel-g-v0.3.0-alpha66 (gecko code 167) and pushed it: handoff self-
  60f8deca ckpt 2633: alpha65 SHIPPED and RECORDED (run 44120758574 all 7 jobs green, Release kest
  f7d26151 ckpt 2632: alpha65 run is android.yml run 44120758574 (run_number 216, started 16:00:17
  0fa23623 ckpt 2631: alpha65 DISPATCHED ONCE (compile at install = LAB6: release script publishes
  d32b8cf2 ckpt 2630: gated kestrel-g-v0.3.0-alpha65 (gecko code 166) and pushed it: handoff self-
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
