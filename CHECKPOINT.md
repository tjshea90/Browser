# CHECKPOINT 512 — read me first, then TASKS.md

**Written:** 2026-11-11T21:15:28Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `4c27f5b` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.2.0-alpha11 (gecko code 110) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha11: Download video. Play a video, then long-press it (or open Menu > Download video) and pick a quality: every row shows its name and size, like '720p - MP4 - 48 MB'. It works for video and audio files (MP4, WebM, MP3...) and for HLS streams, the kind most sites use, including ones with standard AES-128 encryption; downloads run in parallel pieces and can be paused and resumed. Not possible: copy-protected (DRM) video, YouTube, live streams, and sites that only offer DASH (coming). Streams made of .ts pieces are saved as .ts (plays in VLC or MX Player); MP4 conversion is next. Also fixed: a finished download could be recorded as unfinished after a bad moment of timing."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.2.0-alpha11), then:  bash tools/record-release.sh kestrel-g-v0.2.0-alpha11 "alpha11: Download video. Play a video, then long-press it (or open Menu > Download video) and pick a quality: every row shows its name and size, like '720p - MP4 - 48 MB'. It works for video and audio files (MP4, WebM, MP3...) and for HLS streams, the kind most sites use, including ones with standard AES-128 encryption; downloads run in parallel pieces and can be paused and resumed. Not possible: copy-protected (DRM) video, YouTube, live streams, and sites that only offer DASH (coming). Streams made of .ts pieces are saved as .ts (plays in VLC or MX Player); MP4 conversion is next. Also fixed: a finished download could be recorded as unfinished after a bad moment of timing."
NEVER ship kestrel-g-v0.2.0-alpha11 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  db137ac ckpt 509: V4/V5 P1 VERIFIED on the emulator: HLS streams via the Download video sheet. M
  696b6f0 ckpt 501: V4 progress: P0 (direct files) WORKS on the emulator (sniffer in kestrel-shiel
  307128f ckpt 487: alpha10 SHIPPED and recorded: Release https://github.com/tjshea90/Browser/rele
  50d5718 ckpt 478: alpha10 CI run 43093416759 FAILED in test-jvm: EngineTest.plainDownloadWithout
  b9e656d ckpt 476: alpha10 DISPATCHED on GitHub Actions: run 43093416759 (android.yml, ref ccr-a5
  d69353d ckpt 475: gated kestrel-g-v0.2.0-alpha10 (gecko code 109) and pushed it: handoff self-te
  80e35c0 ckpt 473: V3 verified on the emulator (idle swipe-away = all processes end, active downl
  9b84b98 ckpt 468: Request 8 first batch CODE (compiles, tests+lint green, NOT yet run on the emu
  ded35f9 ckpt 454: Owner request 8 recorded in TASKS.md (V0-V7: background downloads + notificati
  df62246 ckpt 451: alpha9 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha9, gecko code 108, run 430843
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
