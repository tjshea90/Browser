# CHECKPOINT 2542 — read me first, then TASKS.md

**Written:** 2026-11-16T17:51:53Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `3d8145a3` (this checkpoint is the commit after it)

## Just done
ANS59a coded: regular tabs are not put to sleep by a trip to another app either (LiveBudget.mayHibernateInBackground(isPrivate, awayMs, level): only a regular tab, only at trim level 80, only after 30 minutes away); tests + guard + compile green; pv lab control now expects the regular tab to stay too; note on ProcessLife.maybeExit (no isFinishing check) in research/ANS58...

## Do this next
1) read lab gv run 43921764750 (old pv control; gp with the hold fix and photos); fix findings; 2) dispatch lab gv again (new pv control for the regular tab); 3) add Health 'life destroy' (isFinishing, isChangingConfigurations) in BrowserActivity.onDestroy to settle the process-exit question from the next Diagnostics; 4) lab vp; ship alpha60; answer 55, 57, 58, 59.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/ANS58_PRIVATE_TAB_AND_CHALLENGE_PAGES.md

## Last ten checkpoints
```
  421a0dd9 ckpt 2541: owner message 59 written into TASKS.md (ANS59a-b): 'It should work for regul
  66aa33a7 ckpt 2540: ANS58a/b first cheap measure: kestrel-shields manifest exclude_matches for t
  a23e57dc ckpt 2539: lab suite gv = gp + pv in ONE emulator boot (the queue of GitHub was slow: t
  c70f2aac ckpt 2538: ANS58c FIX coded + tested: private tabs are NEVER put to sleep by the memory
  c0017fa7 ckpt 2537: owner message 58 written into TASKS.md (ANS58a-d): a PRIVATE tab was reloade
  3fb0e402 ckpt 2536: ANS57 lab scene: the gesture info and the level bars are now photographed wh
  9a20b91e ckpt 2535: ANS57 lab gp run 1 (43918159286): volume / brightness drags, dead zone, plai
  e3cd26c4 ckpt 2534: ANS57: lab suite gp DISPATCHED: run 43918159286 (head 55e61f02, started 17:2
  55e61f02 ckpt 2533: ANS57 coded end to end + lab scene written: gp suite (soak.py gp_scene: A = 
  33203ec9 ckpt 2532: ANS57 glue coded + compiles: VideoLevels (music-stream volume, window bright
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
