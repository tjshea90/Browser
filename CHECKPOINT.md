# CHECKPOINT 2600 — read me first, then TASKS.md

**Written:** 2026-11-17T03:23:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `dd595cbe` (this checkpoint is the commit after it)

## Just done
LAB4 (owner message 63: do the other runs that should be done, then implement the findings into the app) written into TASKS.md in his words as LAB4a-d BEFORE any code. Plan: (1) add a site-tour step + idle-CPU + keyboard-resize lab steps to daily.py, dispatch fast-link and hotspot baselines (they queue behind run 5 and use the head at dispatch), (2) meanwhile implement candidates 3 and 2 in the app (time per call first), (3) after-run vs baseline, (4) ship one alpha

## Do this next
Read run 5 (id via list_workflow_runs perPage 1 branch+workflow_dispatch, newest; wake-up trig_01M81FL9EBuVKoGbGeoGxuD1 at 03:28Z): finalise nothing yet, just confirm the five lab fixes held; then daily.py: site-tour step, idle-CPU step, keyboard-resize note; commit; dispatch fast-link baseline then hotspot baseline

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  dd595cbe ckpt 2599: LAB3e: lab run 4 (44005894797) READ: Security GREEN 16 pass / 0 fail, camera
  025bf31d ckpt 2598: LAB3f: research/LAB3_IMPROVEMENTS.md now opens with the measured table of la
  2d31626a ckpt 2597: LAB3e: lab run 3 (44003337190) READ: all four lab false positives gone, Secu
  d06f3696 ckpt 2596: LAB3e: lab run 3 DISPATCHED ONCE: android.yml run 44003337190 (daily, head b
  b52ed4c3 ckpt 2595: LAB3e: lab run 2 (43997607118) READ. The whole pipeline works on the real em
  e61155d2 ckpt 2594: LAB3f: the speed ledger + 8 NEW ranked candidates are saved in research/LAB3
  a3b36287 ckpt 2593: LAB3 docs: research/LAB3_DAILY.md (how to start, what is matched / not, the 
  a1a955e8 ckpt 2592: LAB3e: lab run 2 DISPATCHED ONCE: android.yml run 43997607118 (app=lab lab_s
  c59a2c9a ckpt 2591: LAB3 run 1 (43997001918) failed at the Gradle step: Mozilla publishes NO gec
  3b71b73b ckpt 2590: LAB3d done: tools/emu/buildreport.py (build identity from geckoapp/build.gra
```
