# CHECKPOINT 509 — read me first, then TASKS.md

**Written:** 2026-11-11T21:10:55Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `594840b` (this checkpoint is the commit after it)

## Just done
V4/V5 P1 VERIFIED on the emulator: HLS streams via the Download video sheet. Master playlist -> '720p HLS about 6.2 MB 0:40' / '360p HLS about 2.0 MB' rows (variant playlists hidden); TS stream byte-identical to the concatenated segments incl. pause (no traffic while paused, 'Paused 2.5 MB of about 3.2 MB'), resume, and a server restart mid-download; AES-128 stream byte-identical to openssl-decrypted segments; fMP4 (init + m4s) saved as .mp4 byte-identical, ffprobe h264 1280x720 + aac 40 s. Fixes: sniffer de-dup is now per tab+page (it was global: a reload or second tab never reported the same URL again), stagedCount double count made the size estimate half the real size (test added). New: StreamStoreFiles, DownloadController merges StreamEngine (ids >= 1e9), VideoFlow HLS rows, Source.nameFixed

## Do this next
1) ship alpha11 now? (needs: gate :core:test :app:testDebugUnitTest :app:compileDebugKotlin :geckoapp:compileDebugKotlin :app:lintDebug; docs ARCHITECTURE/DESIGN notes for video downloader; TASKS V4/V5 evidence; bash ship.sh gecko note; dispatch android.yml; record-release) 2) long-press on a <video> element test on the emulator (TYPE_VIDEO menu) 3) P2 TS->MP4 remux (Android Remuxer + StreamFinisher hook) then DASH 4) V7 speed review

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M core/src/test/kotlin/app/kestrel/core/media/StreamEngineTest.kt

## Last ten checkpoints
```
  696b6f0 ckpt 501: V4 progress: P0 (direct files) WORKS on the emulator (sniffer in kestrel-shiel
  307128f ckpt 487: alpha10 SHIPPED and recorded: Release https://github.com/tjshea90/Browser/rele
  50d5718 ckpt 478: alpha10 CI run 43093416759 FAILED in test-jvm: EngineTest.plainDownloadWithout
  b9e656d ckpt 476: alpha10 DISPATCHED on GitHub Actions: run 43093416759 (android.yml, ref ccr-a5
  d69353d ckpt 475: gated kestrel-g-v0.2.0-alpha10 (gecko code 109) and pushed it: handoff self-te
  80e35c0 ckpt 473: V3 verified on the emulator (idle swipe-away = all processes end, active downl
  9b84b98 ckpt 468: Request 8 first batch CODE (compiles, tests+lint green, NOT yet run on the emu
  ded35f9 ckpt 454: Owner request 8 recorded in TASKS.md (V0-V7: background downloads + notificati
  df62246 ckpt 451: alpha9 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha9, gecko code 108, run 430843
  8ff3900 ckpt 448: alpha9 DISPATCHED: Actions run 43084348983 (gecko code 108, 19:08Z). alpha8 (c
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
