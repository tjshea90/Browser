# CHECKPOINT 2706 — read me first, then TASKS.md

**Written:** 2026-11-19T02:22:04Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kestrel-browser-optimization-h6y8u2` · **builds on:** `8ceee4cf` (this checkpoint is the commit after it)

## Just done
Lab dr v2 (run 44439418098) READ: wrapper-direct rows are NOT the owner's flow (no server tap, so the gate has nothing to inherit): Vidfast rule ON = 3 POST maybes skipped, no stream; rule OFF = finder lists 2 HLS streams (so the player starts without the gate; with the person's tap on the server the VP2 inherit rule should apply: UNPROVEN on the real chain); vidsrc/vidcore runner timeouts, 111movies froze the phone (restart-safe path ran). dr scene v3: opens the TITLE page and TAPS the server button (positions measured in Chromium: Vidfast (145,45) 111movies (415,45) Vidsrc (145,91) Videasy (415,91) Vidzee (145,137) Vidcore (415,137) CSS px, screen y = 26 + css*1.0714), Vidfast also with the rule OFF, 111movies last.

## Do this next
Release run 44443158040 (alpha71) is building (check + test-js green, test-jvm running): when green verify tag/release then bash tools/record-release.sh kestrel-g-v0.3.0-alpha71 with the note of docs/handoff/ALPHA71_RELEASE_NOTE.txt. Dispatch dr v3 (lab_suite=dr) now = queued behind compat run 44441687717. Then read compat + dr v3 into research/EM1_RESULTS.md; if dr v3 shows vidfast with the person's tap still holds, fix gate / personFrame (background.js frameNavigates, PTAP_MS 8 s, PCHILD_MS 12 s) for alpha72. Heartbeat trig_018ZFUWQpV3dBUPXUvKDiXwQ.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5b20c111 ckpt 2705: alpha71 DISPATCHED ONCE: android.yml app=gecko full_build=true on branch tip
  f314d5ed ckpt 2704: gated kestrel-g-v0.3.0-alpha71 (gecko code 172) and pushed it: handoff self-
  c1463b83 ckpt 2703: alpha71 gates: first ship.sh run stopped on two documented timing-flaky down
  434b5ea7 ckpt 2702: alpha71 PREPARED: versionCode 172 / 0.3.0-alpha71, extension manifest 1.0.17
  ab9af2b4 ckpt 2701: dr scene: Vidfast also tried with press-to-play OFF (tells the rule from the
  378e887d ckpt 2700: Lab run 44435517079 (em) READ: reopen 3/3 drew the picture again; dr INCONCL
  1476784b ckpt 2699: EM1i naming: the REAL rendered doraby title page (headless Chromium) gives O
  abb85ece ckpt 2698: Lab run 44434241145 (suite ro, commit 609d22a7) READ: the 3 reopens that ran
  0853cfea ckpt 2697: EM1k step 2 CODED + tests green + Gecko module compiles: a page that painted
  7c63b314 ckpt 2696: EM1a baseline READ (daily run 44432470135 green: security 17/0, 68 pages pai
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
