# CHECKPOINT 3054 — read me first, then TASKS.md

**Written:** 2026-11-23T19:32:02Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `48e2535b` (this checkpoint is the commit after it)

## Just done
alpha96 run 45245013667 FAILED at test-jvm on the GitHub runner: two timing tests (core PhoneBackendTest aPhoneThatIsKilledInTheMiddleOfAVideoFinishesItAfterTheRestart and courier ServerTest manyClientsAtOnceGetTheirOwnAnswers, 12 clients with 20 s joins), both green in the local full gate, neither touches the code of alpha96; no tag, no Release, build-gecko skipped; rerun_failed_jobs queued on the SAME run (precedent alpha79), not a new dispatch

## Do this next
Next: the tag watcher (background) fires when kestrel-g-v0.3.0-alpha96 exists; check mcp__github__actions_list list_workflow_jobs 45245013667: if test-jvm fails AGAIN read get_job_logs job for the failing tests and harden them (longer awaits / join timeouts) in their own commit, then rerun failed jobs (still no new dispatch unless the run is gone); if green: get_release_by_tag, bash tools/record-release.sh kestrel-g-v0.3.0-alpha96 with the identical note (scratchpad note96.txt), answer the owner with the Release link.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  48e2535b ckpt 3053: alpha96 (gecko code 197) gated by ship.sh (core + app + geckoapp suites, han
  09eaca29 ckpt 3052: gated kestrel-g-v0.3.0-alpha96 (gecko code 197) and pushed it: handoff self-
  bd5370ef ckpt 3049: PERF12 + PERF13 coded and tested (targeted): cut-short loads, page loop, qui
  fab351e4 ckpt 3039: PERF13 written to TASKS.md (owner: instant first load and instant Back, find
  d41227d4 ckpt 3034: PERF12 written to TASKS.md (owner 2026-11-23: look at the latest diagnostics
  b375e29e ckpt 3033: PERF11a done + research written (alpha95 uploads: Safe drawing + PBO off app
  489d966d ckpt 3032: PERF11 written to TASKS.md (owner after alpha95: invisible blocks covering w
  eefcc152 ckpt 3031: alpha95 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha95 (gecko code 196, run
  f107edb6 ckpt 3029: alpha95 (gecko code 196) gated by ship.sh (core+app+geckoapp suites, handoff
  12cc7bbd ckpt 3028: gated kestrel-g-v0.3.0-alpha95 (gecko code 196) and pushed it: handoff self-
```
