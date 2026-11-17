# CHECKPOINT 2630 — read me first, then TASKS.md

**Written:** 2026-11-17T15:59:18Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `16681653` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha65 (gecko code 166) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-e89b9dfe-pl3lk5, inputs {"app":"gecko","full_build":"true","note":"alpha65: compile at install, the one thing you said go to. Until now an update was only checked by Android when it installed, and its code then ran in the slow interpreter until the tablet was idle and charging (and every update started that over). Now each release also carries a 7 KB start-up profile file. The updater in this version fetches it, checks it, and hands it to Android next to the APK, so Android compiles the update while it installs (in the test phone: 3.7 MB of compiled code from the first start, install 0.4 seconds slower). If Android ever refuses the file the updater installs the update once more without it and stops sending it for two weeks; on other Android versions, for releases without the file, or when storage is tight, the install is exactly what it was. IMPORTANT: this only helps updates that come FROM a version that has this updater, so the update to alpha65 is still compiled the old way and the first one compiled at install is the update after it. Nothing else changed. NOT proven on the tablet: Android Go may not honour the file (then nothing is gained and nothing is lost). If an update ever fails to install, tell me the exact words Android shows."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha65), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha65 "alpha65: compile at install, the one thing you said go to. Until now an update was only checked by Android when it installed, and its code then ran in the slow interpreter until the tablet was idle and charging (and every update started that over). Now each release also carries a 7 KB start-up profile file. The updater in this version fetches it, checks it, and hands it to Android next to the APK, so Android compiles the update while it installs (in the test phone: 3.7 MB of compiled code from the first start, install 0.4 seconds slower). If Android ever refuses the file the updater installs the update once more without it and stops sending it for two weeks; on other Android versions, for releases without the file, or when storage is tight, the install is exactly what it was. IMPORTANT: this only helps updates that come FROM a version that has this updater, so the update to alpha65 is still compiled the old way and the first one compiled at install is the update after it. Nothing else changed. NOT proven on the tablet: Android Go may not honour the file (then nothing is gained and nothing is lost). If an update ever fails to install, tell me the exact words Android shows."
NEVER ship kestrel-g-v0.3.0-alpha65 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a6159180 ckpt 2629: OWNER SAID GO on LAB6 (compile at install through the updater). Recorded in 
  20e28803 ckpt 2628: LAB5 PASS COMPLETE. alpha64 shipped 11:52Z (https://github.com/tjshea90/Brow
  cda703be ckpt 2627: alpha64 SHIPPED + RECORDED (Release kestrel-g-v0.3.0-alpha64, run 4407646379
  e5e2e612 ckpt 2626: alpha64 DISPATCHED ONCE at 11:42Z: android.yml run 44076463792 (app=gecko fu
  d9efb312 ckpt 2625: gated kestrel-g-v0.3.0-alpha64 (gecko code 165) and pushed it: handoff self-
  4692623b ckpt 2624: RELEASE PREP (not shipped): versionCode 165 / versionName 0.3.0-alpha64 in g
  1c468757 ckpt 2623: SWEEP READ (run 44059232336, cancelled after the emulator went offline: 17 g
  bd981349 ckpt 2622: LAB5: OwnerRulesTest.lab5MeasuredPackagingProfileAndPrefsStay (33 tests in t
  c83d9f96 ckpt 2621: LAB5c batch 2 proven by tests: JVM suite 1972 tests / 0 failures (core + app
  41f2a308 ckpt 2620: LAB5a digests written (research/LAB5_PACKAGING, _STARTUP, _EXTENSION, _PREFS
```

(13 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
