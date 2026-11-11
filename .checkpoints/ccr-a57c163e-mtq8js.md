# CHECKPOINT 473 — read me first, then TASKS.md

**Written:** 2026-11-11T20:00:42Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `aa48208` (this checkpoint is the commit after it)

## Just done
V3 verified on the emulator (idle swipe-away = all processes end, active download survives the swipe and the process exits ~6 s after the last byte); ProcessLife now also ends the crash-helper sibling process; dismissOverlays() before the Updates sheet on a notification tap; engine destKind + AppSinks/FileSinks tests; research/VIDEO_DOWNLOADER.md written from the research agent (HLS/DASH/muxing/extension limits)

## Do this next
1) run gate :core:test :app:testDebugUnitTest :app:compileDebugKotlin :geckoapp:compileDebugKotlin :app:lintDebug 2) rebuild+install debug apk on emulator and re-run bash scratchpad swipe_test (motionevent drag works, input swipe is flaky) to confirm the crash helper now exits too 3) bump geckoapp versionCode 109 / 0.2.0-alpha10 + bash tools/sync-ext-version.sh, bash ship.sh gecko note, dispatch android.yml (app=gecko full_build=true), record-release after green 4) reply to owner (honest: Android 10 cannot install silently; DRM/YouTube; video downloader = alpha11 per research/VIDEO_DOWNLOADER.md P0-P1; V3b download-only process idea) 5) then V4 P0: extension sniffer + long-press Download video + quality sheet

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  9b84b98 ckpt 468: Request 8 first batch CODE (compiles, tests+lint green, NOT yet run on the emu
  ded35f9 ckpt 454: Owner request 8 recorded in TASKS.md (V0-V7: background downloads + notificati
  df62246 ckpt 451: alpha9 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha9, gecko code 108, run 430843
  8ff3900 ckpt 448: alpha9 DISPATCHED: Actions run 43084348983 (gecko code 108, 19:08Z). alpha8 (c
  3b0bd85 ckpt 447: gated kestrel-g-v0.2.0-alpha9 (gecko code 108) and pushed it: handoff self-tes
  30794d6 ckpt 446: pre-ship: alpha9: the update screen's storage numbers now match what Android r
  e822130 ckpt 442: alpha8 build DISPATCHED: Actions run 43080438606 (android.yml, ref ccr-a57c163
  18ccff9 ckpt 441: gated kestrel-g-v0.2.0-alpha8 (gecko code 107) and pushed it: handoff self-tes
  e3285db ckpt 438: U1 cause found + fixed in code: Android needs 10% of the partition free AFTER 
  b39879a ckpt 427: Owner message after alpha7 recorded as TASKS U1: in-place updates never instal
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
