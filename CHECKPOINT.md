# CHECKPOINT 3079 — read me first, then TASKS.md

**Written:** 2026-11-23T20:44:04Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `d47330ac` (this checkpoint is the commit after it)

## Just done
alpha97 run 45259809870 FAILED at test-jvm on the GitHub runner: core StreamEngineTest refusalsThatStartHalfWayAreAskedAgainBeforeTheLadderIsTried (a race of 8 part workers on a shared counter; StreamEngine unchanged since 11-20; green locally); rerun_failed_jobs queued on the SAME run; FLK1 written to TASKS (three runner flakes in two releases)

## Do this next
Next: poll the run (mcp__github__actions_list list_workflow_jobs 45245... no: run id 45259809870, filter latest) every ~5 min; when green: get_release_by_tag kestrel-g-v0.3.0-alpha97, bash tools/record-release.sh kestrel-g-v0.3.0-alpha97 with the identical note (scratchpad note97.txt), tick PERF14b/c/e [~], answer the owner with the Release link; then FLK1 (harden the three flaky tests in their own commit, no new release needed unless wanted)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  d47330ac ckpt 3078: alpha97 (gecko code 198) gated by ship.sh (core + app + geckoapp suites, han
  80d592db ckpt 3077: gated kestrel-g-v0.3.0-alpha97 (gecko code 198) and pushed it: handoff self-
  fc06903d ckpt 3074: PERF14 coded (not gated): photo attach copy (core PickStore + tests, Browser
  75f112b7 ckpt 3059: PERF14a research written: photo attach root cause (GeckoView 160 FilePrompt.
  678153fe ckpt 3057: PERF14 written to TASKS.md (owner after alpha96: Back still slow although th
  d8a9f0e7 ckpt 3056: alpha96 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha96 (gecko code 197, run
  ca317a33 ckpt 3054: alpha96 run 45245013667 FAILED at test-jvm on the GitHub runner: two timing 
  48e2535b ckpt 3053: alpha96 (gecko code 197) gated by ship.sh (core + app + geckoapp suites, han
  09eaca29 ckpt 3052: gated kestrel-g-v0.3.0-alpha96 (gecko code 197) and pushed it: handoff self-
  bd5370ef ckpt 3049: PERF12 + PERF13 coded and tested (targeted): cut-short loads, page loop, qui
```
