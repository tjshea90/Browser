# CHECKPOINT 3146 — read me first, then TASKS.md

**Written:** 2026-11-24T02:26:30Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `544223d2` (this checkpoint is the commit after it)

## Just done
PERF20 coded and green: TrimGate (core) + TrimGateContext (app, Robolectric-tested) + EngineTrimGate (Gecko: GeckoRuntime.create with the gating context) + Settings switch + Diagnostics line + digest lines (handoff 95) + OwnerRulesTest perf20; research/RAM_FOR_SPEED.md written; release note ALPHA101 written

## Do this next
bump geckoapp/build.gradle.kts to versionCode 202 / 0.3.0-alpha101, bash tools/sync-ext-version.sh, ship.sh gecko with the ALPHA101 note (background), ONE dispatch, poll the RUN, get_release_by_tag, record-release.sh with the identical note, answer the owner (PERF20) with the Release link and what was NOT run on the tablet; open: PERF17c/d fixes (wait for alpha100 numbers), PERF19 cheaper blocker, FLK1, PERF14e, PERF9, BackLab MISSED false positive, decoded-image expiry 3->10 min after the gate numbers

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
    ?? docs/handoff/ALPHA101_RELEASE_NOTE.txt

## Last ten checkpoints
```
  f082d388 ckpt 3138: owner request PERF20 (research how the browser can be as fast as possible us
  2b1119f3 ckpt 3137: TASKS.md closed for PERF17e/PERF18, PERF17 left open for the c/d fixes (clai
  7a224ab8 ckpt 3135: alpha100 SHIPPED and recorded: run 45303459458 green on attempt 1 (check, te
  654a7ecd ckpt 3133: alpha100 (code 201) gated, pushed (ckpt 3132) and dispatched ONCE: run 45303
  4b69f1eb ckpt 3132: gated kestrel-g-v0.3.0-alpha100 (gecko code 201) and pushed it: handoff self
  39ac98cc ckpt 3130: PERF17/PERF18 coded and green: lazy restore after 4 h (core LazyStart, Home 
  2d2398f7 ckpt 3120: PERF17b lazy restore CODED and compiled (core LazyStart + LazyStartTest gree
  0cf14251 ckpt 3113: owner request PERF18 (research online what major browsers do for fast loadin
  9dcb9f9f ckpt 3111: owner request PERF17 written into TASKS.md in his words (a-e): faster speed,
  77f06ebc ckpt 3110: alpha99 SHIPPED and recorded: run 45292001575 green on attempt 1 (check, tes
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
