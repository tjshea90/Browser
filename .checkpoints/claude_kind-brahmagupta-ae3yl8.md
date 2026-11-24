# CHECKPOINT 3152 — read me first, then TASKS.md

**Written:** 2026-11-24T02:45:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `aa4e4bfa` (this checkpoint is the commit after it)

## Just done
alpha101 SHIPPED and recorded: run 45312620908 green on attempt 1 (check, test-js, test-jvm, build-gecko), Release has kestrel-g-0.3.0-alpha101.apk 96.7 MB, certificate verified, BUILDLOG.md updated; PERF20 closed

## Do this next
Waiting for the owner's tablet test of alpha101 and a new upload: python3 -B -I tools/diag_digest.py --days 2, FOCUS REPORT section 1: the 'memory signals' and 'the gate' lines (held vs passed on, engine kills not more than 2 per 14 days, free memory at page loads), the Diagnostics line 'gate registered on: org.mozilla.gecko.process.MemoryController' proves the wrapper context is in place; section 2 PAGE ANIMATION lines and section 4 bot checks lines (alpha100 data) and tabs lazy-start/lazy-wake. Then in order: PERF17c/d fixes from those numbers, PERF19 cheaper blocker (benchmark in web-tests first), decoded-image expiry 3 to 10 min if the gate holds, FLK1, PERF14e, PERF9, BackLab MISSED false positive. alpha101 is shipped: next is alpha102 / versionCode 203

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  1e465904 ckpt 3150: alpha101 (code 202) gated (ckpt 3148, head 94c20d92 + the corrected note fil
  94c20d92 ckpt 3148: gated kestrel-g-v0.3.0-alpha101 (gecko code 202) and pushed it: handoff self
  9d6056a5 ckpt 3146: PERF20 coded and green: TrimGate (core) + TrimGateContext (app, Robolectric-
  f082d388 ckpt 3138: owner request PERF20 (research how the browser can be as fast as possible us
  2b1119f3 ckpt 3137: TASKS.md closed for PERF17e/PERF18, PERF17 left open for the c/d fixes (clai
  7a224ab8 ckpt 3135: alpha100 SHIPPED and recorded: run 45303459458 green on attempt 1 (check, te
  654a7ecd ckpt 3133: alpha100 (code 201) gated, pushed (ckpt 3132) and dispatched ONCE: run 45303
  4b69f1eb ckpt 3132: gated kestrel-g-v0.3.0-alpha100 (gecko code 201) and pushed it: handoff self
  39ac98cc ckpt 3130: PERF17/PERF18 coded and green: lazy restore after 4 h (core LazyStart, Home 
  2d2398f7 ckpt 3120: PERF17b lazy restore CODED and compiled (core LazyStart + LazyStartTest gree
```
