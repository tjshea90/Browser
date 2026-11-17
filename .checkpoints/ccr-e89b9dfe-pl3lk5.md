# CHECKPOINT 2634 — read me first, then TASKS.md

**Written:** 2026-11-17T16:24:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `53ce01e9` (this checkpoint is the commit after it)

## Just done
LAB7a+b lab code written and dry-run against a fake adb (aotlab.py breadth_check/update_check/report sections, packlab /ping page, tools/emu/profvariant.py exercised|all, emu-lab.yml aot step builds old/new debug APKs + three release APKs, workflow parses, test_handoff 69 passed); local facts: research/LAB7_INSTALL_COMPILE.md (profile compiles 2,553 of 14,996 methods = 17 pct; a single wildcard line HL**;->**(**)** makes all 14,996 hot, .dm 4,131 bytes; AGP expandReleaseArtProfileWildcards works; control build reproduces alpha65 .dm byte for byte)

## Do this next
dispatch ONE lab run: mcp__github__actions_run_trigger run_workflow android.yml ref ccr-e89b9dfe-pl3lk5 inputs app=lab lab_suite=aot lab_profile=standard lab_net=full lab_minutes=3; find the run id with list_workflow_runs (event workflow_dispatch, branch ccr-e89b9dfe-pl3lk5, perPage 1); poll jobs with list_workflow_jobs (small) not get_workflow_run; when done: list_workflow_run_artifacts, download emulator-lab-text via actions_get download_workflow_run_artifact then python3 -I tools/emu/fetch_artifact.py '<url>' <empty dir under scratchpad>; read AOT-REPORT.md; write research/LAB7_RESULTS.md; decide per the rule in research/LAB7_INSTALL_COMPILE.md section 3

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  60f8deca ckpt 2633: alpha65 SHIPPED and RECORDED (run 44120758574 all 7 jobs green, Release kest
  f7d26151 ckpt 2632: alpha65 run is android.yml run 44120758574 (run_number 216, started 16:00:17
  0fa23623 ckpt 2631: alpha65 DISPATCHED ONCE (compile at install = LAB6: release script publishes
  d32b8cf2 ckpt 2630: gated kestrel-g-v0.3.0-alpha65 (gecko code 166) and pushed it: handoff self-
  a6159180 ckpt 2629: OWNER SAID GO on LAB6 (compile at install through the updater). Recorded in 
  20e28803 ckpt 2628: LAB5 PASS COMPLETE. alpha64 shipped 11:52Z (https://github.com/tjshea90/Brow
  cda703be ckpt 2627: alpha64 SHIPPED + RECORDED (Release kestrel-g-v0.3.0-alpha64, run 4407646379
  e5e2e612 ckpt 2626: alpha64 DISPATCHED ONCE at 11:42Z: android.yml run 44076463792 (app=gecko fu
  d9efb312 ckpt 2625: gated kestrel-g-v0.3.0-alpha64 (gecko code 165) and pushed it: handoff self-
  4692623b ckpt 2624: RELEASE PREP (not shipped): versionCode 165 / versionName 0.3.0-alpha64 in g
```

(21 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
