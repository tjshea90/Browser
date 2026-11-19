# CHECKPOINT 2709 — read me first, then TASKS.md

**Written:** 2026-11-19T06:10:09Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-da310608-abhqo4` · **builds on:** `ce7a15bf` (this checkpoint is the commit after it)

## Just done
DG1a-d DONE in code: LastRun.kt tells an update from a crash and marks the first start after an install (seconds, not '0 min'), 2 s tick in the first minute, start oat/art kept; ShieldsBridge press-to-play stats clip 300 -> 600; pull-down Health line 1 per 8 s; LastRunTest 17 green; :geckoapp:compileDebugKotlin green (needed sdkmanager platforms;android-37.0 and gradle without --offline, now in CLAUDE.md quirks); research/DG1_FIRST_RUN_AFTER_INSTALL.md written

## Do this next
DG1e: answer the owner in plain words (finding: every unexplained on-screen end sits on an install; what changed; NOT proven on the tablet; ask: did the keyboard pop up by itself on returning to the app; the lines to send after the next install). Changes are NOT released: alpha72 would need versionCode 173 + bash ship.sh gecko + ONE dispatch (EM1k + DG1 code are unreleased); ALSO still open from the earlier checkpoint: read compat run 44441687717 into research/EM1_RESULTS.md and dr v3 (EM1f/EM1h), EM1l answer; heartbeat trig_018ZFUWQpV3dBUPXUvKDiXwQ may still exist (get_trigger, delete when done)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M CLAUDE.md
     M TASKS.md
     M research/DG1_FIRST_RUN_AFTER_INSTALL.md

## Last ten checkpoints
```
  e8adf8c4 ckpt 2708: DG1 written into TASKS (owner Diagnostics paste, alpha71): finding 1 = every
  4acfe271 ckpt 2707: alpha71 RELEASED and RECORDED: https://github.com/tjshea90/Browser/releases/
  1aa1cf0c ckpt 2706: Lab dr v2 (run 44439418098) READ: wrapper-direct rows are NOT the owner's fl
  5b20c111 ckpt 2705: alpha71 DISPATCHED ONCE: android.yml app=gecko full_build=true on branch tip
  f314d5ed ckpt 2704: gated kestrel-g-v0.3.0-alpha71 (gecko code 172) and pushed it: handoff self-
  c1463b83 ckpt 2703: alpha71 gates: first ship.sh run stopped on two documented timing-flaky down
  434b5ea7 ckpt 2702: alpha71 PREPARED: versionCode 172 / 0.3.0-alpha71, extension manifest 1.0.17
  ab9af2b4 ckpt 2701: dr scene: Vidfast also tried with press-to-play OFF (tells the rule from the
  378e887d ckpt 2700: Lab run 44435517079 (em) READ: reopen 3/3 drew the picture again; dr INCONCL
  1476784b ckpt 2699: EM1i naming: the REAL rendered doraby title page (headless Chromium) gives O
```

(13 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
