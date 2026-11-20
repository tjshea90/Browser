# CHECKPOINT 2757 — read me first, then TASKS.md

**Written:** 2026-11-20T18:40:25Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/adoring-hamilton-58trdj` · **builds on:** `eebefd5f` (this checkpoint is the commit after it)

## Just done
REL75b: alpha75 DISPATCHED ONCE (android.yml app=gecko full_build=true ref claude/adoring-hamilton-58trdj, note = docs/handoff/ALPHA75_RELEASE_NOTE.txt, gecko code 176) at about 18:41Z. DO NOT DISPATCH AGAIN. Gates before it: handoff self-test, versionCode 176 > 175, :core:test :app:testDebugUnitTest compile green, web npm test 649/649, lint clean, emulator lab run 44787922316 green with the site fixes running.

## Do this next
wait for the run (find its id: mcp__github__actions_list list_workflow_runs android.yml perPage 1; NEVER perPage>1: huge output), then list_workflow_jobs perPage 8: all jobs success; git ls-remote --tags origin | grep alpha75; mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha75 shows the APK; then bash tools/record-release.sh kestrel-g-v0.3.0-alpha75 "$(cat docs/handoff/ALPHA75_RELEASE_NOTE.txt)" (IDENTICAL note); tick RVW4 rows in TASKS.md; answer the owner with the Release link on its own line + what is still open

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  eebefd5f ckpt 2756: gated kestrel-g-v0.3.0-alpha75 (gecko code 176) and pushed it: handoff self-
  9e99464e ckpt 2755: RVW4: whole JVM suite GREEN (:core:test 1907 tests 0 failures 12 skipped as 
  b1ec2fd4 ckpt 2754: NL1 PROVEN on the real engine: lab run 44787922316 (daily, 62 scenes, 12 min
  ae4e6a11 ckpt 2753: RVW4: NL2 done: realfinder.js (real pages through the real background.js + c
  d637614b ckpt 2752: NL1: licence row (docs/FILTER-LISTS.md) + HANDOFF engine step written; emula
  85af50a4 ckpt 2751: RVW4-NL1 code done and compiling: Gradle prepareWebcompat (AAR from Mozilla 
  934b8dad ckpt 2750: RVW4a: researched. Engine prefs / downloads / finder are saturated (all chea
  8d40d448 ckpt 2749: RVW4 written into TASKS.md in the owner's words (new levers for speed, compa
  aeef6973 ckpt 2748: RVW3 DONE AND SHIPPED: alpha74 RELEASED AND RECORDED = kestrel-g-v0.3.0-alph
  a1da4068 ckpt 2747: REL74b: alpha74 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
```
