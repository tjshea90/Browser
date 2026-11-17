# CHECKPOINT 2626 — read me first, then TASKS.md

**Written:** 2026-11-17T11:43:11Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `d9efb312` (this checkpoint is the commit after it)

## Just done
alpha64 DISPATCHED ONCE at 11:42Z: android.yml run 44076463792 (app=gecko full_build=true) on commit d9efb312 = ckpt 2625 (gates green: handoff self-test, versionCode 165 > 164, unit suites). DO NOT DISPATCH AGAIN. The inline-player gate was cleared first: lab ip run 44073666762 = 3 scenes ok, 31 check rows ok, 0 issues (film inside the page + quality list), Diagnostics there shows engine prefs version=10 applied. aot lab (2 rounds, run 44074456934) is running in parallel (informational; saves partial reports).

## Do this next
1) wait for run 44076463792 (jobs: check, test-js, test-jvm, build-gecko; poll list_workflow_jobs, do not poll get_job_logs): green -> verify the tag (git ls-remote --tags origin kestrel-g-v0.3.0-alpha64) and the Release asset (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha64: kestrel-g-0.3.0-alpha64.apk), THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha64 with the IDENTICAL note = the text of docs/handoff/ALPHA64_RELEASE_NOTE.txt (tr -d newline); 2) read aot run 44074456934 (AOT-REPORT.md) -> research/LAB5_PACKAGING.md section 4 + LAB5_RESULTS.md; 3) dispatch lab_suite=daily on the released commit, then buildreport.py --write-baseline research/lab-baseline.json; 4) HANDOFF.md alpha64 entry, tick LAB3/LAB4/LAB5 rows in TASKS.md, answer the owner in plain words (Release link on its own line), delete send_later trigger trig_01B3Wx9aKtTueNWZ2BwMts6R

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d9efb312 ckpt 2625: gated kestrel-g-v0.3.0-alpha64 (gecko code 165) and pushed it: handoff self-
  4692623b ckpt 2624: RELEASE PREP (not shipped): versionCode 165 / versionName 0.3.0-alpha64 in g
  1c468757 ckpt 2623: SWEEP READ (run 44059232336, cancelled after the emulator went offline: 17 g
  bd981349 ckpt 2622: LAB5: OwnerRulesTest.lab5MeasuredPackagingProfileAndPrefsStay (33 tests in t
  c83d9f96 ckpt 2621: LAB5c batch 2 proven by tests: JVM suite 1972 tests / 0 failures (core + app
  41f2a308 ckpt 2620: LAB5a digests written (research/LAB5_PACKAGING, _STARTUP, _EXTENSION, _PREFS
  a55d8865 ckpt 2619: LAB5c-1 (first real app change, measured by a same-APK A/B): GeckoEngine.pre
  da746ada ckpt 2618: LAB5b: more lab tooling committed (no run yet): suite 'sweep' = tools/emu/pr
  96e097e3 ckpt 2617: LAB5b: pref-sweep tooling started: GeckoPrefs.writeConfig reads debug.kestre
  97f31512 ckpt 2616: LAB5b: packaging A/B lab built and wired: geckoapp/build.gradle.kts has two 
```
