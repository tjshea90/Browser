# CHECKPOINT 1619 — read me first, then TASKS.md

**Written:** 2026-11-13T19:07:30Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `1275518` (this checkpoint is the commit after it)

## Just done
owner message 26 ('The download works now' = alpha31 saved the vidsrc movie) written into TASKS as AL1-AL6 and claimed: (1) video list only for the current page, (2) download-started pop up + mini progress window top right with pause/resume/x/cancel, (3) taller full-screen manager bars with centred contrasting text inside, (4) cancel deletes the partial file, (5) server switch refreshes the video options without false triggers from ads, (6) intuitive default names (Show S02E05)

## Do this next
start with AL1: TabDelegates.kt:161 and BrowserActivity.kt:902 clear tab.media; find why old candidates stay (VideoFlow.seen falls back to a.current for any page; late messages from the old page) -> drop stale candidates, clear on main-frame change; then AL5 frame-based refresh (sub_frame navigation of a frame that owns candidates, NOT a new candidate in the same document); then AL4 (cancel deletes partial: check engine, StreamEngine, phone downloads), AL3 (DownloadsScreen), AL2 (new mini window), AL6 (names), then ship alpha32 (bump versionCode 131, web-tests first)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  cdab618 ckpt 1609: alpha31 DONE and recorded (BUILDLOG line, run 43482256190 green, APK 93,443,1
  064ab78 ckpt 1606: alpha31 shipped to GitHub: ship.sh gecko gates green + pushed, android.yml di
  3336bd5 ckpt 1605: gated kestrel-g-v0.3.0-alpha31 (gecko code 130) and pushed it: handoff self-t
  e4be816 ckpt 1596: AK4 steps 1-3 done in code + tests: (1) extension background.js captures the 
  a32efe5 ckpt 1582: AK1 answered in chat (appops REQUEST_INSTALL_PACKAGES/RUN_IN_BACKGROUND/RUN_A
  24b86ca ckpt 1570: AJ1 DONE and answered in chat: adb question (owner msgs 23 + newer msg 24 'wi
  47dbd77 ckpt 1568: AJ1d code done + green: core UpdateLogic.lowReserve + new storagePlan (reserv
  db37d60 ckpt 1555: AJ1a DONE on the Android 10 emulator + Android 10 source: install reserve = m
  18ae1e8 ckpt 1550: owner message 23 (question: can the browser benefit from adb now that they ha
  9a8cb75 ckpt 1545: alpha30 SHIPPED and RECORDED (Release kestrel-g-v0.3.0-alpha30, run 434028420
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
