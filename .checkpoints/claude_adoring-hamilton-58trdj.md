# CHECKPOINT 2754 — read me first, then TASKS.md

**Written:** 2026-11-20T18:14:00Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/adoring-hamilton-58trdj` · **builds on:** `ae4e6a11` (this checkpoint is the commit after it)

## Just done
NL1 PROVEN on the real engine: lab run 44787922316 (daily, 62 scenes, 12 min, 0 crashes): Health 'sitefixes running v=160.4.0 took=181ms', Diagnostics line shows it, no script errors, memory peak 298 vs 303 MB baseline, page paint/done unchanged. Written to research NL1 proof + TASKS.

## Do this next
JVM suite + lint running in background (log scratchpad/suite.log, wait for EXIT); then cd web-tests && npm test; then bump geckoapp versionCode 176 / 0.3.0-alpha75, bash tools/sync-ext-version.sh, write docs/handoff/ALPHA75_RELEASE_NOTE.txt (ASCII, no double quotes or backslashes), bash ship.sh gecko with it, ONE dispatch (app=gecko full_build=true), record-release; consider one more lever first only if cheap

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/REVIEW_2026-11-22_NEWLEVERS.md

## Last ten checkpoints
```
  ae4e6a11 ckpt 2753: RVW4: NL2 done: realfinder.js (real pages through the real background.js + c
  d637614b ckpt 2752: NL1: licence row (docs/FILTER-LISTS.md) + HANDOFF engine step written; emula
  85af50a4 ckpt 2751: RVW4-NL1 code done and compiling: Gradle prepareWebcompat (AAR from Mozilla 
  934b8dad ckpt 2750: RVW4a: researched. Engine prefs / downloads / finder are saturated (all chea
  8d40d448 ckpt 2749: RVW4 written into TASKS.md in the owner's words (new levers for speed, compa
  aeef6973 ckpt 2748: RVW3 DONE AND SHIPPED: alpha74 RELEASED AND RECORDED = kestrel-g-v0.3.0-alph
  a1da4068 ckpt 2747: REL74b: alpha74 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  4c5ff5b3 ckpt 2746: gated kestrel-g-v0.3.0-alpha74 (gecko code 175) and pushed it: handoff self-
  6ab74f37 ckpt 2745: RVW3 fixes CODED + compile green (:geckoapp:compileDebugKotlin EXIT 0): secu
  7496a10a ckpt 2744: RVW3: security lane back and VERIFIED by me against the code (SC1 SC2 SC4 SC
```
