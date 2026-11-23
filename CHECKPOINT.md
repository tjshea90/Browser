# CHECKPOINT 3078 — read me first, then TASKS.md

**Written:** 2026-11-23T20:29:20Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `80d592db` (this checkpoint is the commit after it)

## Just done
alpha97 (gecko code 198) gated by ship.sh (core + app + geckoapp suites, handoff 92, web tests) and ONE dispatch made on claude/kind-brahmagupta-ae3yl8 (android.yml app=gecko full_build=true); contents: PERF14 photo attach copy (root cause), Back stop-on-leave, Calmer redrawing switch, digest SETTING A/B, permission + Settings row removed

## Do this next
Next: poll the RUN not only the tag: mcp__github__actions_list list_workflow_runs android.yml branch claude/kind-brahmagupta-ae3yl8 (status / conclusion) every ~5 min; if conclusion failure read mcp__github__get_job_logs for the failed job: known timing flakes (core PhoneBackendTest, courier ServerTest) -> mcp__github__actions_run_trigger rerun_failed_jobs on the SAME run (no new dispatch); real failure -> fix and ship alpha98. When green: get_release_by_tag kestrel-g-v0.3.0-alpha97, bash tools/record-release.sh kestrel-g-v0.3.0-alpha97 with the identical note (scratchpad note97.txt), tick PERF14b/c/e [~] with the run id, answer the owner with the Release link on its own line. NEVER dispatch alpha97 twice.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  80d592db ckpt 3077: gated kestrel-g-v0.3.0-alpha97 (gecko code 198) and pushed it: handoff self-
  fc06903d ckpt 3074: PERF14 coded (not gated): photo attach copy (core PickStore + tests, Browser
  75f112b7 ckpt 3059: PERF14a research written: photo attach root cause (GeckoView 160 FilePrompt.
  678153fe ckpt 3057: PERF14 written to TASKS.md (owner after alpha96: Back still slow although th
  d8a9f0e7 ckpt 3056: alpha96 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha96 (gecko code 197, run
  ca317a33 ckpt 3054: alpha96 run 45245013667 FAILED at test-jvm on the GitHub runner: two timing 
  48e2535b ckpt 3053: alpha96 (gecko code 197) gated by ship.sh (core + app + geckoapp suites, han
  09eaca29 ckpt 3052: gated kestrel-g-v0.3.0-alpha96 (gecko code 197) and pushed it: handoff self-
  bd5370ef ckpt 3049: PERF12 + PERF13 coded and tested (targeted): cut-short loads, page loop, qui
  fab351e4 ckpt 3039: PERF13 written to TASKS.md (owner: instant first load and instant Back, find
```
