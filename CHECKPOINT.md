# CHECKPOINT 454 — read me first, then TASKS.md

**Written:** 2026-11-11T19:18:43Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `37166fe` (this checkpoint is the commit after it)

## Just done
Owner request 8 recorded in TASKS.md (V0-V7: background downloads + notifications, swipe-to-quit, InShot-style video downloader, parallel/fast/pausable downloads, neon still invisible, speed review). Plan: alpha10 = V1-V3+V6, alpha11 = V4-V5, alpha12 = V7

## Do this next
Start V1-V3: read download/DownloadService.kt, Notifier.kt, update/UpdateManager.kt (download thread), BrowserActivity quit/onDestroy; add WakeLock+WifiLock to the service, move the update download into the engine, remove the Quit menu item, onDestroy/onTaskRemoved process exit when idle. Then V6 neon (drop battery-saver and idle-rest holds, Settings status line + 10 s test).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  df62246 ckpt 451: alpha9 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha9, gecko code 108, run 430843
  8ff3900 ckpt 448: alpha9 DISPATCHED: Actions run 43084348983 (gecko code 108, 19:08Z). alpha8 (c
  3b0bd85 ckpt 447: gated kestrel-g-v0.2.0-alpha9 (gecko code 108) and pushed it: handoff self-tes
  30794d6 ckpt 446: pre-ship: alpha9: the update screen's storage numbers now match what Android r
  e822130 ckpt 442: alpha8 build DISPATCHED: Actions run 43080438606 (android.yml, ref ccr-a57c163
  18ccff9 ckpt 441: gated kestrel-g-v0.2.0-alpha8 (gecko code 107) and pushed it: handoff self-tes
  e3285db ckpt 438: U1 cause found + fixed in code: Android needs 10% of the partition free AFTER 
  b39879a ckpt 427: Owner message after alpha7 recorded as TASKS U1: in-place updates never instal
  378cb59 ckpt 423: alpha7 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha7, gecko code 106, run 430722
  d87281f ckpt 419: alpha7 build DISPATCHED: Actions run 43072284547 (android.yml, ref ccr-a57c163
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
