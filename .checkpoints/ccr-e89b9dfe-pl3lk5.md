# CHECKPOINT 2623 — read me first, then TASKS.md

**Written:** 2026-11-17T10:35:23Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `1ac6896e` (this checkpoint is the commit after it)

## Just done
SWEEP READ (run 44059232336, cancelled after the emulator went offline: 17 good starts): every variant inside the noise of two identical configs (base vs base2 differ by 45 MB PSS and 0.12 s engine-ready; warm bench +-3 %): NO nursery / gcpar / gcsmall / canvas64 pref adopted; nowarm (config warm-up thread off) = no x86 difference, GeckoEngine.prewarmConfig kept on theory + the stack sample (stated as NOT a measured gain). tools/emu/prefsweep.py hardened: saves after every start, adb get-state check, stops after two dead starts. research/LAB5_PREFSWEEP.md run 1 + research/LAB5_RESULTS.md written; HANDOFF.md points at LAB5. IN FLIGHT: after-run daily 44062596768 (commit c83d9f96 = all LAB5c app changes) started 10:33Z; vp run queued behind it (dispatched 10:36Z).

## Do this next
1) when the daily STARTS its successor (vp) is already queued: after vp starts dispatch lab_suite=aot lab_minutes=3 (NOT the default 60); 2) read daily 44062596768 (artifacts emulator-lab-text + build-report) vs run 11 (44054301212) and research/lab-baseline.json: typed-address-without-scheme-loads, start timeline, memory, scroll gaps; update research/lab-baseline.json with buildreport.py --write-baseline only if the run is clean; 3) read vp (video hand-over after the extension dispatcher change: any failed check = fix before release); 4) read aot (research/LAB5_PACKAGING.md section 4: baseline profile worth, release path, dm install check); 5) alpha64 release recipe (git ls-remote --tags origin first), answer the owner, tick LAB3/LAB4/LAB5 rows, delete send_later trigger trig_01N9rqESvUntvENiV7VxVhfi

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  bd981349 ckpt 2622: LAB5: OwnerRulesTest.lab5MeasuredPackagingProfileAndPrefsStay (33 tests in t
  c83d9f96 ckpt 2621: LAB5c batch 2 proven by tests: JVM suite 1972 tests / 0 failures (core + app
  41f2a308 ckpt 2620: LAB5a digests written (research/LAB5_PACKAGING, _STARTUP, _EXTENSION, _PREFS
  a55d8865 ckpt 2619: LAB5c-1 (first real app change, measured by a same-APK A/B): GeckoEngine.pre
  da746ada ckpt 2618: LAB5b: more lab tooling committed (no run yet): suite 'sweep' = tools/emu/pr
  96e097e3 ckpt 2617: LAB5b: pref-sweep tooling started: GeckoPrefs.writeConfig reads debug.kestre
  97f31512 ckpt 2616: LAB5b: packaging A/B lab built and wired: geckoapp/build.gradle.kts has two 
  1cf2f4fd ckpt 2615: LAB5 (owner message 2026-11-17T06:42:30Z: improve the browser as much as pos
  cc1424d3 ckpt 2614: alpha63 SHIPPED + recorded (run 44046401335 green, release kestrel-g-v0.3.0-
  12cd7f08 ckpt 2613: alpha63 dispatched ONCE (run_workflow app=gecko full_build=true ref ccr-a10e
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
