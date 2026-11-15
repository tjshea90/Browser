# CHECKPOINT 2422 — read me first, then TASKS.md

**Written:** 2026-11-15T20:31:23Z · **tests:** 1 RED: test_handoff.sh (0 green)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `73a82638` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha50: fixes from an independent check of alpha49 (everything else is as alpha49: full-screen bars, turning, controls, sound, video bar). The Diagnostics line Full-screen system bars now reads the bars correctly on Android 10 (alpha49 could say hidden while both bars were showing, and its retry never ran); a tab that was closed, put to sleep or replaced no longer makes a turn of the screen cover the page for nothing, or count a video pause that never happened; turning the tablet again during a turn now extends the turn (before, it was ignored once the video had been paused); timers of the full-screen code are cancelled when the app closes. Tested in headless browsers, by unit tests and on an Android 10 emulator, NOT on your tablet: please try the Yahoo Finance video in full screen, turn the tablet both ways, and send Diagnostics (lines Full-screen system bars and Turning under a video) if a bar still covers anything.

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  342c62e0 ckpt 2421: compat run 1 (43747605584) read: only 4 sites measured, the emulated phone's
  b0aa0482 ckpt 2420: lab driver: Yahoo Full-button tap x 556 (inside the button for film widths 5
  a88bd7cb ckpt 2419: alpha49 review (a1b60178ab815123f) fixed: RA1 media state forgotten with the
  2de3f20b ckpt 2418: alpha49 shipped and recorded: Release kestrel-g-v0.3.0-alpha49 (run 43751318
  7e7d3319 ckpt 2417: lab scene states fixed (the app turns a wide video sideways by itself at ful
  0a81c2b8 ckpt 2416: gated kestrel-g-v0.3.0-alpha49 (gecko code 150) and pushed it: handoff self-
  afdbb7c6 ckpt 2415: pre-ship: alpha49: full-screen video fixes after your three screenshots. (1)
  7c987e9f ckpt 2414: code for owner message 46 + second review: FSB1 (applyImmersive without LAYO
  7f2c4c92 ckpt 2413: owner message 46 written into TASKS.md (FSB1 bars cover the controls in full
  b8c5d333 ckpt 2412: hour soak 43740398387 read (commit 281c48f4, hotspot-shaped, 70 min, 79 scen
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
