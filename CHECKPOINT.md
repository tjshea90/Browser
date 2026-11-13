# CHECKPOINT 1582 — read me first, then TASKS.md

**Written:** 2026-11-13T17:52:29Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `e5ecd1a` (this checkpoint is the commit after it)

## Just done
AK1 answered in chat (appops REQUEST_INSTALL_PACKAGES/RUN_IN_BACKGROUND/RUN_ANY_IN_BACKGROUND allow, deviceidle whitelist, optional pm grant camera/mic/location; POST_NOTIFICATIONS errors on Android 10; all tested on the emulator). AK2 code done: Video lab removed (5.3 MiB clips + video.html + button + Settings line + web test + docs note). AK3 code done: Tab.cardTitle/homeOverPage, TabGrid/TabSwitcher show Home for a tab with the Home overlay, ThumbView Home mode, BrowserActivity.homeSnapshot -> tabGrid.homePreview; TabGridHomeTest 3 green; :geckoapp:compileDebugKotlin ok

## Do this next
AK4: start with AK4a: read how Kestrel finds videos today (geckoapp assets kestrel-shields background.js/content.js sniffer, geckoapp web/VideoFlow.kt, MediaList, download engine in app download/*) and how the engine fetches HLS/DASH (headers Referer/Origin/Cookie, keys); write research/VIDEO_SOURCES.md with why a vidsrc-style embed fails; then implement generic improvements + tests; emulator proof of AK3 (Home card) + AK2 (APK smaller) still to do on the x86_64 debug build (gradlew :geckoapp:assembleDebug -PgeckoAbi=x86_64); then ship alpha31 (bump versionCode 130, ship.sh gecko, web-tests first) with AJ1d+AK2+AK3+AK4

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  24b86ca ckpt 1570: AJ1 DONE and answered in chat: adb question (owner msgs 23 + newer msg 24 'wi
  47dbd77 ckpt 1568: AJ1d code done + green: core UpdateLogic.lowReserve + new storagePlan (reserv
  db37d60 ckpt 1555: AJ1a DONE on the Android 10 emulator + Android 10 source: install reserve = m
  18ae1e8 ckpt 1550: owner message 23 (question: can the browser benefit from adb now that they ha
  9a8cb75 ckpt 1545: alpha30 SHIPPED and RECORDED (Release kestrel-g-v0.3.0-alpha30, run 434028420
  c43cfff ckpt 1541: alpha30 RE-DISPATCHED on the test-fix commit 59a5f37: android.yml run 4340284
  59a5f37 ckpt 1540: alpha30 CI run 43399774749 FAILED in test-js (not the app build): web-tests/b
  ea9b9f4 ckpt 1536: alpha30 DISPATCHED: android.yml run 43399774749 (run_number 80) queued 09:15:
  ab7b158 ckpt 1535: gated kestrel-g-v0.3.0-alpha30 (gecko code 129) and pushed it: handoff self-t
  ac7766e ckpt 1534: pre-ship: alpha30 (includes alpha29 and alpha28): your three requests. 1) Set
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
