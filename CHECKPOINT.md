# CHECKPOINT 2699 — read me first, then TASKS.md

**Written:** 2026-11-19T01:27:24Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kestrel-browser-optimization-h6y8u2` · **builds on:** `32740b2a` (this checkpoint is the commit after it)

## Just done
EM1i naming: the REAL rendered doraby title page (headless Chromium) gives Ozark S1E1 and, after clicking, S1E2 with names.js; the episode TITLE was empty because the real list has number and title in two spans with only spaces between: names.js now accepts a space-only title that starts with a letter and is not filler (of 10, season, years); web-tests names.test.js 9/9 with the real markup (episode list + season dropdown). Lab ro v2 queued behind em run 44435517079.

## Do this next
Read em (artifacts: reopen.md rows, doraby.md, events.jsonl kind doraby) then ro v2; then dispatch compat (items 5/7, lab_minutes 45, profile standard); then ship alpha71 (EM1k steps 1-2, naming title, whatever else lands): bump geckoapp versionCode/Name, bash tools/sync-ext-version.sh, bash ship.sh gecko with note, ONE dispatch app=gecko full_build=true. Heartbeat trig_018ZFUWQpV3dBUPXUvKDiXwQ.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  abb85ece ckpt 2698: Lab run 44434241145 (suite ro, commit 609d22a7) READ: the 3 reopens that ran
  0853cfea ckpt 2697: EM1k step 2 CODED + tests green + Gecko module compiles: a page that painted
  7c63b314 ckpt 2696: EM1a baseline READ (daily run 44432470135 green: security 17/0, 68 pages pai
  69bb5648 ckpt 2695: EM1i research: real doraby chain measured from the sandbox (6 servers, resea
  609d22a7 ckpt 2694: EM1k step 1 CODED + compiled: core LastLookRules.whyNot (+test, green) names
  54c9c34f ckpt 2693: EM1a: baseline lab DISPATCHED 2026-11-19 00:52Z: android.yml run 44432470135
  ce7436f4 ckpt 2692: Owner request 2026-11-19 (11 improvement areas + start GitHub emulator labs)
  06160983 ckpt 2691: Handoff saved (.handoff/handoff.md + archive; CLAUDE.md and AGENTS.md learni
  7cfac9cf ckpt 2690: VS1e ticked (shipped in alpha70, tested, not proven on the real site).
  dc968cf9 ckpt 2689: alpha70 RELEASED: tag kestrel-g-v0.3.0-alpha70, run 44255522156 all jobs gre
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
