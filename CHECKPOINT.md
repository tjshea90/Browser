# CHECKPOINT 869 — read me first, then TASKS.md

**Written:** 2026-11-12T05:58:40Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `9932748` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha18 (gecko code 117) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha18: the updater now always tries to install (it no longer stops on a free-storage estimate; Android is the only judge), and when Android does refuse it explains why and offers Try again, Storage settings or Install the other way (saves the update to Downloads and opens Android's uninstall prompt). New Make MP4 button on finished .ts downloads. Ad inserts nobody marked in a stream are spotted and left out. The download icon is found when the same page is open in two tabs."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha18), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha18 "alpha18: the updater now always tries to install (it no longer stops on a free-storage estimate; Android is the only judge), and when Android does refuse it explains why and offers Try again, Storage settings or Install the other way (saves the update to Downloads and opens Android's uninstall prompt). New Make MP4 button on finished .ts downloads. Ad inserts nobody marked in a stream are spotted and left out. The download icon is found when the same page is open in two tabs."
NEVER ship kestrel-g-v0.3.0-alpha18 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  9932748 ckpt 868: pre-ship: alpha18: the updater now always tries to install (it no longer stops
  879b10f ckpt 867: Y1 DONE (updater always tries: no storage gate in download()/install(), floor2
  ceaa63c ckpt 860: owner message 12 written into TASKS (Y1 updater must always try, Y2 phone offl
  eadd5fc ckpt 856: post-alpha17 in the tree (NOT shipped): Make MP4 for finished .ts rows (Stream
  0948a0d ckpt 843: post-dispatch hardening (NOT in alpha17): TsToMp4.peekFormatChange (unmarked s
  376dc8d ckpt 837: alpha17 DISPATCHED: android.yml run 43157262742 (run_number 60) on ccr-a57c163
  2726199 ckpt 836: gated kestrel-g-v0.3.0-alpha17 (gecko code 116) and pushed it: handoff self-te
  7f4771d ckpt 835: pre-ship: alpha17: the download button is now an icon in the bottom bar (gray 
  791bba1 ckpt 833: X15 + X18 DONE and emulator-proven (alpha-debug x86_64, Android 10): dock popu
  4f4235e ckpt 826: X18b DONE: StreamEngine writes TS HLS straight into an MP4 (decideMux probes s
```
