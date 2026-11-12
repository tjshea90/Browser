# CHECKPOINT 836 — read me first, then TASKS.md

**Written:** 2026-11-12T04:49:03Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `7f4771d` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha17 (gecko code 116) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha17: the download button is now an icon in the bottom bar (gray when the page has no video, pulsing neon blue when it has one; tap it for a list with names, sizes, resolutions and types), long-press on a video opens Kestrel's own Download menu instead of the site's player menu, fake play buttons that open spam pages are blocked, video streams (HLS) are written straight into an .mp4 while they download (no .ts file, no wait at the end, picks up after a kill, says why when it cannot), Settings > Downloads (speed limit, downloads at once, connections)"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha17), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha17 "alpha17: the download button is now an icon in the bottom bar (gray when the page has no video, pulsing neon blue when it has one; tap it for a list with names, sizes, resolutions and types), long-press on a video opens Kestrel's own Download menu instead of the site's player menu, fake play buttons that open spam pages are blocked, video streams (HLS) are written straight into an .mp4 while they download (no .ts file, no wait at the end, picks up after a kill, says why when it cannot), Settings > Downloads (speed limit, downloads at once, connections)"
NEVER ship kestrel-g-v0.3.0-alpha17 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  7f4771d ckpt 835: pre-ship: alpha17: the download button is now an icon in the bottom bar (gray 
  791bba1 ckpt 833: X15 + X18 DONE and emulator-proven (alpha-debug x86_64, Android 10): dock popu
  4f4235e ckpt 826: X18b DONE: StreamEngine writes TS HLS straight into an MP4 (decideMux probes s
  70b4d0b ckpt 804: X18a DONE (core, JVM-tested, ffmpeg-verified): Avc.kt (SPS/NAL), TsDemux.kt (s
  1b3baa2 ckpt 756: X17 dock download icon VERIFIED on emulator after a real redraw (home+relaunch
  51942ec ckpt 744: owner message 11 written into TASKS as X14-X18 in their words (long-press down
  e8ca3f6 ckpt 741: M8-lite: Settings > Downloads (speed limit 2 MB/s..250 KB/s applied at once, d
  06945e0 ckpt 733: Owner told (alpha16 reply: Release link, repeated message 10 answered part by 
  b87f19d ckpt 732: alpha16 SHIPPED + RECORDED (run 43143728373 green; the two earlier runs failed
  dc57dde ckpt 729: stream engine: a dropped job (merged-away sound of a pair, cancelled, cleared)
```
