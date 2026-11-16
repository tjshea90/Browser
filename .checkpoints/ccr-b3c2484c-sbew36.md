# CHECKPOINT 2535 — read me first, then TASKS.md

**Written:** 2026-11-16T17:34:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `b7b5a5c4` (this checkpoint is the commit after it)

## Just done
ANS57 lab gp run 1 (43918159286): volume / brightness drags, dead zone, plain sideways = nothing, double tap puts the controls away ALL OK on the real engine; hold + drag FAILED in A and B: the loaded emulator's UI thread handled the first MOVE before the long-press timer rang -> FIX: PlayerGestures takes the events' own times (a finger that rested >= 350 ms before it moved is a long press even when the timer is late) + test; gesture log lines rate-limited, gesture-armed logs xdpi/view/range

## Do this next
Dispatch lab gp again (android.yml app=lab lab_suite=gp lab_minutes=1) on this head and read it; then lab vp; then ship alpha60.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e3cd26c4 ckpt 2534: ANS57: lab suite gp DISPATCHED: run 43918159286 (head 55e61f02, started 17:2
  55e61f02 ckpt 2533: ANS57 coded end to end + lab scene written: gp suite (soak.py gp_scene: A = 
  33203ec9 ckpt 2532: ANS57 glue coded + compiles: VideoLevels (music-stream volume, window bright
  0e7f4377 ckpt 2531: ANS55f: lab suite ip run 2 (43914267812): ALL 21 CHECKS OK on the real engin
  45054263 ckpt 2530: owner message 57 written into TASKS.md (ANS57a-f): double tap on a side must
  736bac20 ckpt 2529: ANS55f lab run 1 (ip, 43911183498): the inline player WORKS on the real engi
  6d700eee ckpt 2528: ANS55f: lab run 1 of suite ip DISPATCHED: run 43911183498 (head 72df6d09, st
  72df6d09 ckpt 2527: ANS55f: inline player coded + unit suites ALL green (core, app, geckoapp; gu
  441aa19c ckpt 2526: ANS55f step 2 coded and COMPILES (:geckoapp:compileDebugKotlin ok): PlayerFa
  16795a42 ckpt 2525: ANS55f step 1 done: gate-main.js geomOf + content.js geo() + background.js g
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
