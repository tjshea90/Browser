# CHECKPOINT 2631 — read me first, then TASKS.md

**Written:** 2026-11-17T16:00:55Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `d32b8cf2` (this checkpoint is the commit after it)

## Just done
alpha65 DISPATCHED ONCE (compile at install = LAB6: release script publishes <apk>.dm, UpdateManager.ensureDm + install attach on API 28-30 + InstallResultReceiver one retry without the file; lab aot run 44118008035 proved names kestrel-update.apk/.dm, an UPDATE over an installed copy = install-dm 3753 KB, a non-zip dm = INSTALL_FAILED_BAD_DEX_METADATA with the old copy intact, garbage profile / format 0 = ignored; 23 new tests: core DexMetadataTest 8, app DexMetadataFetchTest 15 incl. install smoke tests; publish script dry-run with a stub gh attaches the .dm). DO NOT DISPATCH ALPHA65 AGAIN: find its run with list_workflow_runs (app gecko), poll the tag with git ls-remote --tags origin kestrel-g-v0.3.0-alpha65 (do NOT read get_workflow_run: its commit message holds the whole note), then verify the Release assets (apk, apk.sha256, .dm) and bash tools/record-release.sh kestrel-g-v0.3.0-alpha65 with the IDENTICAL note (docs/handoff/ALPHA65_RELEASE_NOTE.txt, newlines removed). NEW OWNER MESSAGE (LAB7): 'If there is a way to make the install and updates take even longer, but the browser run faster, do it.' written into TASKS.md as LAB7a-c.

## Do this next
1) wait for the alpha65 release (tag poll, then Release asset check incl. kestrel-g-0.3.0-alpha65.dm, then record-release); 2) LAB7a/7b in the lab: (a) profile breadth: ART compiles only HOT methods, our baseline profile has 6265 P-only (post-startup, not compiled) of 11173 methods: variants current / all-hot / all-hot + wildcards of our packages vs , measure oat size, install/compile time, am start -W n>=8, PSS; (b) scene upd: first launch after adb install -r of a version-bumped build vs later launches; add a lab-only build flag -PlabHealth=true that mirrors Health lines on the minified non-debuggable build; 3) build only what the numbers support, alpha66 by the recipe; 4) answer the owner in plain words

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  d32b8cf2 ckpt 2630: gated kestrel-g-v0.3.0-alpha65 (gecko code 166) and pushed it: handoff self-
  a6159180 ckpt 2629: OWNER SAID GO on LAB6 (compile at install through the updater). Recorded in 
  20e28803 ckpt 2628: LAB5 PASS COMPLETE. alpha64 shipped 11:52Z (https://github.com/tjshea90/Brow
  cda703be ckpt 2627: alpha64 SHIPPED + RECORDED (Release kestrel-g-v0.3.0-alpha64, run 4407646379
  e5e2e612 ckpt 2626: alpha64 DISPATCHED ONCE at 11:42Z: android.yml run 44076463792 (app=gecko fu
  d9efb312 ckpt 2625: gated kestrel-g-v0.3.0-alpha64 (gecko code 165) and pushed it: handoff self-
  4692623b ckpt 2624: RELEASE PREP (not shipped): versionCode 165 / versionName 0.3.0-alpha64 in g
  1c468757 ckpt 2623: SWEEP READ (run 44059232336, cancelled after the emulator went offline: 17 g
  bd981349 ckpt 2622: LAB5: OwnerRulesTest.lab5MeasuredPackagingProfileAndPrefsStay (33 tests in t
  c83d9f96 ckpt 2621: LAB5c batch 2 proven by tests: JVM suite 1972 tests / 0 failures (core + app
```
