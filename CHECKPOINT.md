# CHECKPOINT 478 — read me first, then TASKS.md

**Written:** 2026-11-11T20:11:53Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `6d61701` (this checkpoint is the commit after it)

## Just done
alpha10 CI run 43093416759 FAILED in test-jvm: EngineTest.plainDownloadWithoutRangeSupport (a pre-existing race in the TEST: the view shows COMPLETED a moment before the store row is saved; it passed locally by luck). Fixed the 3 racy store assertions with a polling awaitRow helper; 288 JVM tests pass 3x under 6 busy CPU loops. No tag/release was created by the failed run

## Do this next
re-dispatch android.yml (app=gecko full_build=true, note = alpha10 text from ckpt 475) on this head; then wait for it: mcp__github__actions_list list_workflow_runs; when green: get_release_by_tag kestrel-g-v0.2.0-alpha10, bash tools/record-release.sh kestrel-g-v0.2.0-alpha10 <same note>, ckpt, reply to owner. Meanwhile V4 P0 (see research/VIDEO_DOWNLOADER.md section 9); ffmpeg exists at /usr/bin/ffmpeg for generating test media (mp4 faststart, HLS ts + fMP4, AES-128 HLS)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b9e656d ckpt 476: alpha10 DISPATCHED on GitHub Actions: run 43093416759 (android.yml, ref ccr-a5
  d69353d ckpt 475: gated kestrel-g-v0.2.0-alpha10 (gecko code 109) and pushed it: handoff self-te
  80e35c0 ckpt 473: V3 verified on the emulator (idle swipe-away = all processes end, active downl
  9b84b98 ckpt 468: Request 8 first batch CODE (compiles, tests+lint green, NOT yet run on the emu
  ded35f9 ckpt 454: Owner request 8 recorded in TASKS.md (V0-V7: background downloads + notificati
  df62246 ckpt 451: alpha9 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha9, gecko code 108, run 430843
  8ff3900 ckpt 448: alpha9 DISPATCHED: Actions run 43084348983 (gecko code 108, 19:08Z). alpha8 (c
  3b0bd85 ckpt 447: gated kestrel-g-v0.2.0-alpha9 (gecko code 108) and pushed it: handoff self-tes
  30794d6 ckpt 446: pre-ship: alpha9: the update screen's storage numbers now match what Android r
  e822130 ckpt 442: alpha8 build DISPATCHED: Actions run 43080438606 (android.yml, ref ccr-a57c163
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
