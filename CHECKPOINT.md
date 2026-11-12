# CHECKPOINT 1004 — read me first, then TASKS.md

**Written:** 2026-11-12T09:25:49Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `1b67245` (this checkpoint is the commit after it)

## Just done
gated kestrel-courier-v0.1.0-alpha2 (courier code 2) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"courier","full_build":"true","note":"alpha2: the phone now also downloads VIDEOS for the tablet: HLS streams, and DASH videos whose picture and sound come apart (the phone merges them with its own converter). It writes the MP4 and hands the finished file to the tablet over the hotspot wifi. If the phone cannot do a video it says so and the tablet does it itself. Install it over alpha1 (same signing key)."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-courier-v0.1.0-alpha2), then:  bash tools/record-release.sh kestrel-courier-v0.1.0-alpha2 "alpha2: the phone now also downloads VIDEOS for the tablet: HLS streams, and DASH videos whose picture and sound come apart (the phone merges them with its own converter). It writes the MP4 and hands the finished file to the tablet over the hotspot wifi. If the phone cannot do a video it says so and the tablet does it itself. Install it over alpha1 (same signing key)."
NEVER ship kestrel-courier-v0.1.0-alpha2 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  1b67245 ckpt 1003: Y2h2 verified end to end and documented; full gates green (core 506 tests, ap
  366f38a ckpt 996: Y2h2 code: Remuxer moved to shared-android/kotlin (package app.kestrel.platfor
  eab846d ckpt 989: Y2h stage 2a ticked with emulator evidence (video through the phone: live row,
  325467d ckpt 986: Y2 stage 2 (video streams through the phone) code complete, JVM + build verifi
  f6958c7 ckpt 976: Y2 stage 1 SHIPPED and recorded: kestrel-courier-v0.1.0-alpha1 (run 4317795327
  6dbba44 ckpt 973: gated kestrel-g-v0.3.0-alpha19 (gecko code 118) and pushed it: handoff self-te
  cebfa82 ckpt 969: gated kestrel-courier-v0.1.0-alpha1 (courier code 1) and pushed it: handoff se
  919c2a3 ckpt 968: pre-ship: first Courier release: the small app for your PHONE. It downloads fi
  1df58a2 ckpt 967: pre-ship: first Courier release: the small app for your PHONE. It downloads fi
  354d25f ckpt 960: Y2g (CI plumbing for the third APK) written: appinfo.sh/ship.sh/record-release
```
