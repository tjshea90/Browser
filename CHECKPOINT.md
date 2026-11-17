# CHECKPOINT 2621 — read me first, then TASKS.md

**Written:** 2026-11-17T10:11:05Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `b4bd84fa` (this checkpoint is the commit after it)

## Just done
LAB5c batch 2 proven by tests: JVM suite 1972 tests / 0 failures (core + app incl. new StorageInfoPurgeTest; gecko release Kotlin compiles), web-tests 581 pass. LOCAL RELEASE BUILD (assembleRelease) inspected: assets/omni.ja is Stored (15.2 MB), assets/dexopt/baseline.prof 5,913 B + .profm 762 B (was 246 B; R8 rewrote 14.2k rules to 6.7k), tools/verify-apk.sh now asserts both (passes on the local APK). NEW: Health line 'start art' (androidx ProfileVerifier, 30 s after resume) tells on the tablet whether Android has compiled the profile; GeckoApp.onCreate asks for the 3 SharedPreferences files at once; setRecentsLook moved out of the first-frame callback; androidx.profileinstaller:profileinstaller:1.4.0 declared in :geckoapp (already in the APK at runtime). NEW lab suite aot (tools/emu/aotlab.py + workflow wiring in emu-lab.yml/android.yml): one APK compiled as verify / speed-profile (trained profile) / speed, cold starts of each = what a baseline profile is worth; packlab smaps parser fixed for Android 10 (Anonymous instead of Pss_Anon). TASKS.md LAB5a ticked, LAB5c-1..7 written with evidence. Daily run 44054301212 (HEAD da746ada, before these changes) is the BEFORE picture: scene typed-address-without-scheme-loads FAILED there (google page stayed, no wikipedia navigation in 90 s while the emulator janked: ui slow 950-1600 ms) but passed in run 8: re-check in the after-run before calling it a regression.

## Do this next
1) read sweep run 44059232336 (variants base base2 nowarm nursery16 gcpar gcsmall canvas64) -> fill research/LAB5_PREFSWEEP.md 'Run 1' + decide GC prefs; 2) dispatch ONE after-run daily (lab_suite=daily) on this HEAD, then lab_suite=vp (video hand-over after the extension dispatcher change), then lab_suite=aot (lab_minutes=3); each one pending slot; 3) compare daily vs research/lab-baseline.json + run 11; 4) alpha64 recipe: geckoapp/build.gradle.kts versionCode 165 / versionName 0.3.0-alpha64, bash tools/sync-ext-version.sh, docs/handoff/ALPHA64_RELEASE_NOTE.txt, bash ship.sh gecko note, ONE dispatch, verify tag/jobs/release, bash tools/record-release.sh kestrel-g-v0.3.0-alpha64 with the identical note; 5) answer the owner in plain words (before/after numbers, what the emulator cannot show, Neon idle pulse choice, libs-in-APK option, DM-file install idea not done)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  41f2a308 ckpt 2620: LAB5a digests written (research/LAB5_PACKAGING, _STARTUP, _EXTENSION, _PREFS
  a55d8865 ckpt 2619: LAB5c-1 (first real app change, measured by a same-APK A/B): GeckoEngine.pre
  da746ada ckpt 2618: LAB5b: more lab tooling committed (no run yet): suite 'sweep' = tools/emu/pr
  96e097e3 ckpt 2617: LAB5b: pref-sweep tooling started: GeckoPrefs.writeConfig reads debug.kestre
  97f31512 ckpt 2616: LAB5b: packaging A/B lab built and wired: geckoapp/build.gradle.kts has two 
  1cf2f4fd ckpt 2615: LAB5 (owner message 2026-11-17T06:42:30Z: improve the browser as much as pos
  cc1424d3 ckpt 2614: alpha63 SHIPPED + recorded (run 44046401335 green, release kestrel-g-v0.3.0-
  12cd7f08 ckpt 2613: alpha63 dispatched ONCE (run_workflow app=gecko full_build=true ref ccr-a10e
  d59a6ddb ckpt 2612: gated kestrel-g-v0.3.0-alpha63 (gecko code 164) and pushed it: handoff self-
  3cdec9fe ckpt 2611: run 9 read: degraded run (emulator restarted, no network, Google A/B all 60 
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
