# CHECKPOINT 1634 — read me first, then TASKS.md

**Written:** 2026-11-13T19:24:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `1a37ea1` (this checkpoint is the commit after it)

## Just done
AL3 coded: Format.barText/belowBar (core, PlannerTest green), new ui/DownloadBar.kt (30 dp, transparent track, neon fill, centred text drawn twice split at the fill edge), DownloadsScreen uses it; both apps compile; NOT yet seen on the emulator

## Do this next
AL2: write app/src/main/kotlin/app/kestrel/browser/ui/DownloadPopup.kt (top banner 'Download started' + top-right mini window with DownloadBar compact, pause/resume, x cancel+delete, close x) and wire into geckoapp BrowserActivity (startDownload + VideoFlow.pickStream replace the bottom toast); then AL6 names (core/media/MediaNames.kt), then ONE emulator session proving AL1/AL2/AL3/AL5 (emulator is NOT running: start it, build -PgeckoAbi=x86_64), then ship alpha32 (versionCode 131)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e688a4c ckpt 1629: AL4 done (cancel deletes partial file on every path; Clear/restore leaks clos
  f43eec4 ckpt 1619: owner message 26 ('The download works now' = alpha31 saved the vidsrc movie) 
  cdab618 ckpt 1609: alpha31 DONE and recorded (BUILDLOG line, run 43482256190 green, APK 93,443,1
  064ab78 ckpt 1606: alpha31 shipped to GitHub: ship.sh gecko gates green + pushed, android.yml di
  3336bd5 ckpt 1605: gated kestrel-g-v0.3.0-alpha31 (gecko code 130) and pushed it: handoff self-t
  e4be816 ckpt 1596: AK4 steps 1-3 done in code + tests: (1) extension background.js captures the 
  a32efe5 ckpt 1582: AK1 answered in chat (appops REQUEST_INSTALL_PACKAGES/RUN_IN_BACKGROUND/RUN_A
  24b86ca ckpt 1570: AJ1 DONE and answered in chat: adb question (owner msgs 23 + newer msg 24 'wi
  47dbd77 ckpt 1568: AJ1d code done + green: core UpdateLogic.lowReserve + new storagePlan (reserv
  db37d60 ckpt 1555: AJ1a DONE on the Android 10 emulator + Android 10 source: install reserve = m
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
