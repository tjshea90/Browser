# CHECKPOINT 2543 — read me first, then TASKS.md

**Written:** 2026-11-16T18:04:36Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `49771a07` (this checkpoint is the commit after it)

## Just done
lab touch fix coded (not yet run): raw dd finger paths never reached the app in gp/gv runs 43918159286 + 43921764750, so hold+drag seek and the inline level drags were never proven; Phone.motion_path (input motionevent) + touch-probe.txt + way probe in gp_scene, vertical drags back to input swipe with photos, pv scene checks the tab ids (privtab/normtab log tab=N), BrowserActivity.onDestroy logs 'life destroy finishing= config=' and a non-finishing destroy keeps the process 2 min (cancelled by the next onCreate); compile + selftest green

## Do this next
1) dispatch lab gv (android.yml app=lab lab_suite=gv) on this head and read touch-probe.txt + report; fix what it shows; 2) then lab vp; 3) ship alpha60 (note TESTED_PLACEHOLDER + private/regular tab paragraph); 4) answer owner 55, 57, 58, 59.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/ANS57_VLC_GESTURES.md

## Last ten checkpoints
```
  dcf083e4 ckpt 2542: ANS59a coded: regular tabs are not put to sleep by a trip to another app eit
  421a0dd9 ckpt 2541: owner message 59 written into TASKS.md (ANS59a-b): 'It should work for regul
  66aa33a7 ckpt 2540: ANS58a/b first cheap measure: kestrel-shields manifest exclude_matches for t
  a23e57dc ckpt 2539: lab suite gv = gp + pv in ONE emulator boot (the queue of GitHub was slow: t
  c70f2aac ckpt 2538: ANS58c FIX coded + tested: private tabs are NEVER put to sleep by the memory
  c0017fa7 ckpt 2537: owner message 58 written into TASKS.md (ANS58a-d): a PRIVATE tab was reloade
  3fb0e402 ckpt 2536: ANS57 lab scene: the gesture info and the level bars are now photographed wh
  9a20b91e ckpt 2535: ANS57 lab gp run 1 (43918159286): volume / brightness drags, dead zone, plai
  e3cd26c4 ckpt 2534: ANS57: lab suite gp DISPATCHED: run 43918159286 (head 55e61f02, started 17:2
  55e61f02 ckpt 2533: ANS57 coded end to end + lab scene written: gp suite (soak.py gp_scene: A = 
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
