# CHECKPOINT 2536 — read me first, then TASKS.md

**Written:** 2026-11-16T17:35:42Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `00ce5a14` (this checkpoint is the commit after it)

## Just done
ANS57 lab scene: the gesture info and the level bars are now photographed while the finger is down (raw_path end_hold_s + with_shot threads); the queued gp run 43919867824 (older scene) cancelled to save runner minutes

## Do this next
Dispatch lab gp (android.yml app=lab lab_suite=gp lab_minutes=1) on this head; read the report + the shots (gp-A-2-volume-while-dragging, gp-A-3-brightness-while-dragging, gp-A-4-seek-info-while-dragging) with the Read tool; fix; then lab vp; then ship alpha60.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  9a20b91e ckpt 2535: ANS57 lab gp run 1 (43918159286): volume / brightness drags, dead zone, plai
  e3cd26c4 ckpt 2534: ANS57: lab suite gp DISPATCHED: run 43918159286 (head 55e61f02, started 17:2
  55e61f02 ckpt 2533: ANS57 coded end to end + lab scene written: gp suite (soak.py gp_scene: A = 
  33203ec9 ckpt 2532: ANS57 glue coded + compiles: VideoLevels (music-stream volume, window bright
  0e7f4377 ckpt 2531: ANS55f: lab suite ip run 2 (43914267812): ALL 21 CHECKS OK on the real engin
  45054263 ckpt 2530: owner message 57 written into TASKS.md (ANS57a-f): double tap on a side must
  736bac20 ckpt 2529: ANS55f lab run 1 (ip, 43911183498): the inline player WORKS on the real engi
  6d700eee ckpt 2528: ANS55f: lab run 1 of suite ip DISPATCHED: run 43911183498 (head 72df6d09, st
  72df6d09 ckpt 2527: ANS55f: inline player coded + unit suites ALL green (core, app, geckoapp; gu
  441aa19c ckpt 2526: ANS55f step 2 coded and COMPILES (:geckoapp:compileDebugKotlin ok): PlayerFa
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
