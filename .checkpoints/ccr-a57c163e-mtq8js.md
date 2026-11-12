# CHECKPOINT 1007 — read me first, then TASKS.md

**Written:** 2026-11-12T09:28:10Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `b01309f` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha20 (gecko code 119) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha20: Download on my phone now covers videos too. With Settings > Download on my phone on and the new Kestrel Courier (alpha2) running on your phone, the Download video button hands HLS videos and picture-plus-sound (DASH) videos to the phone: the tablet shows the same live row (progress, speed, Pause, Cancel), the phone writes the MP4, then the tablet copies the finished file. If the phone cannot do it, the tablet downloads it itself and the row says why. With an older Courier the tablet simply downloads by itself."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha20), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha20 "alpha20: Download on my phone now covers videos too. With Settings > Download on my phone on and the new Kestrel Courier (alpha2) running on your phone, the Download video button hands HLS videos and picture-plus-sound (DASH) videos to the phone: the tablet shows the same live row (progress, speed, Pause, Cancel), the phone writes the MP4, then the tablet copies the finished file. If the phone cannot do it, the tablet downloads it itself and the row says why. With an older Courier the tablet simply downloads by itself."
NEVER ship kestrel-g-v0.3.0-alpha20 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d3a8580 ckpt 1005: gated kestrel-g-v0.3.0-alpha20 (gecko code 119) and pushed it: handoff self-t
  5047bd2 ckpt 1004: gated kestrel-courier-v0.1.0-alpha2 (courier code 2) and pushed it: handoff s
  1b67245 ckpt 1003: Y2h2 verified end to end and documented; full gates green (core 506 tests, ap
  366f38a ckpt 996: Y2h2 code: Remuxer moved to shared-android/kotlin (package app.kestrel.platfor
  eab846d ckpt 989: Y2h stage 2a ticked with emulator evidence (video through the phone: live row,
  325467d ckpt 986: Y2 stage 2 (video streams through the phone) code complete, JVM + build verifi
  f6958c7 ckpt 976: Y2 stage 1 SHIPPED and recorded: kestrel-courier-v0.1.0-alpha1 (run 4317795327
  6dbba44 ckpt 973: gated kestrel-g-v0.3.0-alpha19 (gecko code 118) and pushed it: handoff self-te
  cebfa82 ckpt 969: gated kestrel-courier-v0.1.0-alpha1 (courier code 1) and pushed it: handoff se
  919c2a3 ckpt 968: pre-ship: first Courier release: the small app for your PHONE. It downloads fi
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
