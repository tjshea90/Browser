# CHECKPOINT 528 — read me first, then TASKS.md

**Written:** 2026-11-11T21:48:34Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `7589d06` (this checkpoint is the commit after it)

## Just done
TS->MP4 (Remuxer + ADTS->raw AAC frames with an exact AudioClock; verified on the emulator before the refactor: 1724 audio packets, raw frames, decodes clean) ; Adts/AudioClock moved to core with 5 tests; Mp4Probe (picture size/length/codec from the moov, also when it is at the end) + 4 tests + app fetchBytes + VideoFlow probes bare MP4s when the sheet opens; sniffer attributes media to the exact tab page (extension sends top-level URL). Two review agents (page-load and stutter) running in the background; their reports arrive as notifications

## Do this next
1) rebuild debug apk + test on the emulator: remux regression (page hlse.html title 'Kestrel HLS E'), MP4 probe (page probe.html: 3 candidates should show 1080p / 360p / 720p without a player); 2) when review agents report (task-notifications), triage findings: apply cheap high-value fixes (e.g. ShieldsBridge per-block updateUi coalescing, Log.d per block), plan D1 only if cheap; 3) gate + ship alpha12 (versionCode 111, bash tools/sync-ext-version.sh) 4) DASH + merge later

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  bb6a984 ckpt 523: alpha11 SHIPPED and recorded: https://github.com/tjshea90/Browser/releases/tag
  5d55a7b ckpt 513: alpha11 DISPATCHED: run 43101503028 (android.yml, head 6d529ad, 21:15Z). Gate 
  6d529ad ckpt 512: gated kestrel-g-v0.2.0-alpha11 (gecko code 110) and pushed it: handoff self-te
  db137ac ckpt 509: V4/V5 P1 VERIFIED on the emulator: HLS streams via the Download video sheet. M
  696b6f0 ckpt 501: V4 progress: P0 (direct files) WORKS on the emulator (sniffer in kestrel-shiel
  307128f ckpt 487: alpha10 SHIPPED and recorded: Release https://github.com/tjshea90/Browser/rele
  50d5718 ckpt 478: alpha10 CI run 43093416759 FAILED in test-jvm: EngineTest.plainDownloadWithout
  b9e656d ckpt 476: alpha10 DISPATCHED on GitHub Actions: run 43093416759 (android.yml, ref ccr-a5
  d69353d ckpt 475: gated kestrel-g-v0.2.0-alpha10 (gecko code 109) and pushed it: handoff self-te
  80e35c0 ckpt 473: V3 verified on the emulator (idle swipe-away = all processes end, active downl
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
