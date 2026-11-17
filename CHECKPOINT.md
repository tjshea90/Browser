# CHECKPOINT 2612 — read me first, then TASKS.md

**Written:** 2026-11-17T08:23:53Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a10e07b7-6zxjmi` · **builds on:** `3cdec9fe` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha63 (gecko code 164) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a10e07b7-6zxjmi, inputs {"app":"gecko","full_build":"true","note":"alpha63: a small, careful build; I did not find a speed problem I could prove, so I changed nothing about how fast pages load. What I did: I ran the whole tablet-shaped test phone on GitHub again (two clean runs, one with the start-up profiler) and looked for the cause of the 1 second pause at start-up. Result: the pause is mostly the test phone's software graphics (not your tablet's), the engine create costs about 0.17 s, the rest is idle; so I left start-up alone rather than guess. The second run's network and Google probes failed (the test phone's own network, not the app), so no Google rule was added. Changes in the app itself: (1) when a video is playing and you use the Android back gesture on Android 13 or newer (your Android 16 phone), Kestrel's own player now closes properly (it used to rely on the old back key only); on your Android 10 tablet nothing changes. (2) Extra measuring only in test builds (what the main thread is doing during a long pause), not in this release for you. The tablet idle cost is about 5 percent CPU, of which about 4 percent is the glowing Neon edge you asked for; switching Neon off would save it, tell me if you want that. NOT tested on your tablet; the back-gesture change was only compiled and unit-tested, the emulator image is Android 10 and cannot run it."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha63), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha63 "alpha63: a small, careful build; I did not find a speed problem I could prove, so I changed nothing about how fast pages load. What I did: I ran the whole tablet-shaped test phone on GitHub again (two clean runs, one with the start-up profiler) and looked for the cause of the 1 second pause at start-up. Result: the pause is mostly the test phone's software graphics (not your tablet's), the engine create costs about 0.17 s, the rest is idle; so I left start-up alone rather than guess. The second run's network and Google probes failed (the test phone's own network, not the app), so no Google rule was added. Changes in the app itself: (1) when a video is playing and you use the Android back gesture on Android 13 or newer (your Android 16 phone), Kestrel's own player now closes properly (it used to rely on the old back key only); on your Android 10 tablet nothing changes. (2) Extra measuring only in test builds (what the main thread is doing during a long pause), not in this release for you. The tablet idle cost is about 5 percent CPU, of which about 4 percent is the glowing Neon edge you asked for; switching Neon off would save it, tell me if you want that. NOT tested on your tablet; the back-gesture change was only compiled and unit-tested, the emulator image is Android 10 and cannot run it."
NEVER ship kestrel-g-v0.3.0-alpha63 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  3cdec9fe ckpt 2611: run 9 read: degraded run (emulator restarted, no network, Google A/B all 60 
  ede3c889 ckpt 2610: OWNER RESCHEDULE (usage running out, 2026-11-17 04:58Z): ALL work moved to O
  8bf31a36 ckpt 2609: LAB4b: decisions recorded: ANS60e NOT done (would delay every navigation's g
  b68aca47 ckpt 2608: LAB4a/b: run 8 (44017705030, instrumented app, fixed lab order, fast link) i
  edacce50 ckpt 2607: LAB4a: run 7 (44010606968, instrumented app, hotspot, OLD lab order) CANCELL
  7f6901e6 ckpt 2606: LAB4a: run 6 (44010165696) was CANCELLED after 53 min in the lab step; its a
  c2e8ae15 ckpt 2605: LAB4b: ANS60f DONE in code (NOT testable on the Android 10 lab, compile-chec
  4ada6834 ckpt 2604: LAB4b: instrumentation step verified: JVM suites :core:test :app:testDebugUn
  d7f8b405 ckpt 2603: LAB3/LAB4a: run 5 (44008297246) READ: all lab fixes held (Features + Securit
  4dc88c0d ckpt 2602: LAB4b step 1 (measure before fixing, evidence rule): app instrumentation onl
```
