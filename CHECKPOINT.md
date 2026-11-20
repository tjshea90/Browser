# CHECKPOINT 2755 — read me first, then TASKS.md

**Written:** 2026-11-20T18:24:25Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/adoring-hamilton-58trdj` · **builds on:** `393b877c` (this checkpoint is the commit after it)

## Just done
RVW4: whole JVM suite GREEN (:core:test 1907 tests 0 failures 12 skipped as before, :app:testDebugUnitTest 450 0 failures, :app:lintDebug EXIT 0), SiteFixes.settingsChanged only passes a real change of the switch, SiteFixesRulesTest 5/5 + :geckoapp:compileDebugKotlin EXIT 0 after the last edit; docs/handoff/ALPHA75_RELEASE_NOTE.txt written (ASCII, no quotes). web-tests npm test running in background (scratchpad/npm.log, wait for EXIT).

## Do this next
when npm test is green (expect about 650 tests): bump geckoapp/build.gradle.kts versionCode 176 / versionName 0.3.0-alpha75, bash tools/sync-ext-version.sh, bash ship.sh gecko "$(cat docs/handoff/ALPHA75_RELEASE_NOTE.txt)", then ONE dispatch mcp__github__actions_run_trigger run_workflow android.yml ref claude/adoring-hamilton-58trdj inputs app=gecko full_build=true note=<same text>, wait for tag kestrel-g-v0.3.0-alpha75 + jobs, record-release.sh with the IDENTICAL note, give the owner the Release link

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b1ec2fd4 ckpt 2754: NL1 PROVEN on the real engine: lab run 44787922316 (daily, 62 scenes, 12 min
  ae4e6a11 ckpt 2753: RVW4: NL2 done: realfinder.js (real pages through the real background.js + c
  d637614b ckpt 2752: NL1: licence row (docs/FILTER-LISTS.md) + HANDOFF engine step written; emula
  85af50a4 ckpt 2751: RVW4-NL1 code done and compiling: Gradle prepareWebcompat (AAR from Mozilla 
  934b8dad ckpt 2750: RVW4a: researched. Engine prefs / downloads / finder are saturated (all chea
  8d40d448 ckpt 2749: RVW4 written into TASKS.md in the owner's words (new levers for speed, compa
  aeef6973 ckpt 2748: RVW3 DONE AND SHIPPED: alpha74 RELEASED AND RECORDED = kestrel-g-v0.3.0-alph
  a1da4068 ckpt 2747: REL74b: alpha74 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  4c5ff5b3 ckpt 2746: gated kestrel-g-v0.3.0-alpha74 (gecko code 175) and pushed it: handoff self-
  6ab74f37 ckpt 2745: RVW3 fixes CODED + compile green (:geckoapp:compileDebugKotlin EXIT 0): secu
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
