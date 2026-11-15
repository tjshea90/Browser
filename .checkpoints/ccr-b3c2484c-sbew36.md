# CHECKPOINT 2427 — read me first, then TASKS.md

**Written:** 2026-11-15T20:52:03Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `0b06b051` (this checkpoint is the commit after it)

## Just done
FB1 + FB2 + FB3 coded: system bars kept by default (Prefs.fullscreenBars, Settings row, applyImmersive only for Hide, lab hook 'bars'), turn.js pauses every video of every frame for a turn + one more resize + play again (web-tests/turn.test.js 9 tests), vbk 'turn' relayed + Diagnostics line, guard tests; lab workflow fixed (import os: the fsy re-run 43759631927 failed in 3 s at 'Create the tablet-shaped emulator'), lab scenes use keep/hide

## Do this next
NEXT: npm test (background log scratchpad/npm-test2.log) and the full Kotlin suite, then ship alpha51 (versionCode 152; note51.one) and dispatch; re-dispatch the lab fsy (now works) and read it; answer the owner (ANS47)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  691a247e ckpt 2426: owner message 47 written into TASKS.md (alpha50 changed nothing on the table
  d941af5c ckpt 2425: alpha50 shipped and recorded (Release kestrel-g-v0.3.0-alpha50, run 43758859
  4415fbaf ckpt 2424: lab run 43753283814 (fsy) read: it says NOTHING about the full-screen code: 
  6689210f ckpt 2423: gated kestrel-g-v0.3.0-alpha50 (gecko code 151) and pushed it: handoff self-
  ca8bc3fb ckpt 2422: pre-ship: alpha50: fixes from an independent check of alpha49 (everything el
  342c62e0 ckpt 2421: compat run 1 (43747605584) read: only 4 sites measured, the emulated phone's
  b0aa0482 ckpt 2420: lab driver: Yahoo Full-button tap x 556 (inside the button for film widths 5
  a88bd7cb ckpt 2419: alpha49 review (a1b60178ab815123f) fixed: RA1 media state forgotten with the
  2de3f20b ckpt 2418: alpha49 shipped and recorded: Release kestrel-g-v0.3.0-alpha49 (run 43751318
  7e7d3319 ckpt 2417: lab scene states fixed (the app turns a wide video sideways by itself at ful
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
