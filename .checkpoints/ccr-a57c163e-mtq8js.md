# CHECKPOINT 487 — read me first, then TASKS.md

**Written:** 2026-11-11T20:16:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `e8b347f` (this checkpoint is the commit after it)

## Just done
alpha10 SHIPPED and recorded: Release https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.2.0-alpha10 (run 43094237718 green, APK 92.6 MB, BUILDLOG line committed e8b347f). V4 P0 groundwork written (not yet compiled together): core/media/MediaSniff.kt + MediaList.kt (+17 tests green), extension background.js onHeadersReceived sniffer + content.js <video> info relay, ShieldsBridge handles 'media'/'vid' messages (mediaSeen/mediaInfo callbacks), Tab.media, ui/VideoPanel.kt

## Do this next
V4 P0 wiring in geckoapp: (1) BrowserActivity: pass mediaSeen/mediaInfo to ShieldsBridge (line ~195: attach to tabForPage(page) ?: current, Io.ui), clear tab.media in TabDelegates.onPageStart and in navigateTo; (2) new geckoapp web/VideoFlow.kt: sheet with VideoPanel rows from tab.media.usable() (title = quality or 'Video', detail = 'MP4 - 48.2 MB - 1:02'), start = DownloadController.enqueue(Source(url, pageUrl, name 'Page title - 720p.mp4', mime, headers Referer origin, fetcher=1, priv, meteredOk=true)); (3) longPress: TYPE_VIDEO/TYPE_AUDIO -> menu 'Download video'; menu tile M.VIDEO=18 with count; (4) compile (:geckoapp:compileDebugKotlin), build x86_64 debug apk, test on emulator with ffmpeg-made mp4s served by scratchpad rangeserver (page with <video> + <source label>), (5) then P1 HLS: core/media/Hls.kt parser+tests, StreamEngine (segment jobs, parallel, resume, AES-128), merge views in DownloadController; bump versionCode 110 + sync-ext-version for alpha11

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  50d5718 ckpt 478: alpha10 CI run 43093416759 FAILED in test-jvm: EngineTest.plainDownloadWithout
  b9e656d ckpt 476: alpha10 DISPATCHED on GitHub Actions: run 43093416759 (android.yml, ref ccr-a5
  d69353d ckpt 475: gated kestrel-g-v0.2.0-alpha10 (gecko code 109) and pushed it: handoff self-te
  80e35c0 ckpt 473: V3 verified on the emulator (idle swipe-away = all processes end, active downl
  9b84b98 ckpt 468: Request 8 first batch CODE (compiles, tests+lint green, NOT yet run on the emu
  ded35f9 ckpt 454: Owner request 8 recorded in TASKS.md (V0-V7: background downloads + notificati
  df62246 ckpt 451: alpha9 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha9, gecko code 108, run 430843
  8ff3900 ckpt 448: alpha9 DISPATCHED: Actions run 43084348983 (gecko code 108, 19:08Z). alpha8 (c
  3b0bd85 ckpt 447: gated kestrel-g-v0.2.0-alpha9 (gecko code 108) and pushed it: handoff self-tes
  30794d6 ckpt 446: pre-ship: alpha9: the update screen's storage numbers now match what Android r
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
