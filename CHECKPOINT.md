# CHECKPOINT 1605 — read me first, then TASKS.md

**Written:** 2026-11-13T18:23:30Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `71159a7` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha31 (gecko code 130) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha31 (includes alpha30): 1) The tab list now shows a tab that is on the Home screen as "Home", with a picture of the Home screen, instead of the last web site it was on. 2) The Video lab and its four test clips are gone (5.3 MB smaller). 3) Saving videos from a player that sits inside another site (the "servers" on movie sites): a video server often refuses anyone who is not its own player, and Kestrel used to introduce itself with the movie page's address. It now repeats the player's own headers, tries other combinations before giving up, finds playlists a site hides under names like .txt, handles stream parts disguised as pictures, and says why when a site refuses (with the HTTP number). Still not possible: players that keep the stream out of the network, copy-protected (DRM) video and live streams. Checked here on a test site built to behave like that, not on doraby.com itself. 4) The update-space numbers are fixed: Android keeps only about 0.5 GB free after an install (5 % of the storage, at most 500 MB), so an update needs about 1 GB free on this tablet, not 3 GB."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha31), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha31 "alpha31 (includes alpha30): 1) The tab list now shows a tab that is on the Home screen as "Home", with a picture of the Home screen, instead of the last web site it was on. 2) The Video lab and its four test clips are gone (5.3 MB smaller). 3) Saving videos from a player that sits inside another site (the "servers" on movie sites): a video server often refuses anyone who is not its own player, and Kestrel used to introduce itself with the movie page's address. It now repeats the player's own headers, tries other combinations before giving up, finds playlists a site hides under names like .txt, handles stream parts disguised as pictures, and says why when a site refuses (with the HTTP number). Still not possible: players that keep the stream out of the network, copy-protected (DRM) video and live streams. Checked here on a test site built to behave like that, not on doraby.com itself. 4) The update-space numbers are fixed: Android keeps only about 0.5 GB free after an install (5 % of the storage, at most 500 MB), so an update needs about 1 GB free on this tablet, not 3 GB."
NEVER ship kestrel-g-v0.3.0-alpha31 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  e4be816 ckpt 1596: AK4 steps 1-3 done in code + tests: (1) extension background.js captures the 
  a32efe5 ckpt 1582: AK1 answered in chat (appops REQUEST_INSTALL_PACKAGES/RUN_IN_BACKGROUND/RUN_A
  24b86ca ckpt 1570: AJ1 DONE and answered in chat: adb question (owner msgs 23 + newer msg 24 'wi
  47dbd77 ckpt 1568: AJ1d code done + green: core UpdateLogic.lowReserve + new storagePlan (reserv
  db37d60 ckpt 1555: AJ1a DONE on the Android 10 emulator + Android 10 source: install reserve = m
  18ae1e8 ckpt 1550: owner message 23 (question: can the browser benefit from adb now that they ha
  9a8cb75 ckpt 1545: alpha30 SHIPPED and RECORDED (Release kestrel-g-v0.3.0-alpha30, run 434028420
  c43cfff ckpt 1541: alpha30 RE-DISPATCHED on the test-fix commit 59a5f37: android.yml run 4340284
  59a5f37 ckpt 1540: alpha30 CI run 43399774749 FAILED in test-js (not the app build): web-tests/b
  ea9b9f4 ckpt 1536: alpha30 DISPATCHED: android.yml run 43399774749 (run_number 80) queued 09:15:
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
