# CHECKPOINT 501 — read me first, then TASKS.md

**Written:** 2026-11-11T20:33:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `c46d4ff` (this checkpoint is the commit after it)

## Just done
V4 progress: P0 (direct files) WORKS on the emulator (sniffer in kestrel-shields extension -> ShieldsBridge -> Tab.media -> menu tile 'Download video' + long-press menu + VideoPanel sheet with '720p MP4 1.8 MB 0:12' rows; tap = ordinary download with fixed name 'Page title - 720p.mp4'; engine got Source.nameFixed + DB v2 migration). P1 core written + tested: core/media/Hls.kt (master+media parser, AES-128, byte ranges, MAP, live/DRM detection; 10 tests), core/media/StreamEngine.kt (segment jobs: parallel fetch, ordered append, AES-128, pause/resume/kill-resume, retries, windows for Gecko fetcher; 14 tests), MediaSniff/MediaList (17 tests). versionCode 110 / 0.2.0-alpha11 in build.gradle.kts, ext manifest 1.0.110

## Do this next
1) Android: app/download/StreamStoreFiles.kt (StreamStore on files/streams/<id>.json + <id>.m3u8, staging dir cache/streams/<id>) 2) DownloadController: second engine StreamEngine(idBase 1e9) merged into views()/view()/counts()/needsService()/pause/resume/cancel/restart/remove/clearFinished/pauseAll/resumeAll/onNetwork/addListener/uiChanged notifications; DownloadService keep-alive uses merged needsService 3) VideoFlow: HLS candidate -> fetch master playlist (GeckoFetcher kind 1 via DownloadController.fetchText) -> rows per variant ('1080p - about 210 MB - 12:34') -> pick -> StreamSource; media playlist directly -> single row 4) test on emulator with ffmpeg HLS (ts + aes + fmp4) served by scratchpad mediaserver.py on 8002 and hls.js (npm pack hls.js) page; 5) then DASH/muxing decision, speed review V7, ship alpha11 (record-release after green)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  307128f ckpt 487: alpha10 SHIPPED and recorded: Release https://github.com/tjshea90/Browser/rele
  50d5718 ckpt 478: alpha10 CI run 43093416759 FAILED in test-jvm: EngineTest.plainDownloadWithout
  b9e656d ckpt 476: alpha10 DISPATCHED on GitHub Actions: run 43093416759 (android.yml, ref ccr-a5
  d69353d ckpt 475: gated kestrel-g-v0.2.0-alpha10 (gecko code 109) and pushed it: handoff self-te
  80e35c0 ckpt 473: V3 verified on the emulator (idle swipe-away = all processes end, active downl
  9b84b98 ckpt 468: Request 8 first batch CODE (compiles, tests+lint green, NOT yet run on the emu
  ded35f9 ckpt 454: Owner request 8 recorded in TASKS.md (V0-V7: background downloads + notificati
  df62246 ckpt 451: alpha9 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha9, gecko code 108, run 430843
  8ff3900 ckpt 448: alpha9 DISPATCHED: Actions run 43084348983 (gecko code 108, 19:08Z). alpha8 (c
  3b0bd85 ckpt 447: gated kestrel-g-v0.2.0-alpha9 (gecko code 108) and pushed it: handoff self-tes
```

(13 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
