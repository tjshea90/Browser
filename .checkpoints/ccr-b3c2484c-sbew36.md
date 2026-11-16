# CHECKPOINT 2538 — read me first, then TASKS.md

**Written:** 2026-11-16T17:44:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `7bd082f2` (this checkpoint is the commit after it)

## Just done
ANS58c FIX coded + tested: private tabs are NEVER put to sleep by the memory handling (core LiveBudget.mayHibernate / mayHibernateInBackground; BrowserActivity.hibernate refuses private, enforceLiveLimit + onTrimMemory ask the rule) and a hidden private ANCHOR session stays open while any private tab exists (syncPrivateAnchor: the engine forgets private logins when its LAST private session closes; a session replaced after its process was killed can no longer be the last); LiveBudgetTest + OwnerRulesTest guard green; lab suite pv written (soaklab /pv login-cookie page, debug privtab/normtab, soak.py pv_scene: Home + am send-trim-memory MODERATE/COMPLETE + back; private: no sleep, no reload, cookie kept; normal tab control)

## Do this next
1) read the gp lab run 43920043728 (artifacts when done; head 3fb0e402 has the hold fix + photos while dragging) and fix; 2) dispatch lab pv (android.yml app=lab lab_suite=pv lab_minutes=1) and read it; 3) ANS58a/b: (cheap, plausible) exclude challenge frames (challenges.cloudflare.com, hcaptcha, recaptcha ...) from ALL content scripts in kestrel-shields/manifest.json (exclude_matches): our MAIN-world scripts patch prototypes in every frame and cost ~450 KB of script per frame on the slow CPU; ask the owner to send Diagnostics right after a slow check (CPU by process shows who is busy); 4) lab vp then ship alpha60 (inline + gestures + private fix).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  c0017fa7 ckpt 2537: owner message 58 written into TASKS.md (ANS58a-d): a PRIVATE tab was reloade
  3fb0e402 ckpt 2536: ANS57 lab scene: the gesture info and the level bars are now photographed wh
  9a20b91e ckpt 2535: ANS57 lab gp run 1 (43918159286): volume / brightness drags, dead zone, plai
  e3cd26c4 ckpt 2534: ANS57: lab suite gp DISPATCHED: run 43918159286 (head 55e61f02, started 17:2
  55e61f02 ckpt 2533: ANS57 coded end to end + lab scene written: gp suite (soak.py gp_scene: A = 
  33203ec9 ckpt 2532: ANS57 glue coded + compiles: VideoLevels (music-stream volume, window bright
  0e7f4377 ckpt 2531: ANS55f: lab suite ip run 2 (43914267812): ALL 21 CHECKS OK on the real engin
  45054263 ckpt 2530: owner message 57 written into TASKS.md (ANS57a-f): double tap on a side must
  736bac20 ckpt 2529: ANS55f lab run 1 (ip, 43911183498): the inline player WORKS on the real engi
  6d700eee ckpt 2528: ANS55f: lab run 1 of suite ip DISPATCHED: run 43911183498 (head 72df6d09, st
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
