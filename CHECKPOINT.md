# CHECKPOINT 2633 — read me first, then TASKS.md

**Written:** 2026-11-17T16:10:20Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `1e6d8fa8` (this checkpoint is the commit after it)

## Just done
alpha65 SHIPPED and RECORDED (run 44120758574 all 7 jobs green, Release kestrel-g-v0.3.0-alpha65 has the APK 95.9 MB, .apk.sha256 and kestrel-g-0.3.0-alpha65.dm 6,766 bytes, BUILDLOG commit 4a810288); TASKS LAB6 ticked, HANDOFF alpha65 entry written. Owner message LAB7 is TASKS LAB7a-c.

## Do this next
LAB7a+b in tools/emu/aotlab.py: (1) packlab.start_server: add /ping page + srv.pings timestamps (host-clock launch->page-loaded for the minified RELEASE build, no app change), (2) aotlab breadth_check(): verify / speed-profile (embedded profile) / all-hot (profman dump -> every line H -> --create-profile-from) / everything (profman --generate-test-profile 100 pct with --apk) / speed, each: compile seconds, oat files + KB, am start -W + ping n=6 alternating warm/cold, main-process PSS, (3) aotlab update_check(): debug APK old (vc N) + new (vc N+1, extension version bumped via tools/sync-ext-version.sh): install old, 2 runs, adb install -r new, launch #1/#2/#3 with Health marks, plus a run with a warm-up launch, record files/mozilla startupCache + compatibility.ini, (4) emu-lab.yml suite aot builds both debug APKs (restore build.gradle.kts + manifest after), android.yml/emu-lab.yml options unchanged (aot exists), (5) then dispatch ONE lab run (android.yml app=lab lab_suite=aot) and read the artifact

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M HANDOFF.md

## Last ten checkpoints
```
  f7d26151 ckpt 2632: alpha65 run is android.yml run 44120758574 (run_number 216, started 16:00:17
  0fa23623 ckpt 2631: alpha65 DISPATCHED ONCE (compile at install = LAB6: release script publishes
  d32b8cf2 ckpt 2630: gated kestrel-g-v0.3.0-alpha65 (gecko code 166) and pushed it: handoff self-
  a6159180 ckpt 2629: OWNER SAID GO on LAB6 (compile at install through the updater). Recorded in 
  20e28803 ckpt 2628: LAB5 PASS COMPLETE. alpha64 shipped 11:52Z (https://github.com/tjshea90/Brow
  cda703be ckpt 2627: alpha64 SHIPPED + RECORDED (Release kestrel-g-v0.3.0-alpha64, run 4407646379
  e5e2e612 ckpt 2626: alpha64 DISPATCHED ONCE at 11:42Z: android.yml run 44076463792 (app=gecko fu
  d9efb312 ckpt 2625: gated kestrel-g-v0.3.0-alpha64 (gecko code 165) and pushed it: handoff self-
  4692623b ckpt 2624: RELEASE PREP (not shipped): versionCode 165 / versionName 0.3.0-alpha64 in g
  1c468757 ckpt 2623: SWEEP READ (run 44059232336, cancelled after the emulator went offline: 17 g
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
