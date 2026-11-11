# CHECKPOINT 476 — read me first, then TASKS.md

**Written:** 2026-11-11T20:07:12Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `d69353d` (this checkpoint is the commit after it)

## Just done
alpha10 DISPATCHED on GitHub Actions: run 43093416759 (android.yml, ref ccr-a57c163e-mtq8js, head d69353d, started 20:06Z; the previous run took ~6 min). Emulator verification of V1-V3 complete (swipe-away idle = all processes end; swipe-away during a download = survives, download completes, all 4 processes (incl. crash helper) exit ~7 s after the last byte)

## Do this next
1) wait for run 43093416759: mcp__github__actions_list list_workflow_runs android.yml branch ccr-a57c163e-mtq8js; green = status completed + conclusion success; 2) mcp__github__get_release_by_tag kestrel-g-v0.2.0-alpha10 shows the APK; 3) bash tools/record-release.sh kestrel-g-v0.2.0-alpha10 with the alpha10 note (same text as the dispatch); 4) reply to owner with the Release link on its own line + honest caveats (emulator only; Android 10 install prompt is mandatory; swipe-away leaves the engine in memory while a download runs; neon unverified on tablet); 5) meanwhile V4 P0 per research/VIDEO_DOWNLOADER.md section 9: extension sniffer (onHeadersReceived, types media/xmlhttprequest/other) -> ShieldsBridge 'media' message -> Tab.media list -> longPress TYPE_VIDEO/AUDIO + menu entry -> quality sheet -> DownloadController.enqueue(Source fetcher=1)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d69353d ckpt 475: gated kestrel-g-v0.2.0-alpha10 (gecko code 109) and pushed it: handoff self-te
  80e35c0 ckpt 473: V3 verified on the emulator (idle swipe-away = all processes end, active downl
  9b84b98 ckpt 468: Request 8 first batch CODE (compiles, tests+lint green, NOT yet run on the emu
  ded35f9 ckpt 454: Owner request 8 recorded in TASKS.md (V0-V7: background downloads + notificati
  df62246 ckpt 451: alpha9 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha9, gecko code 108, run 430843
  8ff3900 ckpt 448: alpha9 DISPATCHED: Actions run 43084348983 (gecko code 108, 19:08Z). alpha8 (c
  3b0bd85 ckpt 447: gated kestrel-g-v0.2.0-alpha9 (gecko code 108) and pushed it: handoff self-tes
  30794d6 ckpt 446: pre-ship: alpha9: the update screen's storage numbers now match what Android r
  e822130 ckpt 442: alpha8 build DISPATCHED: Actions run 43080438606 (android.yml, ref ccr-a57c163
  18ccff9 ckpt 441: gated kestrel-g-v0.2.0-alpha8 (gecko code 107) and pushed it: handoff self-tes
```
