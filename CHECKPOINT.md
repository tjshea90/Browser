# CHECKPOINT 3089 — read me first, then TASKS.md

**Written:** 2026-11-23T21:31:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `665bc7a9` (this checkpoint is the commit after it)

## Just done
alpha98 (gecko code 199) gated by ship.sh and ONE dispatch made on claude/kind-brahmagupta-ae3yl8 (android.yml app=gecko full_build=true); contents: PERF15 tab switch (LiveBudget.afterTrim follows free memory, tabs switch-done line, digest TABS)

## Do this next
Next: poll the RUN (mcp__github__actions_list list_workflow_runs android.yml branch claude/kind-brahmagupta-ae3yl8) every ~5 min; failure: get_job_logs failed_only; known runner flakes (core PhoneBackendTest, courier ServerTest, core StreamEngineTest) -> rerun_failed_jobs on the SAME run; green: get_release_by_tag kestrel-g-v0.3.0-alpha98, bash tools/record-release.sh kestrel-g-v0.3.0-alpha98 with the identical note (scratchpad note98.txt), tick PERF15b [~], answer the owner. NEVER dispatch alpha98 twice.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  665bc7a9 ckpt 3088: gated kestrel-g-v0.3.0-alpha98 (gecko code 199) and pushed it: handoff self-
  6b92a4b3 ckpt 3087: pre-ship: alpha98: switching between tabs no longer reloads them. Your diagn
  a5e04498 ckpt 3085: PERF15 coded (not gated): LiveBudget.afterTrim level 60+ follows free memory
  adeb2192 ckpt 3082: owner after alpha97: Back is faster and images attach (CONFIRMED); NEW PERF1
  8a9c210e ckpt 3081: alpha97 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha97 (gecko code 198, run
  a7a096c8 ckpt 3079: alpha97 run 45259809870 FAILED at test-jvm on the GitHub runner: core Stream
  d47330ac ckpt 3078: alpha97 (gecko code 198) gated by ship.sh (core + app + geckoapp suites, han
  80d592db ckpt 3077: gated kestrel-g-v0.3.0-alpha97 (gecko code 198) and pushed it: handoff self-
  fc06903d ckpt 3074: PERF14 coded (not gated): photo attach copy (core PickStore + tests, Browser
  75f112b7 ckpt 3059: PERF14a research written: photo attach root cause (GeckoView 160 FilePrompt.
```
