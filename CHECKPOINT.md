# CHECKPOINT 2430 — read me first, then TASKS.md

**Written:** 2026-11-15T20:58:46Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `38a628d2` (this checkpoint is the commit after it)

## Just done
alpha51 gated and queued (FB1 bars kept by default + Settings row, FB2/FB3 turn.js page-level pause + resize nudge): web tests 452 pass, Kotlin suites green in ship.sh; lab fsy re-dispatched 20:55Z after fixing the workflow (import os)

## Do this next
NEXT: (1) alpha51 release run (dispatched ~20:59Z): when the tag kestrel-g-v0.3.0-alpha51 exists and the run is green: bash tools/record-release.sh kestrel-g-v0.3.0-alpha51 "$(cat scratchpad/note51.one)" (identical note) then answer the owner with the Release link; (2) read the fsy lab run (dispatched 20:55Z): keep vs hide strip rows, turn lines, the page's own pause, yahoofs-* pictures (the owner's stale-size symptom), sound table; (3) tell the owner to check Settings for the new row 'System bars in full-screen video' (if it is missing the update did not install); (4) compat run again (lab_minutes=120) after fsy

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  38a628d2 ckpt 2429: gated kestrel-g-v0.3.0-alpha51 (gecko code 152) and pushed it: handoff self-
  7c27ad91 ckpt 2428: pre-ship: alpha51: full-screen video, second attempt after alpha50 changed n
  88fb79ef ckpt 2427: FB1 + FB2 + FB3 coded: system bars kept by default (Prefs.fullscreenBars, Se
  691a247e ckpt 2426: owner message 47 written into TASKS.md (alpha50 changed nothing on the table
  d941af5c ckpt 2425: alpha50 shipped and recorded (Release kestrel-g-v0.3.0-alpha50, run 43758859
  4415fbaf ckpt 2424: lab run 43753283814 (fsy) read: it says NOTHING about the full-screen code: 
  6689210f ckpt 2423: gated kestrel-g-v0.3.0-alpha50 (gecko code 151) and pushed it: handoff self-
  ca8bc3fb ckpt 2422: pre-ship: alpha50: fixes from an independent check of alpha49 (everything el
  342c62e0 ckpt 2421: compat run 1 (43747605584) read: only 4 sites measured, the emulated phone's
  b0aa0482 ckpt 2420: lab driver: Yahoo Full-button tap x 556 (inside the button for film widths 5
```
