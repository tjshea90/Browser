# CHECKPOINT 2705 — read me first, then TASKS.md

**Written:** 2026-11-19T02:17:16Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kestrel-browser-optimization-h6y8u2` · **builds on:** `f314d5ed` (this checkpoint is the commit after it)

## Just done
alpha71 DISPATCHED ONCE: android.yml app=gecko full_build=true on branch tip f314d5ed + (note = docs/handoff/ALPHA71_RELEASE_NOTE.txt incl. the keyboard-sync sentence). Do NOT dispatch again. dr v2 lab run 44439418098 running, compat queued behind it.

## Do this next
Find the gecko run id (actions_list list_workflow_runs android.yml perPage 1, event workflow_dispatch, filter branch; app=gecko run, not the lab runs), wait for all jobs green (check, test-js, test-jvm, build-gecko incl. certificate check + Publish), verify tag with git ls-remote --tags origin | grep alpha71 and get_release_by_tag kestrel-g-v0.3.0-alpha71, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha71 with the IDENTICAL note (cat docs/handoff/ALPHA71_RELEASE_NOTE.txt). Then read dr v2 + compat, tick EM1 items in TASKS.md, delete heartbeat trig_018ZFUWQpV3dBUPXUvKDiXwQ when shipped and results recorded.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f314d5ed ckpt 2704: gated kestrel-g-v0.3.0-alpha71 (gecko code 172) and pushed it: handoff self-
  c1463b83 ckpt 2703: alpha71 gates: first ship.sh run stopped on two documented timing-flaky down
  434b5ea7 ckpt 2702: alpha71 PREPARED: versionCode 172 / 0.3.0-alpha71, extension manifest 1.0.17
  ab9af2b4 ckpt 2701: dr scene: Vidfast also tried with press-to-play OFF (tells the rule from the
  378e887d ckpt 2700: Lab run 44435517079 (em) READ: reopen 3/3 drew the picture again; dr INCONCL
  1476784b ckpt 2699: EM1i naming: the REAL rendered doraby title page (headless Chromium) gives O
  abb85ece ckpt 2698: Lab run 44434241145 (suite ro, commit 609d22a7) READ: the 3 reopens that ran
  0853cfea ckpt 2697: EM1k step 2 CODED + tests green + Gecko module compiles: a page that painted
  7c63b314 ckpt 2696: EM1a baseline READ (daily run 44432470135 green: security 17/0, 68 pages pai
  69bb5648 ckpt 2695: EM1i research: real doraby chain measured from the sandbox (6 servers, resea
```
