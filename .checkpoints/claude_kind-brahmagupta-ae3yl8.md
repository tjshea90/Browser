# CHECKPOINT 3274 — read me first, then TASKS.md

**Written:** 2026-11-24T20:57:36Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `89dc777e` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha106 (gecko code 207) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/kind-brahmagupta-ae3yl8, inputs {"app":"gecko","full_build":"true","note":"alpha106: Firefox 161, released today. Kestrel G now runs on the Firefox 161 engine (GeckoView 161.0.20261118180411, the newest, from 2026-11-24) instead of 160. It carries Mozilla's security fixes of this month (advisory 2026-131) and Mozilla's speed-up for pages that blur or frost things behind page elements. The site-fixes add-on is updated to 161.0 as well. FOR YOUR TABLET: every engine setting Kestrel writes was checked against the new engine: all of them still exist and nothing Kestrel uses was removed. The engine is Mozilla's own optimised 32-bit build for your processor. Firefox 161's new settings (new path drawing, a Rust font reader, do not decode pictures that are not on screen) are NOT switched on blindly: the next diagnostics read their default values on your tablet, and the Auto-tune that is being built next will test each one on the tablet itself. TESTED: it compiles against the new engine, the unit tests and browser-engine tests pass, and the emulator test (a pretend Android 10 tablet) started the new engine and loaded pages. NOT run on your tablet. The first start after the update takes longer, and your tabs reopen from their addresses (the old engine's saved page states are not reused with a new engine). ALSO INSIDE, SWITCHED OFF: the first parts of Auto-tune, the automatic settings tester that is being built next (it has no screen in Settings yet, so you will see no change)."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha106), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha106 "alpha106: Firefox 161, released today. Kestrel G now runs on the Firefox 161 engine (GeckoView 161.0.20261118180411, the newest, from 2026-11-24) instead of 160. It carries Mozilla's security fixes of this month (advisory 2026-131) and Mozilla's speed-up for pages that blur or frost things behind page elements. The site-fixes add-on is updated to 161.0 as well. FOR YOUR TABLET: every engine setting Kestrel writes was checked against the new engine: all of them still exist and nothing Kestrel uses was removed. The engine is Mozilla's own optimised 32-bit build for your processor. Firefox 161's new settings (new path drawing, a Rust font reader, do not decode pictures that are not on screen) are NOT switched on blindly: the next diagnostics read their default values on your tablet, and the Auto-tune that is being built next will test each one on the tablet itself. TESTED: it compiles against the new engine, the unit tests and browser-engine tests pass, and the emulator test (a pretend Android 10 tablet) started the new engine and loaded pages. NOT run on your tablet. The first start after the update takes longer, and your tabs reopen from their addresses (the old engine's saved page states are not reused with a new engine). ALSO INSIDE, SWITCHED OFF: the first parts of Auto-tune, the automatic settings tester that is being built next (it has no screen in Settings yet, so you will see no change)."
NEVER ship kestrel-g-v0.3.0-alpha106 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  89dc777e ckpt 3273: alpha106 attempt 2 prepared: the 403 was NOT reproduced by 3 probes (every r
  bfcc866a ckpt 3271: probe v2 result: EVERY release-creation variant works with the workflow toke
  7e08a8c5 ckpt 3269: probe v2: calls POST /releases directly (draft and real, target sha / branch
  8821fc21 ckpt 3268: alpha106 release publish fails with HTTP 403 twice (token has contents: writ
  8caaa5ba ckpt 3266: OPT1: Auto-tune integrated (BenchServer, Store, Apply, Guard, restart trampo
  59502d88 ckpt 3259: alpha106 (code 207) gated (ckpt 3257, head 0f14013b) and dispatched ONCE: ru
  0f14013b ckpt 3257: gated kestrel-g-v0.3.0-alpha106 (gecko code 207) and pushed it: handoff self
  c510bc41 ckpt 3256: OPT1: the Android-side sources and patch scripts are now saved IN the repo (
  f92c5435 ckpt 3255: OPT1: core done + 25 tests green, bench pages + web test green, tools/optimi
  59efade6 ckpt 3253: FF1: alpha106 prepared (code 207, GeckoView 161): lab smoke GREEN on 161 (ru
```
