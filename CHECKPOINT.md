# CHECKPOINT 2708 — read me first, then TASKS.md

**Written:** 2026-11-19T06:04:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-da310608-abhqo4` · **builds on:** `03cd047b` (this checkpoint is the commit after it)

## Just done
DG1 written into TASKS (owner Diagnostics paste, alpha71): finding 1 = every 'ended on screen, no crash' run sits on an install (alpha70 07:12Z, alpha71 02:51Z); LastRun.kt read, ProcessLife.kt read (its own exit marks markExit, not the cause)

## Do this next
DG1a: in app/diag/LastRun.kt add nextBuild= to the saved text in begin() and installAt= (PackageInfo.lastUpdateTime) to flush(); read() says 'ended by an update' when nextBuild != build and 'first start after an install, lived N s' when started - installAt < 5 min; add tests in LastRunTest.kt; then DG1b tick 2 s for the first 60 s; then DG1c research file; then DG1d items; no heartbeat needed

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  4acfe271 ckpt 2707: alpha71 RELEASED and RECORDED: https://github.com/tjshea90/Browser/releases/
  1aa1cf0c ckpt 2706: Lab dr v2 (run 44439418098) READ: wrapper-direct rows are NOT the owner's fl
  5b20c111 ckpt 2705: alpha71 DISPATCHED ONCE: android.yml app=gecko full_build=true on branch tip
  f314d5ed ckpt 2704: gated kestrel-g-v0.3.0-alpha71 (gecko code 172) and pushed it: handoff self-
  c1463b83 ckpt 2703: alpha71 gates: first ship.sh run stopped on two documented timing-flaky down
  434b5ea7 ckpt 2702: alpha71 PREPARED: versionCode 172 / 0.3.0-alpha71, extension manifest 1.0.17
  ab9af2b4 ckpt 2701: dr scene: Vidfast also tried with press-to-play OFF (tells the rule from the
  378e887d ckpt 2700: Lab run 44435517079 (em) READ: reopen 3/3 drew the picture again; dr INCONCL
  1476784b ckpt 2699: EM1i naming: the REAL rendered doraby title page (headless Chromium) gives O
  abb85ece ckpt 2698: Lab run 44434241145 (suite ro, commit 609d22a7) READ: the 3 reopens that ran
```
