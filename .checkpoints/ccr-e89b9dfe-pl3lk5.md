# CHECKPOINT 2599 — read me first, then TASKS.md

**Written:** 2026-11-17T03:10:23Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `bdc8a66c` (this checkpoint is the commit after it)

## Just done
LAB3e: lab run 4 (44005894797) READ: Security GREEN 16 pass / 0 fail, camera/microphone and location each raise a real BLOCK/ALLOW dialog (pass), Stability GREEN, Memory GREEN (peak 332 MB). FIFTH lab false positive found by reading the raw Health lines: typed-address FAIL was a stale event (the Google results page of the previous step finished 9.6 s late, the real en.wikipedia.org load came at +41 s): d_wait_page now takes a wanted host. Also: Shields A/B now 3 alternating repeats per mode with medians, http->https proven from the padlock description ('Connection is not secure' for http), tab-switch time only when an event was seen, private tab via its on-screen indicator, emulator-only scroll gaps rated low with the tablet's own measurement named

## Do this next
Dispatch lab run 5 ONCE (same inputs; id via list_workflow_runs perPage 1 filter branch + workflow_dispatch), read it; if it shows no lab fault FINALISE: buildreport --notes research/LAB3_IMPROVEMENTS.md --write-baseline research/lab-baseline.json on the run-5 dir, copy BUILD-REPORT.md to docs/handoff/LABREPORT_0.3.0-alpha62.md, tick LAB3a-g with evidence, answer the owner, delete triggers trig_01K9rvGwTaZNy2NbUQeBeWMg and trig_01TfmPuNRp4XY2gZATmURc6Z and trig_019veErSUCaE1dD8TCLC9iQP. If run 5 still shows a lab fault, fix it and FINALISE anyway with that fault named (do not loop more than once more)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M tools/emu/buildreport.py
     M tools/emu/daily.py

## Last ten checkpoints
```
  025bf31d ckpt 2598: LAB3f: research/LAB3_IMPROVEMENTS.md now opens with the measured table of la
  2d31626a ckpt 2597: LAB3e: lab run 3 (44003337190) READ: all four lab false positives gone, Secu
  d06f3696 ckpt 2596: LAB3e: lab run 3 DISPATCHED ONCE: android.yml run 44003337190 (daily, head b
  b52ed4c3 ckpt 2595: LAB3e: lab run 2 (43997607118) READ. The whole pipeline works on the real em
  e61155d2 ckpt 2594: LAB3f: the speed ledger + 8 NEW ranked candidates are saved in research/LAB3
  a3b36287 ckpt 2593: LAB3 docs: research/LAB3_DAILY.md (how to start, what is matched / not, the 
  a1a955e8 ckpt 2592: LAB3e: lab run 2 DISPATCHED ONCE: android.yml run 43997607118 (app=lab lab_s
  c59a2c9a ckpt 2591: LAB3 run 1 (43997001918) failed at the Gradle step: Mozilla publishes NO gec
  3b71b73b ckpt 2590: LAB3d done: tools/emu/buildreport.py (build identity from geckoapp/build.gra
  a198ecf8 ckpt 2589: LAB3e: first daily-suite run DISPATCHED ONCE: android.yml run 43997001918 (a
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
