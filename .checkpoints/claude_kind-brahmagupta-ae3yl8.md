# CHECKPOINT 3104 — read me first, then TASKS.md

**Written:** 2026-11-23T23:46:38Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `cea425be` (this checkpoint is the commit after it)

## Just done
PERF16 a-d done: focus_report, sec lines, journal bookkeeping thinning, advisor cert-errors, research/DIAG_FOCUS.md; core tests + :geckoapp:compileDebugKotlin green; OwnerRulesTest.kt had been emptied by an accident at 21:13 (committed empty in a5e04498) and is restored from 8bea6b3a (all core tests pass)

## Do this next
Run :app:testDebugUnitTest (PrivateTabsTest etc.), bump geckoapp/build.gradle.kts to versionCode 200 / 0.3.0-alpha99, bash tools/sync-ext-version.sh, bash ship.sh gecko with note (docs/handoff/ALPHA99_RELEASE_NOTE.txt), ONE dispatch app=gecko full_build=true, poll the RUN, record-release.sh, answer the owner (focus report + sec lines), re-list open items (FLK1, PERF14e, PERF9, MISSED false positive in BackLab)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  a6a2207d ckpt 3092: owner after alpha98: it seems faster; NEW PERF16: focus future diagnostics o
  c5e4207f ckpt 3091: alpha98 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha98 (gecko code 199, run
  946e2e68 ckpt 3089: alpha98 (gecko code 199) gated by ship.sh and ONE dispatch made on claude/ki
  665bc7a9 ckpt 3088: gated kestrel-g-v0.3.0-alpha98 (gecko code 199) and pushed it: handoff self-
  6b92a4b3 ckpt 3087: pre-ship: alpha98: switching between tabs no longer reloads them. Your diagn
  a5e04498 ckpt 3085: PERF15 coded (not gated): LiveBudget.afterTrim level 60+ follows free memory
  adeb2192 ckpt 3082: owner after alpha97: Back is faster and images attach (CONFIRMED); NEW PERF1
  8a9c210e ckpt 3081: alpha97 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha97 (gecko code 198, run
  a7a096c8 ckpt 3079: alpha97 run 45259809870 FAILED at test-jvm on the GitHub runner: core Stream
  d47330ac ckpt 3078: alpha97 (gecko code 198) gated by ship.sh (core + app + geckoapp suites, han
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
