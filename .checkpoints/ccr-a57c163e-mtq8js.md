# CHECKPOINT 513 — read me first, then TASKS.md

**Written:** 2026-11-11T21:15:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `6d529ad` (this checkpoint is the commit after it)

## Just done
alpha11 DISPATCHED: run 43101503028 (android.yml, head 6d529ad, 21:15Z). Gate green (331 tests). Also fixed a real engine race found by CI-like flake: a checkpoint snapshot taken before an fsync could overwrite the COMPLETED row with an older VERIFYING one (Engine.saveSnapshot sequence numbers). Docs: ARCHITECTURE sections for background downloads + video downloader; TASKS V4/V5 evidence

## Do this next
1) wait for run 43101503028 (list_workflow_runs / get_workflow_run), then mcp__github__get_release_by_tag kestrel-g-v0.2.0-alpha11, then bash tools/record-release.sh kestrel-g-v0.2.0-alpha11 with the note saved in the scratchpad note11.txt (same text as the dispatch), ckpt, reply to owner with the Release link on its own line 2) meanwhile P2: TS->MP4 conversion (StreamFinisher hook in StreamEngine.finish + Android Remuxer with MediaExtractor/MediaMuxer, joined TS staged via a DEST_STAGING file sink, fallback keep .ts), test on emulator with the ffmpeg TS streams 3) then V7: D1 in-extension blocking engine per research/ADBLOCK_ENGINE.md section 3 and code review of hot paths

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6d529ad ckpt 512: gated kestrel-g-v0.2.0-alpha11 (gecko code 110) and pushed it: handoff self-te
  db137ac ckpt 509: V4/V5 P1 VERIFIED on the emulator: HLS streams via the Download video sheet. M
  696b6f0 ckpt 501: V4 progress: P0 (direct files) WORKS on the emulator (sniffer in kestrel-shiel
  307128f ckpt 487: alpha10 SHIPPED and recorded: Release https://github.com/tjshea90/Browser/rele
  50d5718 ckpt 478: alpha10 CI run 43093416759 FAILED in test-jvm: EngineTest.plainDownloadWithout
  b9e656d ckpt 476: alpha10 DISPATCHED on GitHub Actions: run 43093416759 (android.yml, ref ccr-a5
  d69353d ckpt 475: gated kestrel-g-v0.2.0-alpha10 (gecko code 109) and pushed it: handoff self-te
  80e35c0 ckpt 473: V3 verified on the emulator (idle swipe-away = all processes end, active downl
  9b84b98 ckpt 468: Request 8 first batch CODE (compiles, tests+lint green, NOT yet run on the emu
  ded35f9 ckpt 454: Owner request 8 recorded in TASKS.md (V0-V7: background downloads + notificati
```
