# CHECKPOINT 3130 — read me first, then TASKS.md

**Written:** 2026-11-24T01:12:08Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `128fddda` (this checkpoint is the commit after it)

## Just done
PERF17/PERF18 coded and green: lazy restore after 4 h (core LazyStart, Home Continue line, Settings switch), frame cadence fm= and bot-check ch=/chs= in pagetime.js -> background.js -> PageTimingLine -> digest (PAGE ANIMATION, bot checks), web tests 11 pagetime + background pass, handoff self-test 94, :core:test + both compiles green; research/LAZY_RESTORE.md, BROWSER_TECHNIQUES.md; PERF19 (cheaper blocker) written; docs/handoff/ALPHA100_RELEASE_NOTE.txt written

## Do this next
bump geckoapp/build.gradle.kts to versionCode 201 / 0.3.0-alpha100, bash tools/sync-ext-version.sh, ship.sh gecko with the ALPHA100 note, ONE dispatch (inputs app=gecko full_build=true note), poll the RUN until completed (flaky PhoneBackendTest/ServerTest/StreamEngineTest -> rerun_failed_jobs on the SAME run), get_release_by_tag kestrel-g-v0.3.0-alpha100, record-release.sh with the identical note, then answer BOTH owner messages (PERF17 and PERF18) and re-list open items (FLK1, PERF14e, PERF9, BackLab MISSED false positive, PERF19)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2d2398f7 ckpt 3120: PERF17b lazy restore CODED and compiled (core LazyStart + LazyStartTest gree
  0cf14251 ckpt 3113: owner request PERF18 (research online what major browsers do for fast loadin
  9dcb9f9f ckpt 3111: owner request PERF17 written into TASKS.md in his words (a-e): faster speed,
  77f06ebc ckpt 3110: alpha99 SHIPPED and recorded: run 45292001575 green on attempt 1 (check, tes
  5ae527e4 ckpt 3108: alpha99 run is 45292001575 (workflow_dispatch, started 23:48Z, head da26da05
  fad5637b ckpt 3107: alpha99 (code 200) gated and pushed (ckpt 3106); workflow dispatched ONCE on
  da26da05 ckpt 3106: gated kestrel-g-v0.3.0-alpha99 (gecko code 200) and pushed it: handoff self-
  feb56817 ckpt 3104: PERF16 a-d done: focus_report, sec lines, journal bookkeeping thinning, advi
  a6a2207d ckpt 3092: owner after alpha98: it seems faster; NEW PERF16: focus future diagnostics o
  c5e4207f ckpt 3091: alpha98 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha98 (gecko code 199, run
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
