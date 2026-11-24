# CHECKPOINT 3138 — read me first, then TASKS.md

**Written:** 2026-11-24T02:06:37Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `2b1119f3` (this checkpoint is the commit after it)

## Just done
owner request PERF20 (research how the browser can be as fast as possible using as much RAM as possible within reason for a low-RAM tablet; implement) written into TASKS.md in his words (a-d)

## Do this next
PERF20a: write a throwaway analysis in the scratchpad over python3 tools/diag_digest.py loaders (load_git): free memory (device avail/MemAvailable), engine kills, tabs live vs page lag (page timing lines carry live= and lag=), cpuP/cpuT; list the RAM-for-speed prefs in geckoapp/src/main/assets/gecko-prefs.txt + GeckoPrefs.kt + LiveBudget + BrowserActivity (Back pictures, thumbs, last look); read ProcessLife.maybeExit (the process exits 2 s after the activity is destroyed: a warm engine thrown away); then PERF20b web searches (parallel), write research/RAM_FOR_SPEED.md, then PERF20c

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  2b1119f3 ckpt 3137: TASKS.md closed for PERF17e/PERF18, PERF17 left open for the c/d fixes (clai
  7a224ab8 ckpt 3135: alpha100 SHIPPED and recorded: run 45303459458 green on attempt 1 (check, te
  654a7ecd ckpt 3133: alpha100 (code 201) gated, pushed (ckpt 3132) and dispatched ONCE: run 45303
  4b69f1eb ckpt 3132: gated kestrel-g-v0.3.0-alpha100 (gecko code 201) and pushed it: handoff self
  39ac98cc ckpt 3130: PERF17/PERF18 coded and green: lazy restore after 4 h (core LazyStart, Home 
  2d2398f7 ckpt 3120: PERF17b lazy restore CODED and compiled (core LazyStart + LazyStartTest gree
  0cf14251 ckpt 3113: owner request PERF18 (research online what major browsers do for fast loadin
  9dcb9f9f ckpt 3111: owner request PERF17 written into TASKS.md in his words (a-e): faster speed,
  77f06ebc ckpt 3110: alpha99 SHIPPED and recorded: run 45292001575 green on attempt 1 (check, tes
  5ae527e4 ckpt 3108: alpha99 run is 45292001575 (workflow_dispatch, started 23:48Z, head da26da05
```
