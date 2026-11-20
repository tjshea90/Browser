# CHECKPOINT 2634 — read me first, then TASKS.md

**Written:** 2026-11-17T16:22:18Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `55e756fd` (this checkpoint is the commit after it)

## Just done
kestrel-g-v0.3.0-alpha66 (gecko code 167) is SHIPPED AND RECORDED (run 44125112190 green, Release has the APK, BUILDLOG.md line written): do NOT dispatch or ship it again. After it, on this branch only: the vendored keyboard engine copy was re-synced to the Keyboard repo commit 4644951 (KEYBOARD_PIN updated, check-keyboard-core.sh passes): the Android 10 "text reports arrive inside the edit call" fix in InputController (matters only for an editor in the keyboard's own app, so Kestrel's own page-field keyboard behaves as before). Not released: the next Kestrel release carries it.

## Do this next
Nothing is running for this lane. Wait for the owner: tablet test of alpha66 (the panel bar opens the Swoop keyboard floating when Swoop is the chosen keyboard; Settings > Use the Swoop keyboard from the panel). The next release: bump versionCode/versionName, bash ship.sh gecko "<note>", ONE dispatch of android.yml, record-release.sh only after the run is green.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  60f8deca ckpt 2633: alpha65 SHIPPED and RECORDED (run 44120758574 all 7 jobs green, Release kest
  f7d26151 ckpt 2632: alpha65 run is android.yml run 44120758574 (run_number 216, started 16:00:17
  0fa23623 ckpt 2631: alpha65 DISPATCHED ONCE (compile at install = LAB6: release script publishes
  d32b8cf2 ckpt 2630: gated kestrel-g-v0.3.0-alpha65 (gecko code 166) and pushed it: handoff self-
  a6159180 ckpt 2629: OWNER SAID GO on LAB6 (compile at install through the updater). Recorded in 
  20e28803 ckpt 2628: LAB5 PASS COMPLETE. alpha64 shipped 11:52Z (https://github.com/tjshea90/Brow
  cda703be ckpt 2627: alpha64 SHIPPED + RECORDED (Release kestrel-g-v0.3.0-alpha64, run 4407646379
  e5e2e612 ckpt 2626: alpha64 DISPATCHED ONCE at 11:42Z: android.yml run 44076463792 (app=gecko fu
  d9efb312 ckpt 2625: gated kestrel-g-v0.3.0-alpha64 (gecko code 165) and pushed it: handoff self-
  4692623b ckpt 2624: RELEASE PREP (not shipped): versionCode 165 / versionName 0.3.0-alpha64 in g
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
