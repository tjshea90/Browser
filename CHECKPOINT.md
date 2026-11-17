# CHECKPOINT 2598 — read me first, then TASKS.md

**Written:** 2026-11-17T02:51:18Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `2d31626a` (this checkpoint is the commit after it)

## Just done
LAB3f: research/LAB3_IMPROVEMENTS.md now opens with the measured table of lab runs 2-3 (cold start 1.2 s UI block = candidate 3 confirmed, 0 hard stalls, film start 0.7 s, memory peak 291 MB with 7 of 8 tabs hibernated, blocker 0 asked one by one = candidate 1 not needed, security 14/0, Shields A/B n=2 hint that blocking costs time on a fast link) and the lab-lesson paragraph; TASKS LAB3h (repeat the Shields A/B 5x) and LAB3i (writable-system low-RAM profile) added. Run 4 is running (wake-up trig_019veErSUCaE1dD8TCLC9iQP at 03:07Z)

## Do this next
At the wake-up: find run 4's id (list_workflow_runs perPage 1 filter branch + workflow_dispatch, the newest), list_workflow_jobs, download build-report + emulator-lab-text, FINALISE per the previous Do-this-next (buildreport --notes research/LAB3_IMPROVEMENTS.md --write-baseline research/lab-baseline.json, docs/handoff/LABREPORT_0.3.0-alpha62.md, tick LAB3a-g, answer the owner, delete triggers trig_01K9rvGwTaZNy2NbUQeBeWMg and trig_01TfmPuNRp4XY2gZATmURc6Z)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/LAB3_IMPROVEMENTS.md

## Last ten checkpoints
```
  2d31626a ckpt 2597: LAB3e: lab run 3 (44003337190) READ: all four lab false positives gone, Secu
  d06f3696 ckpt 2596: LAB3e: lab run 3 DISPATCHED ONCE: android.yml run 44003337190 (daily, head b
  b52ed4c3 ckpt 2595: LAB3e: lab run 2 (43997607118) READ. The whole pipeline works on the real em
  e61155d2 ckpt 2594: LAB3f: the speed ledger + 8 NEW ranked candidates are saved in research/LAB3
  a3b36287 ckpt 2593: LAB3 docs: research/LAB3_DAILY.md (how to start, what is matched / not, the 
  a1a955e8 ckpt 2592: LAB3e: lab run 2 DISPATCHED ONCE: android.yml run 43997607118 (app=lab lab_s
  c59a2c9a ckpt 2591: LAB3 run 1 (43997001918) failed at the Gradle step: Mozilla publishes NO gec
  3b71b73b ckpt 2590: LAB3d done: tools/emu/buildreport.py (build identity from geckoapp/build.gra
  a198ecf8 ckpt 2589: LAB3e: first daily-suite run DISPATCHED ONCE: android.yml run 43997001918 (a
  f1cb9443 ckpt 2588: LAB3a/b/c code written: emu-lab.yml (profile input: tablet = 32-bit x86 Andr
```
