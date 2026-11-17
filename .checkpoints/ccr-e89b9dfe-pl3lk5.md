# CHECKPOINT 2625 — read me first, then TASKS.md

**Written:** 2026-11-17T11:42:24Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `a85cd89b` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha64 (gecko code 165) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-e89b9dfe-pl3lk5, inputs {"app":"gecko","full_build":"true","note":"alpha64: the speed and lightness pass you asked for (ultra fast, snappy, lightweight). I ran the tablet-shaped test phone on GitHub six ways (a whole day of use before and after, the video suite, a packaging comparison, a start-up comparison, an engine settings sweep, a start-up profile) and changed only what a measurement or a stack sample supported. What is new: (1) the engine's 14 MB code bundle is stored uncompressed in the app file, so the engine maps it instead of unpacking it into every engine process: in the test phone a warm start was 11 percent faster and about 28 MB less memory was used after browsing. (2) A start-up profile of the 6700 methods the app, the engine's Java and the player really use is built in; Android turns it into machine code when the tablet is idle and charging (Diagnostics line start art says when it has), so the first days after an update should feel quicker. (3) The ad blocker's extension answers the engine through one message listener instead of 24 (less work per page), no longer runs its player scripts inside human-check frames such as Cloudflare (that was a bug), and scans class names less often. (4) The engine no longer lists its whole page cache (up to 40000 files) at every start. (5) Engine settings version 10: no login or form-history machinery (nothing is stored anyway) and no captcha telemetry actor. (6) Small start-up changes: the settings files are read earlier, the Recents icon is set later, and the engine's config file is read on a spare thread before the engine starts. NOT proven on the tablet: the test phone is a fast x86 machine with a software GPU, so start-up and page speed looked the same there; the gains above are measured memory and engine-create time, the rest is expected on the tablet's slower cores. Try: update, use it as usual, leave the tablet charging overnight, then send the Diagnostics (lines start art and engine runtime-create-took)."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha64), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha64 "alpha64: the speed and lightness pass you asked for (ultra fast, snappy, lightweight). I ran the tablet-shaped test phone on GitHub six ways (a whole day of use before and after, the video suite, a packaging comparison, a start-up comparison, an engine settings sweep, a start-up profile) and changed only what a measurement or a stack sample supported. What is new: (1) the engine's 14 MB code bundle is stored uncompressed in the app file, so the engine maps it instead of unpacking it into every engine process: in the test phone a warm start was 11 percent faster and about 28 MB less memory was used after browsing. (2) A start-up profile of the 6700 methods the app, the engine's Java and the player really use is built in; Android turns it into machine code when the tablet is idle and charging (Diagnostics line start art says when it has), so the first days after an update should feel quicker. (3) The ad blocker's extension answers the engine through one message listener instead of 24 (less work per page), no longer runs its player scripts inside human-check frames such as Cloudflare (that was a bug), and scans class names less often. (4) The engine no longer lists its whole page cache (up to 40000 files) at every start. (5) Engine settings version 10: no login or form-history machinery (nothing is stored anyway) and no captcha telemetry actor. (6) Small start-up changes: the settings files are read earlier, the Recents icon is set later, and the engine's config file is read on a spare thread before the engine starts. NOT proven on the tablet: the test phone is a fast x86 machine with a software GPU, so start-up and page speed looked the same there; the gains above are measured memory and engine-create time, the rest is expected on the tablet's slower cores. Try: update, use it as usual, leave the tablet charging overnight, then send the Diagnostics (lines start art and engine runtime-create-took)."
NEVER ship kestrel-g-v0.3.0-alpha64 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  4692623b ckpt 2624: RELEASE PREP (not shipped): versionCode 165 / versionName 0.3.0-alpha64 in g
  1c468757 ckpt 2623: SWEEP READ (run 44059232336, cancelled after the emulator went offline: 17 g
  bd981349 ckpt 2622: LAB5: OwnerRulesTest.lab5MeasuredPackagingProfileAndPrefsStay (33 tests in t
  c83d9f96 ckpt 2621: LAB5c batch 2 proven by tests: JVM suite 1972 tests / 0 failures (core + app
  41f2a308 ckpt 2620: LAB5a digests written (research/LAB5_PACKAGING, _STARTUP, _EXTENSION, _PREFS
  a55d8865 ckpt 2619: LAB5c-1 (first real app change, measured by a same-APK A/B): GeckoEngine.pre
  da746ada ckpt 2618: LAB5b: more lab tooling committed (no run yet): suite 'sweep' = tools/emu/pr
  96e097e3 ckpt 2617: LAB5b: pref-sweep tooling started: GeckoPrefs.writeConfig reads debug.kestre
  97f31512 ckpt 2616: LAB5b: packaging A/B lab built and wired: geckoapp/build.gradle.kts has two 
  1cf2f4fd ckpt 2615: LAB5 (owner message 2026-11-17T06:42:30Z: improve the browser as much as pos
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
