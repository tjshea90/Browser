# CHECKPOINT 1606 — read me first, then TASKS.md

**Written:** 2026-11-13T18:24:20Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `3336bd5` (this checkpoint is the commit after it)

## Just done
alpha31 shipped to GitHub: ship.sh gecko gates green + pushed, android.yml dispatched (app=gecko, full_build=true, ref ccr-a57c163e-mtq8js). Contains AJ1d (updater maths), AK2 (Video lab + 5.3 MiB clips removed), AK3 (tab switcher shows Home), AK4 (player headers, maybe-media by first bytes, picture-headed parts, honest reasons). Tests: core, app, geckoapp compile, lint, web 125/125 all green; emulator lab proof of AK3 and AK4 (saved MP4 ffprobe h264+aac 12.0 s)

## Do this next
WAIT for the android.yml run: poll mcp__github__actions_list list_workflow_runs (workflow android.yml, branch ccr-a57c163e-mtq8js) until completed+success AND git ls-remote origin refs/tags/kestrel-g-v0.3.0-alpha31 shows the tag; then mcp__github__get_release_by_tag (owner tjshea90 repo browser) to see the APK, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha31 with the note from scratchpad note31.txt (alpha31 ... 4 points), tick AK4d with the run id via bash tools/claim.sh AK4 --done, then answer the owner in plain words (Release link on its own line, what to try on doraby.com: open the movie, press play, tap the download icon; if still nothing: Menu>Diagnostics>Copy and paste the lines with 'media'), re-list open items. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN still on: stand down on its wakes. Never ship the tag twice.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  3336bd5 ckpt 1605: gated kestrel-g-v0.3.0-alpha31 (gecko code 130) and pushed it: handoff self-t
  e4be816 ckpt 1596: AK4 steps 1-3 done in code + tests: (1) extension background.js captures the 
  a32efe5 ckpt 1582: AK1 answered in chat (appops REQUEST_INSTALL_PACKAGES/RUN_IN_BACKGROUND/RUN_A
  24b86ca ckpt 1570: AJ1 DONE and answered in chat: adb question (owner msgs 23 + newer msg 24 'wi
  47dbd77 ckpt 1568: AJ1d code done + green: core UpdateLogic.lowReserve + new storagePlan (reserv
  db37d60 ckpt 1555: AJ1a DONE on the Android 10 emulator + Android 10 source: install reserve = m
  18ae1e8 ckpt 1550: owner message 23 (question: can the browser benefit from adb now that they ha
  9a8cb75 ckpt 1545: alpha30 SHIPPED and RECORDED (Release kestrel-g-v0.3.0-alpha30, run 434028420
  c43cfff ckpt 1541: alpha30 RE-DISPATCHED on the test-fix commit 59a5f37: android.yml run 4340284
  59a5f37 ckpt 1540: alpha30 CI run 43399774749 FAILED in test-js (not the app build): web-tests/b
```
