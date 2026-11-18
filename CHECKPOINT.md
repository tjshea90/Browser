# CHECKPOINT 2674 — read me first, then TASKS.md

**Written:** 2026-11-18T06:04:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `2394631f` (this checkpoint is the commit after it)

## Just done
Lab run 44245067663 read: memory evidence WORKS on Android 10 (per-process RSS in the Last run note); 3 lab-scene faults found and fixed: freeze command ran inside onNewIntent (between pause and resume, watchdog off) now posted; taps in the middle hit the download window (now left edge); keep variant left a Download dialog open that blocked click (now declined) and its stream was only 12 s (now a 3-minute playlist /hls/long so the capture rule applies). App code unchanged apart from the lab-only freeze command.

## Do this next
dispatch ONE lab again: android.yml app=lab lab_suite=new lab_profile=standard lab_net=full lab_minutes=5; expect lr freeze says SCREEN THREAD WAS FROZEN with a ui STALL stack in the note; vs keep: frame-gone line present AND pick not none (status ok); click ok; then ship alpha70 (tree is already bumped to 171, note in docs/handoff/ALPHA70_RELEASE_NOTE.txt)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  220d6189 ckpt 2673: Container restarted at ~05:52Z (a background wait was lost; repo and checkpo
  c6dba2a4 ckpt 2672: Lab run 44245067663 (suite new = lr freeze/kill + vs incl. keep + ql) DISPAT
  5ce8de0c ckpt 2671: CR1g memory evidence in the Last run note (VmRSS of every app process, MemFr
  97f69909 ckpt 2670: Owner answered CR1f-c: the app vanished to the home screen = process death, 
  1ac8a776 ckpt 2669: Owner message VS2 written into TASKS (doraby: vidsrc + 111movies find no dow
  676dd260 ckpt 2668: alpha69 RELEASED: tag kestrel-g-v0.3.0-alpha69, run 44239780214 all jobs gre
  cf070608 ckpt 2667: alpha69 DISPATCHED ONCE at 04:47Z: android.yml run 44239780214 (run_number 2
  3f51b70a ckpt 2666: gated kestrel-g-v0.3.0-alpha69 (gecko code 170) and pushed it: handoff self-
  de1a9d82 ckpt 2665: Lab run 44236411726 suite new ALL GREEN (13 scenes, 0 issues): dm 9 checks, 
  1c980267 ckpt 2664: Lab run 44233990309 (suite new): dm scenes all OK; FOUND+FIXED: background.j
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
