# CHECKPOINT 2437 — read me first, then TASKS.md

**Written:** 2026-11-15T21:43:21Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `41f21c4c` (this checkpoint is the commit after it)

## Just done
owner message 48 (two screenshots of a turn: the page keeps the previous orientation's layout for a few seconds after a rotation) written into TASKS.md as ANS48 + ROT2a-e with my measured reading of the screenshots; lab run 43763822027 (fsy, alpha52 code) finished green, artifact not read yet

## Do this next
1) read lab run 43763822027 artifact (list_workflow_run_artifacts -> download_workflow_run_artifact -> tools/emu/fetch_artifact.py URL <empty dir under scratchpad>): FB4 first-state red strip + fullscreen viewport lines; 2) implement ROT2a (core TurnSettleMachine pageConfirmed + PAGE_CAP_MS + tests), ROT2b (turn.js reports while a turn goes on), ROT2c (BrowserActivity/TurnSettler wiring, lag line in Diagnostics, hint ring), ROT2d lab slow-turn scene; 3) ship alpha53, answer ANS48

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  ac2107ef ckpt 2436: alpha52 RELEASED and recorded (run 43763812046 green, APK in the Release, BU
  1e089300 ckpt 2435: alpha52 shipped to GitHub (gates green, pushed): release run 43763812046 (an
  49db2736 ckpt 2434: gated kestrel-g-v0.3.0-alpha52 (gecko code 153) and pushed it: handoff self-
  ab5859df ckpt 2433: pre-ship: alpha52: full-screen video, third step. Found with the emulator la
  80198e75 ckpt 2432: FB4 coded + all green: turn.js vpt report, background relay, ShieldsBridge v
  9780732e ckpt 2431: lab run 43760546128 (fsy, first clean run, alpha51 code) read: Keep mode put
  e9464a7e ckpt 2430: alpha51 gated and queued (FB1 bars kept by default + Settings row, FB2/FB3 t
  38a628d2 ckpt 2429: gated kestrel-g-v0.3.0-alpha51 (gecko code 152) and pushed it: handoff self-
  7c27ad91 ckpt 2428: pre-ship: alpha51: full-screen video, second attempt after alpha50 changed n
  88fb79ef ckpt 2427: FB1 + FB2 + FB3 coded: system bars kept by default (Prefs.fullscreenBars, Se
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
