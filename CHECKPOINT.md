# CHECKPOINT 475 — read me first, then TASKS.md

**Written:** 2026-11-11T20:06:37Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `9f35405` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.2.0-alpha10 (gecko code 109) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha10: updates and downloads now run in the background at full speed (screen off or locked: the update downloads like any file, with its own notifications for available, downloading, ready to install, failed and not enough space, plus Pause/Cancel/Retry on every download). The Quit menu item is gone: swipe the app away; downloads carry on, and when nothing is running the whole app (browser engine and its helper processes) exits so it costs no battery or memory. Neon pulse: fewer reasons to hold it, Settings shows why it is holding, with a 10 second test button. Tapping a notification now closes Settings first."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.2.0-alpha10), then:  bash tools/record-release.sh kestrel-g-v0.2.0-alpha10 "alpha10: updates and downloads now run in the background at full speed (screen off or locked: the update downloads like any file, with its own notifications for available, downloading, ready to install, failed and not enough space, plus Pause/Cancel/Retry on every download). The Quit menu item is gone: swipe the app away; downloads carry on, and when nothing is running the whole app (browser engine and its helper processes) exits so it costs no battery or memory. Neon pulse: fewer reasons to hold it, Settings shows why it is holding, with a 10 second test button. Tapping a notification now closes Settings first."
NEVER ship kestrel-g-v0.2.0-alpha10 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  80e35c0 ckpt 473: V3 verified on the emulator (idle swipe-away = all processes end, active downl
  9b84b98 ckpt 468: Request 8 first batch CODE (compiles, tests+lint green, NOT yet run on the emu
  ded35f9 ckpt 454: Owner request 8 recorded in TASKS.md (V0-V7: background downloads + notificati
  df62246 ckpt 451: alpha9 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha9, gecko code 108, run 430843
  8ff3900 ckpt 448: alpha9 DISPATCHED: Actions run 43084348983 (gecko code 108, 19:08Z). alpha8 (c
  3b0bd85 ckpt 447: gated kestrel-g-v0.2.0-alpha9 (gecko code 108) and pushed it: handoff self-tes
  30794d6 ckpt 446: pre-ship: alpha9: the update screen's storage numbers now match what Android r
  e822130 ckpt 442: alpha8 build DISPATCHED: Actions run 43080438606 (android.yml, ref ccr-a57c163
  18ccff9 ckpt 441: gated kestrel-g-v0.2.0-alpha8 (gecko code 107) and pushed it: handoff self-tes
  e3285db ckpt 438: U1 cause found + fixed in code: Android needs 10% of the partition free AFTER 
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
