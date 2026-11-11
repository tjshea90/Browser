# CHECKPOINT 220 — read me first, then TASKS.md

**Written:** 2026-11-11T04:48:03Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `57a6d62` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.2.0-alpha3 (gecko code 102) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"Alpha 3: fixes for the typing freeze, instant UI with no animations, a Diagnostics screen with a Refresh screen button, tuned and hardened Firefox engine settings; not yet run on the real tablet"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.2.0-alpha3), then:  bash tools/record-release.sh kestrel-g-v0.2.0-alpha3 "Alpha 3: fixes for the typing freeze, instant UI with no animations, a Diagnostics screen with a Refresh screen button, tuned and hardened Firefox engine settings; not yet run on the real tablet"
NEVER ship kestrel-g-v0.2.0-alpha3 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  57a6d62 ckpt 219: alpha3 candidate: freeze fixes verified on emulator (worker bridge, cosmetics 
  04dab0c ckpt 214: A2/A4 part: gecko-prefs.txt -> YAML config (AV1 off, memory/cache budget, no a
  1739d76 ckpt 209: A3 evidence layer: core RingLog (+tests), app Health (watchdog, frame jank, re
  9c46642 ckpt 175: A3 P0 part 1 (Kestrel G): GeckoView attached ONCE (ntp/error as overlays, setS
  21706f6 ckpt 119: A1 part 1: native UI animations off - instant pressed-state overlay instead of
  3cd7e40 ckpt 117: M1-M2 done: tools/sync.sh (auto-merge other sessions), per-branch cards .check
  cb69194 ckpt 109: Owner request 2 recorded: INBOX captured by the hook, TASKS.md boxes M1-M4 R1-
  286f09a ckpt 104: GitHub Actions pipeline proven: run 42951742468 built, signed, verified and pu
  ecfd90c ckpt 98: ship.sh gecko OK (local gates green, pushed). Dispatched android.yml app=both f
  cdbefdf ckpt 97: gated kestrel-g-v0.2.0-alpha2 (gecko code 101) and pushed it: handoff self-test
```
