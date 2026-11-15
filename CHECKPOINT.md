# CHECKPOINT 2445 — read me first, then TASKS.md

**Written:** 2026-11-15T22:46:15Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `4ac0dfd0` (this checkpoint is the commit after it)

## Just done
ROT3 coded (not shipped): FullscreenTurner waits 650 ms of the tablet staying in its position before handing a one-time sideways turn back to the sensor (found in lab run 43763822027: portrait flash 0.45 s before landscape = two re-layouts); guard test + lab config-change counter; independent review of the FB4/ROT2 change set running in the background (agent a4b6a0705a3019258)

## Do this next
wait for the reviewer's notification and apply real findings (files: TurnSettleMachine, TurnSettler, BrowserActivity onPageViewport/tabOfReport/resyncEngineSize, turn.js); then ship alpha54 (ROT3 + review fixes) only with the owner's tablet report or at the next natural point; compat run 43770074964 ends about 00:40Z

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  f1e253aa ckpt 2444: alpha53 RELEASED and recorded (run 43770059374 green); owner message 48 ANSW
  f4fbfe56 ckpt 2443: alpha53 (ROT2: the cover and pause of a turn wait until the page has the new
  848ef753 ckpt 2442: gated kestrel-g-v0.3.0-alpha53 (gecko code 154) and pushed it: handoff self-
  3b961794 ckpt 2441: pre-ship: alpha53: turning the tablet under a full-screen video, after your 
  9d4609ad ckpt 2440: lab run 43766649065 (fsy with the slow_turn scene) read: ROT2 works on the r
  9cadf845 ckpt 2439: ROT2 complete in the tree and green: all Kotlin suites EXIT 0, npm test 453 
  359fe757 ckpt 2438: ROT2 coded (owner message 48): TurnSettle PAGE_CAP/NUDGE/HINT + settled(expe
  bd7e42fd ckpt 2437: owner message 48 (two screenshots of a turn: the page keeps the previous ori
  ac2107ef ckpt 2436: alpha52 RELEASED and recorded (run 43763812046 green, APK in the Release, BU
  1e089300 ckpt 2435: alpha52 shipped to GitHub (gates green, pushed): release run 43763812046 (an
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
