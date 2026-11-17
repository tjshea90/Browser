# CHECKPOINT 2632 — read me first, then TASKS.md

**Written:** 2026-11-17T16:02:53Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `0fa23623` (this checkpoint is the commit after it)

## Just done
alpha65 run is android.yml run 44120758574 (run_number 216, started 16:00:17Z, in progress at 16:02Z, head d32b8cf2). DISPATCHED ONCE - DO NOT DISPATCH AGAIN. A background loop polls git ls-remote --tags for kestrel-g-v0.3.0-alpha65 (60 min limit; the tag appears when the build is green).

## Do this next
1) when the tag appears: list_workflow_jobs 44120758574 (all green?), get_release_by_tag kestrel-g-v0.3.0-alpha65 (assets: kestrel-g-0.3.0-alpha65.apk, .apk.sha256 or digest, kestrel-g-0.3.0-alpha65.dm), then bash tools/record-release.sh kestrel-g-v0.3.0-alpha65 with the IDENTICAL note from docs/handoff/ALPHA65_RELEASE_NOTE.txt (newlines removed), tick LAB6d, HANDOFF alpha65 entry, ckpt. 2) meanwhile LAB7a: read tools/emu/aotlab.py release_check, add the speed ceiling + profile variants (all-hot / wildcard) and a lab-only -PlabHealth build flag

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  0fa23623 ckpt 2631: alpha65 DISPATCHED ONCE (compile at install = LAB6: release script publishes
  d32b8cf2 ckpt 2630: gated kestrel-g-v0.3.0-alpha65 (gecko code 166) and pushed it: handoff self-
  a6159180 ckpt 2629: OWNER SAID GO on LAB6 (compile at install through the updater). Recorded in 
  20e28803 ckpt 2628: LAB5 PASS COMPLETE. alpha64 shipped 11:52Z (https://github.com/tjshea90/Brow
  cda703be ckpt 2627: alpha64 SHIPPED + RECORDED (Release kestrel-g-v0.3.0-alpha64, run 4407646379
  e5e2e612 ckpt 2626: alpha64 DISPATCHED ONCE at 11:42Z: android.yml run 44076463792 (app=gecko fu
  d9efb312 ckpt 2625: gated kestrel-g-v0.3.0-alpha64 (gecko code 165) and pushed it: handoff self-
  4692623b ckpt 2624: RELEASE PREP (not shipped): versionCode 165 / versionName 0.3.0-alpha64 in g
  1c468757 ckpt 2623: SWEEP READ (run 44059232336, cancelled after the emulator went offline: 17 g
  bd981349 ckpt 2622: LAB5: OwnerRulesTest.lab5MeasuredPackagingProfileAndPrefsStay (33 tests in t
```
