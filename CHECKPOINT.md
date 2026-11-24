# CHECKPOINT 3120 — read me first, then TASKS.md

**Written:** 2026-11-24T00:55:56Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `f5da744a` (this checkpoint is the commit after it)

## Just done
PERF17b lazy restore CODED and compiled (core LazyStart + LazyStartTest green, Prefs.lazyStart/lastLeftAt, Tab.lazyStart, HomeView Continue row, BrowserActivity restore/earlyCover/applyUi/continueLazy/onPause stamp, Settings toggle, Diagnostics line); research/LAZY_RESTORE.md and research/BROWSER_TECHNIQUES.md written (PERF18a done)

## Do this next
(1) OwnerRulesTest guard perf17 (onPause stamps lastLeftAt, restore uses LazyStart.applies with launch/saved, earlyCover skips, applyUi clears, Settings toggle) ; (2) pagetime.js: frame cadence fm= (two 2.5 s rAF windows after load, top frame) and bot-check marker + 'cf' message (title/markers, ms at pagehide) -> background.js send -> ShieldsBridge when(cf) -> Health.log compat challenge host= ms=; web-tests/pagetime.test.js cases; digest: SPEED section median fm by lab_soft/lab_fps30 and PAGE COMPATIBILITY bot-check count + median; (3) PERF19 task for the cheaper blocker (decision cache) written in TASKS.md; (4) bump geckoapp to versionCode 201 / 0.3.0-alpha100, sync-ext-version, docs/handoff/ALPHA100_RELEASE_NOTE.txt, ship.sh gecko, ONE dispatch, poll the RUN, record-release, answer BOTH owner messages (PERF17 + PERF18) in plain words with what was NOT run on the tablet

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  0cf14251 ckpt 3113: owner request PERF18 (research online what major browsers do for fast loadin
  9dcb9f9f ckpt 3111: owner request PERF17 written into TASKS.md in his words (a-e): faster speed,
  77f06ebc ckpt 3110: alpha99 SHIPPED and recorded: run 45292001575 green on attempt 1 (check, tes
  5ae527e4 ckpt 3108: alpha99 run is 45292001575 (workflow_dispatch, started 23:48Z, head da26da05
  fad5637b ckpt 3107: alpha99 (code 200) gated and pushed (ckpt 3106); workflow dispatched ONCE on
  da26da05 ckpt 3106: gated kestrel-g-v0.3.0-alpha99 (gecko code 200) and pushed it: handoff self-
  feb56817 ckpt 3104: PERF16 a-d done: focus_report, sec lines, journal bookkeeping thinning, advi
  a6a2207d ckpt 3092: owner after alpha98: it seems faster; NEW PERF16: focus future diagnostics o
  c5e4207f ckpt 3091: alpha98 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha98 (gecko code 199, run
  946e2e68 ckpt 3089: alpha98 (gecko code 199) gated by ship.sh and ONE dispatch made on claude/ki
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
