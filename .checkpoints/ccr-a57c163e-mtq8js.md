# CHECKPOINT 543 — read me first, then TASKS.md

**Written:** 2026-11-11T22:07:00Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `b69c135` (this checkpoint is the commit after it)

## Just done
DASH + merge DONE and emulator-verified: core/media/Dash.kt (MPD parser: SegmentTemplate number/timeline, SegmentList, single file; live/DRM/multi-period flagged; 8 tests incl. a real ffmpeg MPD), StreamEngine repId/ext + companion jobs (picture + hidden sound job, family pause/resume/cancel/remove, aggregated row, failure cascade, merge via StreamFinisher.merge; 24 tests), Android Remuxer.merge (two MediaExtractors -> MediaMuxer, time-ordered), VideoFlow DASH rows ('720p DASH about 6.0 MB 0:40' + 'Audio only'), HLS with separate audio now also merges. Emulator: ffmpeg DASH stream -> one MP4 h264 1280x720 + aac 1724 raw frames, 40.03 s, decodes clean. Review agent #1 (stutter) report received: top items = download notification/UI refresh storm (5x/s, 5-6 Binder calls each, allViews() 2-3x per tick), KestrelGeckoView per-touch onTouchEventForDetailResult, tab grid rebuilt on every tab event, suggestions not debounced, keyboard probe IPC every 100 ms. Page-load review (agent a8108ab8a54aee18d) still running

## Do this next
1) apply the stutter fixes: Notifier/DownloadService (build intents once, post only when changed and at most ~1/s, one snapshot per tick, renew wake lock rarely), KestrelGeckoView touch (super.onTouchEvent except DOWN), tabGrid.rebind coalescing, suggestions debounce 70 ms, PullChip INVISIBLE, progress quantize 2%; 2) wait for the page-load review then triage (per-request ShieldsBridge cost, D1 decision); 3) rebuild debug apk and emulator-check downloads screen + notifications still fine; 4) gate, alpha12 (versionCode 111 + bash tools/sync-ext-version.sh), ship, record, reply

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5220430 ckpt 528: TS->MP4 (Remuxer + ADTS->raw AAC frames with an exact AudioClock; verified on 
  bb6a984 ckpt 523: alpha11 SHIPPED and recorded: https://github.com/tjshea90/Browser/releases/tag
  5d55a7b ckpt 513: alpha11 DISPATCHED: run 43101503028 (android.yml, head 6d529ad, 21:15Z). Gate 
  6d529ad ckpt 512: gated kestrel-g-v0.2.0-alpha11 (gecko code 110) and pushed it: handoff self-te
  db137ac ckpt 509: V4/V5 P1 VERIFIED on the emulator: HLS streams via the Download video sheet. M
  696b6f0 ckpt 501: V4 progress: P0 (direct files) WORKS on the emulator (sniffer in kestrel-shiel
  307128f ckpt 487: alpha10 SHIPPED and recorded: Release https://github.com/tjshea90/Browser/rele
  50d5718 ckpt 478: alpha10 CI run 43093416759 FAILED in test-jvm: EngineTest.plainDownloadWithout
  b9e656d ckpt 476: alpha10 DISPATCHED on GitHub Actions: run 43093416759 (android.yml, ref ccr-a5
  d69353d ckpt 475: gated kestrel-g-v0.2.0-alpha10 (gecko code 109) and pushed it: handoff self-te
```

(14 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
