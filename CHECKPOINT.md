# CHECKPOINT 3257 — read me first, then TASKS.md

**Written:** 2026-11-24T19:52:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `c510bc41` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha106 (gecko code 207) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/kind-brahmagupta-ae3yl8, inputs {"app":"gecko","full_build":"true","note":"alpha106: Firefox 161, released today. Kestrel G now runs on the Firefox 161 engine (GeckoView 161.0.20261118180411, the newest, from 2026-11-24) instead of 160. It carries Mozilla's security fixes of this month (advisory 2026-131) and Mozilla's speed-up for pages that blur or frost things behind page elements. The site-fixes add-on is updated to 161.0 as well. FOR YOUR TABLET: every engine setting Kestrel writes was checked against the new engine: all of them still exist and nothing Kestrel uses was removed. The engine is Mozilla's own optimised 32-bit build for your processor. Firefox 161's new settings (new path drawing, a Rust font reader, do not decode pictures that are not on screen) are NOT switched on blindly: the next diagnostics read their default values on your tablet, and the Auto-tune that is being built next will test each one on the tablet itself. TESTED: it compiles against the new engine, the unit tests and browser-engine tests pass, and the emulator test (a pretend Android 10 tablet) started the new engine and loaded pages. NOT run on your tablet. The first start after the update takes longer, and your tabs reopen from their addresses (the old engine's saved page states are not reused with a new engine)."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha106), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha106 "alpha106: Firefox 161, released today. Kestrel G now runs on the Firefox 161 engine (GeckoView 161.0.20261118180411, the newest, from 2026-11-24) instead of 160. It carries Mozilla's security fixes of this month (advisory 2026-131) and Mozilla's speed-up for pages that blur or frost things behind page elements. The site-fixes add-on is updated to 161.0 as well. FOR YOUR TABLET: every engine setting Kestrel writes was checked against the new engine: all of them still exist and nothing Kestrel uses was removed. The engine is Mozilla's own optimised 32-bit build for your processor. Firefox 161's new settings (new path drawing, a Rust font reader, do not decode pictures that are not on screen) are NOT switched on blindly: the next diagnostics read their default values on your tablet, and the Auto-tune that is being built next will test each one on the tablet itself. TESTED: it compiles against the new engine, the unit tests and browser-engine tests pass, and the emulator test (a pretend Android 10 tablet) started the new engine and loaded pages. NOT run on your tablet. The first start after the update takes longer, and your tabs reopen from their addresses (the old engine's saved page states are not reused with a new engine)."
NEVER ship kestrel-g-v0.3.0-alpha106 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  c510bc41 ckpt 3256: OPT1: the Android-side sources and patch scripts are now saved IN the repo (
  f92c5435 ckpt 3255: OPT1: core done + 25 tests green, bench pages + web test green, tools/optimi
  59efade6 ckpt 3253: FF1: alpha106 prepared (code 207, GeckoView 161): lab smoke GREEN on 161 (ru
  29576308 ckpt 3230: FF1: GeckoView 161.0.20261118180411 + webcompat 161.0 pinned, clean compile,
  e7367846 ckpt 3222: wrote FF1 (new Firefox version into Kestrel, optimised for the hardware) and
  2e56492f ckpt 3221: alpha105 (code 206) SHIPPED: run 45352897624 green, Release kestrel-g-v0.3.0
  6fb30f29 ckpt 3219: alpha105 (code 206) gated (ckpt 3218, head 8b6eebf7) and dispatched ONCE: ru
  8b6eebf7 ckpt 3218: gated kestrel-g-v0.3.0-alpha105 (gecko code 206) and pushed it: handoff self
  c45aae47 ckpt 3217: PERF25: app unit tests green; bumped to alpha105 / code 206, release note do
  512c9e18 ckpt 3215: PERF25c: built the hardware drawing trial (core DrawTrial + DrawTrialTest + 
```
