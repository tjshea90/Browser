# CHECKPOINT 523 — read me first, then TASKS.md

**Written:** 2026-11-11T21:38:50Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `776e53c` (this checkpoint is the commit after it)

## Just done
alpha11 SHIPPED and recorded: https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.2.0-alpha11 (run 43101503028 green, APK 92.66 MB, BUILDLOG recorded). In progress for alpha12: TS->MP4 conversion (core StreamFinisher hook + tests done; app/download/Remuxer.kt works on the emulator for video, AAC arrives from Android's TS extractor as ADTS-bundled samples: now split into raw frames with an exact audio clock; previous attempt failed 'writeSampleData returned an error' because audio times stepped back), sniffer now attributes media to the tab with the exact page URL (extension sends the top-level address per tab). Debug logging callback in StreamEngine

## Do this next
1) rebuild debug apk, retest TS->MP4 on emulator (page hlsc.html title 'Kestrel HLS C' is taken: use a NEW page title per test because deleting Download files with adb rm leaves stale MediaStore rows that make inserts fail), verify with ffprobe: audio packets ~1724 raw frames (first bytes not ff f1), decode ok 2) if audio is right: gate + keep in alpha12; if not, disable the finisher (pass null in DownloadController) and ship .ts only 3) then DASH parser (P3) or V7 speed work: D1 in-extension blocking per research/ADBLOCK_ENGINE.md section 3, ship:ship-code-reviewer agents on hot paths 4) ship alpha12 (versionCode 111 / 0.2.0-alpha12, bash tools/sync-ext-version.sh)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5d55a7b ckpt 513: alpha11 DISPATCHED: run 43101503028 (android.yml, head 6d529ad, 21:15Z). Gate 
  6d529ad ckpt 512: gated kestrel-g-v0.2.0-alpha11 (gecko code 110) and pushed it: handoff self-te
  db137ac ckpt 509: V4/V5 P1 VERIFIED on the emulator: HLS streams via the Download video sheet. M
  696b6f0 ckpt 501: V4 progress: P0 (direct files) WORKS on the emulator (sniffer in kestrel-shiel
  307128f ckpt 487: alpha10 SHIPPED and recorded: Release https://github.com/tjshea90/Browser/rele
  50d5718 ckpt 478: alpha10 CI run 43093416759 FAILED in test-jvm: EngineTest.plainDownloadWithout
  b9e656d ckpt 476: alpha10 DISPATCHED on GitHub Actions: run 43093416759 (android.yml, ref ccr-a5
  d69353d ckpt 475: gated kestrel-g-v0.2.0-alpha10 (gecko code 109) and pushed it: handoff self-te
  80e35c0 ckpt 473: V3 verified on the emulator (idle swipe-away = all processes end, active downl
  9b84b98 ckpt 468: Request 8 first batch CODE (compiles, tests+lint green, NOT yet run on the emu
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
