# CHECKPOINT 2541 — read me first, then TASKS.md

**Written:** 2026-11-16T17:50:09Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `27fb2adf` (this checkpoint is the commit after it)

## Just done
owner message 59 written into TASKS.md (ANS59a-b): 'It should work for regular tabs in addition to private tabs' = a REGULAR tab must also come back unchanged after switching apps (no memory-handling sleep of the tab in front just because the app was left). Lab gv run 43921764750 still running.

## Do this next
1) core LiveBudget.mayHibernateInBackground(isPrivate, awayMs, level) = not private AND level >= 80 AND away >= 30 min; BrowserActivity.onTrimMemory passes the away time (pausedAt) and the level; update LiveBudgetTest + the ANS58c OwnerRules guard; 2) soak.py pv_scene control: the NORMAL tab is also not slept/reloaded after Home + trim; 3) read lab gv artifacts when done; 4) vp; ship alpha60; answer 55, 57, 58, 59.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  66aa33a7 ckpt 2540: ANS58a/b first cheap measure: kestrel-shields manifest exclude_matches for t
  a23e57dc ckpt 2539: lab suite gv = gp + pv in ONE emulator boot (the queue of GitHub was slow: t
  c70f2aac ckpt 2538: ANS58c FIX coded + tested: private tabs are NEVER put to sleep by the memory
  c0017fa7 ckpt 2537: owner message 58 written into TASKS.md (ANS58a-d): a PRIVATE tab was reloade
  3fb0e402 ckpt 2536: ANS57 lab scene: the gesture info and the level bars are now photographed wh
  9a20b91e ckpt 2535: ANS57 lab gp run 1 (43918159286): volume / brightness drags, dead zone, plai
  e3cd26c4 ckpt 2534: ANS57: lab suite gp DISPATCHED: run 43918159286 (head 55e61f02, started 17:2
  55e61f02 ckpt 2533: ANS57 coded end to end + lab scene written: gp suite (soak.py gp_scene: A = 
  33203ec9 ckpt 2532: ANS57 glue coded + compiles: VideoLevels (music-stream volume, window bright
  0e7f4377 ckpt 2531: ANS55f: lab suite ip run 2 (43914267812): ALL 21 CHECKS OK on the real engin
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
