# CHECKPOINT 2537 — read me first, then TASKS.md

**Written:** 2026-11-16T17:39:18Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `a53b4436` (this checkpoint is the commit after it)

## Just done
owner message 58 written into TASKS.md (ANS58a-d): a PRIVATE tab was reloaded and logged out after switching apps (cause suspected: BrowserActivity.onTrimMemory/hibernate closes the engine session, Gecko clears private cookies when its last private session closes); typing lag while a Cloudflare check loads; the check itself slow. Gesture lab run gp 2 is still running.

## Do this next
1) read the gp lab run (dispatched ~17:36Z; artifacts when done) and the web-run2 result; 2) ANS58c FIRST (clear bug): read BrowserActivity.onTrimMemory / hibernate / enforceLiveLimit / LiveBudget for private tabs: never hibernate or close a private tab's session by memory handling; core rule + OwnerRules guard; 3) ANS58b/a: look at what the app does on its UI thread while a page loads and a field is focused (captureLook, Neon, saveSessionNow, flushCounters, the shields round trips) and add a lab scene typing into a field during a heavy page load; 4) alpha60 ship includes these.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  3fb0e402 ckpt 2536: ANS57 lab scene: the gesture info and the level bars are now photographed wh
  9a20b91e ckpt 2535: ANS57 lab gp run 1 (43918159286): volume / brightness drags, dead zone, plai
  e3cd26c4 ckpt 2534: ANS57: lab suite gp DISPATCHED: run 43918159286 (head 55e61f02, started 17:2
  55e61f02 ckpt 2533: ANS57 coded end to end + lab scene written: gp suite (soak.py gp_scene: A = 
  33203ec9 ckpt 2532: ANS57 glue coded + compiles: VideoLevels (music-stream volume, window bright
  0e7f4377 ckpt 2531: ANS55f: lab suite ip run 2 (43914267812): ALL 21 CHECKS OK on the real engin
  45054263 ckpt 2530: owner message 57 written into TASKS.md (ANS57a-f): double tap on a side must
  736bac20 ckpt 2529: ANS55f lab run 1 (ip, 43911183498): the inline player WORKS on the real engi
  6d700eee ckpt 2528: ANS55f: lab run 1 of suite ip DISPATCHED: run 43911183498 (head 72df6d09, st
  72df6d09 ckpt 2527: ANS55f: inline player coded + unit suites ALL green (core, app, geckoapp; gu
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
