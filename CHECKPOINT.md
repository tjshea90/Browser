# CHECKPOINT 468 — read me first, then TASKS.md

**Written:** 2026-11-11T19:29:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `0dba3e4` (this checkpoint is the commit after it)

## Just done
Request 8 first batch CODE (compiles, tests+lint green, NOT yet run on the emulator): neon holds reduced (no battery-saver/rest) + status line + 10 s test button; Quit removed, ProcessLife (swipe-away -> process exits unless a download runs), clear-on-exit moved to cold start; download engine: destKind (update APK in the app's private folder via FileSinks/AppSinks), thread priority follows the screen, Wi-Fi lock + wake lock in DownloadService; UpdateManager now downloads through the engine/service (hookEngine/syncFromEngine/finishDownload), UpdateNotifier (available/ready/failed/needs space/confirm), daily ReleaseCheckJob, Settings 'App updates' toggles, per-download Pause/Cancel/Retry notification actions

## Do this next
Build debug x86_64, install, test on the emulator: (1) Settings Neon status + Test button; (2) debug feed -> auto-download via service -> notification text, screen off (adb input keyevent KEYCODE_SLEEP) keeps downloading (rangeserver.py port 8001 throttled, check server.log for parallel Range requests while the screen is off); (3) swipe the task away: process must die when idle, survive with a download running; (4) ready notification + install tap. Then V4 video downloader once the research agent's note arrives.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ded35f9 ckpt 454: Owner request 8 recorded in TASKS.md (V0-V7: background downloads + notificati
  df62246 ckpt 451: alpha9 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha9, gecko code 108, run 430843
  8ff3900 ckpt 448: alpha9 DISPATCHED: Actions run 43084348983 (gecko code 108, 19:08Z). alpha8 (c
  3b0bd85 ckpt 447: gated kestrel-g-v0.2.0-alpha9 (gecko code 108) and pushed it: handoff self-tes
  30794d6 ckpt 446: pre-ship: alpha9: the update screen's storage numbers now match what Android r
  e822130 ckpt 442: alpha8 build DISPATCHED: Actions run 43080438606 (android.yml, ref ccr-a57c163
  18ccff9 ckpt 441: gated kestrel-g-v0.2.0-alpha8 (gecko code 107) and pushed it: handoff self-tes
  e3285db ckpt 438: U1 cause found + fixed in code: Android needs 10% of the partition free AFTER 
  b39879a ckpt 427: Owner message after alpha7 recorded as TASKS U1: in-place updates never instal
  378cb59 ckpt 423: alpha7 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha7, gecko code 106, run 430722
```

(13 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
