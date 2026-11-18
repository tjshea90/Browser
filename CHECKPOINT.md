# CHECKPOINT 2675 — read me first, then TASKS.md

**Written:** 2026-11-18T06:04:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `d868b655` (this checkpoint is the commit after it)

## Just done
Lab run 44248671431 (suite new, head d868b655) DISPATCHED 06:04Z. alpha70 prepared (171), NOT shipped.

## Do this next
WAIT for run 44248671431 (list_workflow_run_artifacts every ~10 min; the ffmpeg step alone takes 13 min): expect lr freeze SCREEN THREAD WAS FROZEN, vs keep ok with frame-gone present, click ok, mse/msel play, msel0 held, ql ok. If green: bash ship.sh gecko with the note in docs/handoff/ALPHA70_RELEASE_NOTE.txt (cat), ONE dispatch app=gecko full_build=true, verify tag kestrel-g-v0.3.0-alpha70, record-release, answer owner. If not: fix the scene or the app, one lab at a time.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d868b655 ckpt 2674: Lab run 44245067663 read: memory evidence WORKS on Android 10 (per-process R
  220d6189 ckpt 2673: Container restarted at ~05:52Z (a background wait was lost; repo and checkpo
  c6dba2a4 ckpt 2672: Lab run 44245067663 (suite new = lr freeze/kill + vs incl. keep + ql) DISPAT
  5ce8de0c ckpt 2671: CR1g memory evidence in the Last run note (VmRSS of every app process, MemFr
  97f69909 ckpt 2670: Owner answered CR1f-c: the app vanished to the home screen = process death, 
  1ac8a776 ckpt 2669: Owner message VS2 written into TASKS (doraby: vidsrc + 111movies find no dow
  676dd260 ckpt 2668: alpha69 RELEASED: tag kestrel-g-v0.3.0-alpha69, run 44239780214 all jobs gre
  cf070608 ckpt 2667: alpha69 DISPATCHED ONCE at 04:47Z: android.yml run 44239780214 (run_number 2
  3f51b70a ckpt 2666: gated kestrel-g-v0.3.0-alpha69 (gecko code 170) and pushed it: handoff self-
  de1a9d82 ckpt 2665: Lab run 44236411726 suite new ALL GREEN (13 scenes, 0 issues): dm 9 checks, 
```
