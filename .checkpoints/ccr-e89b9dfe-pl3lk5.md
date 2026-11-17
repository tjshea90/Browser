# CHECKPOINT 2622 — read me first, then TASKS.md

**Written:** 2026-11-17T10:21:28Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `87fc9c1f` (this checkpoint is the commit after it)

## Just done
LAB5: OwnerRulesTest.lab5MeasuredPackagingProfileAndPrefsStay (33 tests in the class, 0 failures) keeps omniStored default, verify-apk asserts, the baseline profile (>8000 rules), prefs v10, one onMessage listener, manifest without duplicate keys, cache2/entries skip. aotlab.py now has three parts: debug A/B (verify / speed-profile / speed), release_check (the R8-minified release build for the emulator ABI: embedded baseline profile installed by androidx.profileinstaller via the INSTALL_PROFILE broadcast, compiled, am start -W TotalTime vs verify) and dm_install_check (a PackageInstaller session with base.apk + base.dm from AGP's baselineProfiles/<n>/*.dm = can the profile be compiled AT INSTALL TIME; reason install-dm), wired into emu-lab.yml suite aot (builds the release x86_64 APK too); packlab/prefsweep memory parsers fixed for Android 10 (Anonymous). Looked at and REFUSED (written in research/LAB5_PACKAGING.md): removing the :crashhelper process (a crashed Gecko process could hang writing to a pipe nobody reads). Sweep run 44059232336 still running at 10:21Z (35 starts); after-run daily queued behind it.

## Do this next
1) read sweep run 44059232336 when done (artifact emulator-lab-text: SWEEP-REPORT.md + sweep-results.json) -> research/LAB5_PREFSWEEP.md run 1, decide nursery16 / gcpar / gcsmall / canvas64 (adopt only beyond the base-vs-base2 noise) and read nowarm (= GeckoEngine.prewarmConfig worth); 2) when the daily STARTS dispatch lab_suite=vp (video after the extension dispatcher change), when that starts dispatch lab_suite=aot lab_minutes=3 (baseline profile worth + dm install check); 3) read daily vs run 11 / research/lab-baseline.json; typed-address-without-scheme-loads failed in run 11 (the address bar never entered typing mode in that step: tap missed) - is it a flake?; 4) alpha64 release by the recipe (check git ls-remote --tags origin first); 5) answer the owner (plain words) + tick LAB3/LAB4/LAB5 rows with evidence; delete the send_later trigger trig_01N9rqESvUntvENiV7VxVhfi when done

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  c83d9f96 ckpt 2621: LAB5c batch 2 proven by tests: JVM suite 1972 tests / 0 failures (core + app
  41f2a308 ckpt 2620: LAB5a digests written (research/LAB5_PACKAGING, _STARTUP, _EXTENSION, _PREFS
  a55d8865 ckpt 2619: LAB5c-1 (first real app change, measured by a same-APK A/B): GeckoEngine.pre
  da746ada ckpt 2618: LAB5b: more lab tooling committed (no run yet): suite 'sweep' = tools/emu/pr
  96e097e3 ckpt 2617: LAB5b: pref-sweep tooling started: GeckoPrefs.writeConfig reads debug.kestre
  97f31512 ckpt 2616: LAB5b: packaging A/B lab built and wired: geckoapp/build.gradle.kts has two 
  1cf2f4fd ckpt 2615: LAB5 (owner message 2026-11-17T06:42:30Z: improve the browser as much as pos
  cc1424d3 ckpt 2614: alpha63 SHIPPED + recorded (run 44046401335 green, release kestrel-g-v0.3.0-
  12cd7f08 ckpt 2613: alpha63 dispatched ONCE (run_workflow app=gecko full_build=true ref ccr-a10e
  d59a6ddb ckpt 2612: gated kestrel-g-v0.3.0-alpha63 (gecko code 164) and pushed it: handoff self-
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
