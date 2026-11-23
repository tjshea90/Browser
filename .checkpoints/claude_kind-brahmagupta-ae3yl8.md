# CHECKPOINT 3108 — read me first, then TASKS.md

**Written:** 2026-11-23T23:49:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `fad5637b` (this checkpoint is the commit after it)

## Just done
alpha99 run is 45292001575 (workflow_dispatch, started 23:48Z, head da26da05), in progress

## Do this next
Poll RUN 45292001575 (mcp__github__actions_list list_workflow_jobs resource_id 45292001575 / list_workflow_runs) until completed; on a flaky timing test failure rerun_failed_jobs run_id 45292001575; when green get_release_by_tag kestrel-g-v0.3.0-alpha99, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha99 with the identical note from docs/handoff/ALPHA99_RELEASE_NOTE.txt, then answer the owner with the Release link; NEVER dispatch again

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  fad5637b ckpt 3107: alpha99 (code 200) gated and pushed (ckpt 3106); workflow dispatched ONCE on
  da26da05 ckpt 3106: gated kestrel-g-v0.3.0-alpha99 (gecko code 200) and pushed it: handoff self-
  feb56817 ckpt 3104: PERF16 a-d done: focus_report, sec lines, journal bookkeeping thinning, advi
  a6a2207d ckpt 3092: owner after alpha98: it seems faster; NEW PERF16: focus future diagnostics o
  c5e4207f ckpt 3091: alpha98 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha98 (gecko code 199, run
  946e2e68 ckpt 3089: alpha98 (gecko code 199) gated by ship.sh and ONE dispatch made on claude/ki
  665bc7a9 ckpt 3088: gated kestrel-g-v0.3.0-alpha98 (gecko code 199) and pushed it: handoff self-
  6b92a4b3 ckpt 3087: pre-ship: alpha98: switching between tabs no longer reloads them. Your diagn
  a5e04498 ckpt 3085: PERF15 coded (not gated): LiveBudget.afterTrim level 60+ follows free memory
  adeb2192 ckpt 3082: owner after alpha97: Back is faster and images attach (CONFIRMED); NEW PERF1
```
