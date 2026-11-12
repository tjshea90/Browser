# CHECKPOINT 1247 — read me first, then TASKS.md

**Written:** 2026-11-12T22:36:50Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `6b43626` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha24 (gecko code 123) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha24: Back up & restore, much deeper ad hiding, and a keyboard fix. Settings > Back up & restore: one tap saves your bookmarks, history, settings, per-site Shields choices and open tabs in one file in Downloads (it survives uninstalling Kestrel), and Restore brings them back from the file you pick. Ad hiding: all 29,204 generic hiding rules of the lists now work (only the first 1,200 did before), each one applied only on pages that really have a matching class or id, so pages are not slowed; two bugs in how the hiding rules were written, which would have stopped some from working, are fixed. Tapping a text field in the lower half of the screen now scrolls it above the keyboard instead of leaving it hidden until you type."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha24), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha24 "alpha24: Back up & restore, much deeper ad hiding, and a keyboard fix. Settings > Back up & restore: one tap saves your bookmarks, history, settings, per-site Shields choices and open tabs in one file in Downloads (it survives uninstalling Kestrel), and Restore brings them back from the file you pick. Ad hiding: all 29,204 generic hiding rules of the lists now work (only the first 1,200 did before), each one applied only on pages that really have a matching class or id, so pages are not slowed; two bugs in how the hiding rules were written, which would have stopped some from working, are fixed. Tapping a text field in the lower half of the screen now scrolls it above the keyboard instead of leaving it hidden until you type."
NEVER ship kestrel-g-v0.3.0-alpha24 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6b43626 ckpt 1246: alpha24 content finished and tested: keyboard-reveal fix (content.js), D2 laz
  2e3e341 ckpt 1238: AA3-7 Back up/Restore, part 1 done and tested: core BackupCodec (strict untru
  b630671 ckpt 1227: AA3-3 D2 DONE and verified: all ~29,204 generic cosmetic rules now apply (was
  15b71c1 ckpt 1196: AA3-2 typing/keyboard: closed the 'keyboard resizes the page' hypothesis on G
  81f5a25 ckpt 1187: SHIPPED + RECORDED: Kestrel G 0.3.0-alpha23 (code 122), tag kestrel-g-v0.3.0-
  05062dd ckpt 1185: alpha23 CI run 43305054932 failed in test-jvm: BrowserFlowTest.newTabPageWarn
  65bbb1c ckpt 1183: gated kestrel-g-v0.3.0-alpha23 (gecko code 122) and pushed it: handoff self-t
  8e8c9bc ckpt 1176: AB3 code done + unit-tested: NetProfile (core) now has isFastTunnel (10.111.2
  3a65bfe ckpt 1151: AB2 verified on the Android 10 emulator: long press opens the new Save video 
  d9db0b5 ckpt 1139: AB1 done: ONE master switch for the phone helper: PhoneBridge.setMaster/maste
```
