# CHECKPOINT 2703 — read me first, then TASKS.md

**Written:** 2026-11-19T02:05:24Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kestrel-browser-optimization-h6y8u2` · **builds on:** `434b5ea7` (this checkpoint is the commit after it)

## Just done
alpha71 gates: first ship.sh run stopped on two documented timing-flaky download-engine tests (ServerTest.manyClientsAtOnce, EngineTest.anOutageInTheMiddle; both pass alone, rc=0, and in the earlier full run), ship.sh rerun in progress. Lab dr v2 run 44439418098 running; compat run (EM1f/EM1h) queued behind it.

## Do this next
When ship.sh finishes rc=0 (log in scratchpad ship.log): dispatch android.yml app=gecko full_build=true note=docs/handoff/ALPHA71_RELEASE_NOTE.txt text ONCE, verify tag kestrel-g-v0.3.0-alpha71 (git ls-remote), list jobs, get_release_by_tag, bash tools/record-release.sh kestrel-g-v0.3.0-alpha71 with identical note. Then read dr v2 (44439418098) and compat -> research/EM1_RESULTS.md. Heartbeat trig_018ZFUWQpV3dBUPXUvKDiXwQ (delete when EM1 ships).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  434b5ea7 ckpt 2702: alpha71 PREPARED: versionCode 172 / 0.3.0-alpha71, extension manifest 1.0.17
  ab9af2b4 ckpt 2701: dr scene: Vidfast also tried with press-to-play OFF (tells the rule from the
  378e887d ckpt 2700: Lab run 44435517079 (em) READ: reopen 3/3 drew the picture again; dr INCONCL
  1476784b ckpt 2699: EM1i naming: the REAL rendered doraby title page (headless Chromium) gives O
  abb85ece ckpt 2698: Lab run 44434241145 (suite ro, commit 609d22a7) READ: the 3 reopens that ran
  0853cfea ckpt 2697: EM1k step 2 CODED + tests green + Gecko module compiles: a page that painted
  7c63b314 ckpt 2696: EM1a baseline READ (daily run 44432470135 green: security 17/0, 68 pages pai
  69bb5648 ckpt 2695: EM1i research: real doraby chain measured from the sandbox (6 servers, resea
  609d22a7 ckpt 2694: EM1k step 1 CODED + compiled: core LastLookRules.whyNot (+test, green) names
  54c9c34f ckpt 2693: EM1a: baseline lab DISPATCHED 2026-11-19 00:52Z: android.yml run 44432470135
```
