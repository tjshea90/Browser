# CHECKPOINT 1648 — read me first, then TASKS.md

**Written:** 2026-11-13T19:34:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `264ea44` (this checkpoint is the commit after it)

## Just done
AL2 coded (core/download/MiniTracker + 9 tests, ui/DownloadPopup banner + top-right mini window wired into geckoapp BrowserActivity, toasts removed), AL6 coded (core/media/EpisodeNames + MediaNames.defaultStem/streamStem/friendlyFileName, 21 tests; wired in VideoFlow + startDownload); AL3 coded. Emulator proof NOT done yet

## Do this next
1) wait for build_al.log BUILD-EXIT 0 in the scratchpad (x86_64 debug APK at geckoapp/build/outputs/apk/debug/geckoapp-debug.apk), 2) emulator is BOOTED (kestrel29x64): adb install -r -d, start a slow file server on the host (python, 10.0.2.2:8000) and screenshot: banner, mini window, taller bar with text; 3) extend tools/sitelab vidlab for AL1/AL5 (second server iframe, page change) 4) full test suite + web tests, then ship alpha32 (versionCode 131)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f3ad3c6 ckpt 1634: AL3 coded: Format.barText/belowBar (core, PlannerTest green), new ui/Download
  e688a4c ckpt 1629: AL4 done (cancel deletes partial file on every path; Clear/restore leaks clos
  f43eec4 ckpt 1619: owner message 26 ('The download works now' = alpha31 saved the vidsrc movie) 
  cdab618 ckpt 1609: alpha31 DONE and recorded (BUILDLOG line, run 43482256190 green, APK 93,443,1
  064ab78 ckpt 1606: alpha31 shipped to GitHub: ship.sh gecko gates green + pushed, android.yml di
  3336bd5 ckpt 1605: gated kestrel-g-v0.3.0-alpha31 (gecko code 130) and pushed it: handoff self-t
  e4be816 ckpt 1596: AK4 steps 1-3 done in code + tests: (1) extension background.js captures the 
  a32efe5 ckpt 1582: AK1 answered in chat (appops REQUEST_INSTALL_PACKAGES/RUN_IN_BACKGROUND/RUN_A
  24b86ca ckpt 1570: AJ1 DONE and answered in chat: adb question (owner msgs 23 + newer msg 24 'wi
  47dbd77 ckpt 1568: AJ1d code done + green: core UpdateLogic.lowReserve + new storagePlan (reserv
```

(13 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
