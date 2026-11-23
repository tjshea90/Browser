# CHECKPOINT 3074 — read me first, then TASKS.md

**Written:** 2026-11-23T20:11:18Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `e84c21c5` (this checkpoint is the commit after it)

## Just done
PERF14 coded (not gated): photo attach copy (core PickStore + tests, BrowserActivity.copyPick, permission + Settings row removed), Back stop-on-leave (content.js click listener, bkm, st, +stop, leavestop.test.js), Calmer redrawing switch, digest SETTING A/B, research files ATTACH_ROOT_CAUSE / PAGE_SPEED / INSTANT_BACK section 5 / PAINT_GLITCH result

## Do this next
Next: run targeted tests (core PickStoreTest BackLabTest OwnerRulesTest DiagStats*, app PrivateTabsTest PageTimesTest), web npm tests for leavestop/back/background/pagetime, test_handoff; bump versionCode 198 / 0.3.0-alpha97 + sync-ext-version; ship.sh gecko (note ASCII no double quotes, in scratchpad note97.txt); ONE dispatch; if test-jvm fails on the known flakes (PhoneBackendTest, ServerTest) rerun_failed_jobs on the same run; record-release; answer the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/PAINT_GLITCH.md
     M tools/diag_digest.py

## Last ten checkpoints
```
  75f112b7 ckpt 3059: PERF14a research written: photo attach root cause (GeckoView 160 FilePrompt.
  678153fe ckpt 3057: PERF14 written to TASKS.md (owner after alpha96: Back still slow although th
  d8a9f0e7 ckpt 3056: alpha96 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha96 (gecko code 197, run
  ca317a33 ckpt 3054: alpha96 run 45245013667 FAILED at test-jvm on the GitHub runner: two timing 
  48e2535b ckpt 3053: alpha96 (gecko code 197) gated by ship.sh (core + app + geckoapp suites, han
  09eaca29 ckpt 3052: gated kestrel-g-v0.3.0-alpha96 (gecko code 197) and pushed it: handoff self-
  bd5370ef ckpt 3049: PERF12 + PERF13 coded and tested (targeted): cut-short loads, page loop, qui
  fab351e4 ckpt 3039: PERF13 written to TASKS.md (owner: instant first load and instant Back, find
  d41227d4 ckpt 3034: PERF12 written to TASKS.md (owner 2026-11-23: look at the latest diagnostics
  b375e29e ckpt 3033: PERF11a done + research written (alpha95 uploads: Safe drawing + PBO off app
```

(14 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
