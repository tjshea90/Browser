# CHECKPOINT 3150 — read me first, then TASKS.md

**Written:** 2026-11-24T02:28:47Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `3ac61077` (this checkpoint is the commit after it)

## Just done
alpha101 (code 202) gated (ckpt 3148, head 94c20d92 + the corrected note file) and dispatched ONCE: run 45312620908 (queued 02:28Z, head 3ac61077 = same code, note text corrected after the gate)

## Do this next
Poll RUN 45312620908 (mcp__github__actions_list list_workflow_jobs resource_id 45312620908) until completed; flaky timing test -> rerun_failed_jobs run_id 45312620908 (never a new dispatch); when green get_release_by_tag kestrel-g-v0.3.0-alpha101, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha101 with the identical note text from docs/handoff/ALPHA101_RELEASE_NOTE.txt, then answer the owner (PERF20) with the Release link, what was NOT run on the tablet, open items (PERF17c/d fixes wait for alpha100 numbers, PERF19 cheaper blocker, FLK1, PERF14e, PERF9, BackLab MISSED false positive, decoded-image expiry 3 to 10 min after the gate numbers)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  94c20d92 ckpt 3148: gated kestrel-g-v0.3.0-alpha101 (gecko code 202) and pushed it: handoff self
  9d6056a5 ckpt 3146: PERF20 coded and green: TrimGate (core) + TrimGateContext (app, Robolectric-
  f082d388 ckpt 3138: owner request PERF20 (research how the browser can be as fast as possible us
  2b1119f3 ckpt 3137: TASKS.md closed for PERF17e/PERF18, PERF17 left open for the c/d fixes (clai
  7a224ab8 ckpt 3135: alpha100 SHIPPED and recorded: run 45303459458 green on attempt 1 (check, te
  654a7ecd ckpt 3133: alpha100 (code 201) gated, pushed (ckpt 3132) and dispatched ONCE: run 45303
  4b69f1eb ckpt 3132: gated kestrel-g-v0.3.0-alpha100 (gecko code 201) and pushed it: handoff self
  39ac98cc ckpt 3130: PERF17/PERF18 coded and green: lazy restore after 4 h (core LazyStart, Home 
  2d2398f7 ckpt 3120: PERF17b lazy restore CODED and compiled (core LazyStart + LazyStartTest gree
  0cf14251 ckpt 3113: owner request PERF18 (research online what major browsers do for fast loadin
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
