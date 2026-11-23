# CHECKPOINT 3085 — read me first, then TASKS.md

**Written:** 2026-11-23T21:14:24Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `30d5ae86` (this checkpoint is the commit after it)

## Just done
PERF15 coded (not gated): LiveBudget.afterTrim level 60+ follows free memory (the cause of reloads on tab switch: Android sends 80 within a second of every app leave, old rule closed all but one tab), tabs switch-done line + digest TABS section, research/TAB_SWITCH.md

## Do this next
Next: run targeted tests (core LiveBudgetTest OwnerRulesTest; geckoapp compile), bump 0.3.0-alpha98 code 199 + sync-ext-version, ship.sh gecko (note ASCII no double quotes in scratchpad note98.txt), ONE dispatch, poll the RUN every 5 min, rerun_failed_jobs on the same run for known runner flakes (PhoneBackendTest, ServerTest, StreamEngineTest: FLK1), record-release, answer owner (Back faster and photos attach CONFIRMED; tab switching fix; ask: does it still reload; Send now)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M core/src/test/kotlin/app/kestrel/core/OwnerRulesTest.kt
    ?? research/TAB_SWITCH.md

## Last ten checkpoints
```
  adeb2192 ckpt 3082: owner after alpha97: Back is faster and images attach (CONFIRMED); NEW PERF1
  8a9c210e ckpt 3081: alpha97 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha97 (gecko code 198, run
  a7a096c8 ckpt 3079: alpha97 run 45259809870 FAILED at test-jvm on the GitHub runner: core Stream
  d47330ac ckpt 3078: alpha97 (gecko code 198) gated by ship.sh (core + app + geckoapp suites, han
  80d592db ckpt 3077: gated kestrel-g-v0.3.0-alpha97 (gecko code 198) and pushed it: handoff self-
  fc06903d ckpt 3074: PERF14 coded (not gated): photo attach copy (core PickStore + tests, Browser
  75f112b7 ckpt 3059: PERF14a research written: photo attach root cause (GeckoView 160 FilePrompt.
  678153fe ckpt 3057: PERF14 written to TASKS.md (owner after alpha96: Back still slow although th
  d8a9f0e7 ckpt 3056: alpha96 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha96 (gecko code 197, run
  ca317a33 ckpt 3054: alpha96 run 45245013667 FAILED at test-jvm on the GitHub runner: two timing 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
