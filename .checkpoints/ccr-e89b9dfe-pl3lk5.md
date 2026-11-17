# CHECKPOINT 2640 — read me first, then TASKS.md

**Written:** 2026-11-17T17:55:35Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `b56c8822` (this checkpoint is the commit after it)

## Just done
Diagnostics line 'start oat' added (BrowserActivity.oatSizes: sizes of oat/<isa>/ files next to the base APK = what Android compiled at install; guard in OwnerRulesTest.lab7WholeAppIsCompiledAtInstall, compile + core tests pass). Confirmation lab run 44137227924 still running (started 17:27:41Z).

## Do this next
wait for run 44137227924 (artifact appears when it ends, ~18:20Z); then read AOT-REPORT.md and fill 'Run 2' in research/LAB7_RESULTS.md; then alpha67 steps (see previous ckpt next)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M core/src/test/kotlin/app/kestrel/core/OwnerRulesTest.kt

## Last ten checkpoints
```
  63fdb931 ckpt 2639: LAB7 CONFIRMATION lab run DISPATCHED ONCE: android.yml run 44137227924 (run_
  ebd3854a ckpt 2638: LAB7 first lab run 44126148207 READ (research/LAB7_RESULTS.md next): (1) the
  5b6a24c8 ckpt 2637: LAB7 CORRECTED lab run DISPATCHED ONCE: android.yml run 44126148207 (run_num
  e40a5b70 ckpt 2636: LAB7 lab run 44125632537 CANCELLED ON PURPOSE and replaced by a corrected on
  3206d5dd ckpt 2635: LAB7 lab run DISPATCHED ONCE (android.yml app=lab lab_suite=aot lab_profile=
  eaa4e46d ckpt 2634: LAB7a+b lab code written and dry-run against a fake adb (aotlab.py breadth_c
  a2313194 ckpt 2634: gated kestrel-g-v0.3.0-alpha66 (gecko code 167) and pushed it: handoff self-
  60f8deca ckpt 2633: alpha65 SHIPPED and RECORDED (run 44120758574 all 7 jobs green, Release kest
  f7d26151 ckpt 2632: alpha65 run is android.yml run 44120758574 (run_number 216, started 16:00:17
  0fa23623 ckpt 2631: alpha65 DISPATCHED ONCE (compile at install = LAB6: release script publishes
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
