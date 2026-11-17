# CHECKPOINT 2629 — read me first, then TASKS.md

**Written:** 2026-11-17T15:43:55Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `20e28803` (this checkpoint is the commit after it)

## Just done
OWNER SAID GO on LAB6 (compile at install through the updater). Recorded in TASKS.md LAB6 + sub-boxes LAB6a-d BEFORE any code. Plan: lab first (dm_install_check with the updater's real names kestrel-update.apk/.dm, an update over an installed copy, a corrupt dm = which failure text), then release side (publish kestrel-g-<ver>.dm), updater (Offer + best-effort fetch + session write on API 28-30 + one retry without dm), tests, alpha65.

## Do this next
LAB6a: extend tools/emu/aotlab.py dm_install_check (names, update-over-installed, corrupt dm variants) and run lab_suite=aot (lab_minutes=1; release phase only: add a --dm-only flag so it takes ~10 min); read UpdateManager.kt / InstallResultReceiver.kt / UpdateLogic (offer parse, download, verify, install) while it runs

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  20e28803 ckpt 2628: LAB5 PASS COMPLETE. alpha64 shipped 11:52Z (https://github.com/tjshea90/Brow
  cda703be ckpt 2627: alpha64 SHIPPED + RECORDED (Release kestrel-g-v0.3.0-alpha64, run 4407646379
  e5e2e612 ckpt 2626: alpha64 DISPATCHED ONCE at 11:42Z: android.yml run 44076463792 (app=gecko fu
  d9efb312 ckpt 2625: gated kestrel-g-v0.3.0-alpha64 (gecko code 165) and pushed it: handoff self-
  4692623b ckpt 2624: RELEASE PREP (not shipped): versionCode 165 / versionName 0.3.0-alpha64 in g
  1c468757 ckpt 2623: SWEEP READ (run 44059232336, cancelled after the emulator went offline: 17 g
  bd981349 ckpt 2622: LAB5: OwnerRulesTest.lab5MeasuredPackagingProfileAndPrefsStay (33 tests in t
  c83d9f96 ckpt 2621: LAB5c batch 2 proven by tests: JVM suite 1972 tests / 0 failures (core + app
  41f2a308 ckpt 2620: LAB5a digests written (research/LAB5_PACKAGING, _STARTUP, _EXTENSION, _PREFS
  a55d8865 ckpt 2619: LAB5c-1 (first real app change, measured by a same-APK A/B): GeckoEngine.pre
```
