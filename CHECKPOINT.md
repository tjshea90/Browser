# CHECKPOINT 2428 — read me first, then TASKS.md

**Written:** 2026-11-15T20:55:03Z · **tests:** 1 RED: test_handoff.sh (0 green)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `2fbcf755` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha51: full-screen video, second attempt after alpha50 changed nothing on your tablet. (1) The system bars are no longer asked to hide in full screen by default (alpha48 to alpha50 asked, and your tablet kept drawing both bars over a page that had been laid out as if they were gone): the page now fills only the space between the status bar and the navigation bar, exactly like every normal page, so a player's controls and Kestrel's own bar are never under the navigation bar, in both orientations. Settings has a new row System bars in full-screen video (Keep is the default; Hide fills the whole screen, only for a tablet that really hides its bars). (2) Turning the tablet: every video of the page is paused when the screen turns (muted ones too, which alpha49 could not reach), the page is given one extra resize once the screen has stopped moving so a player that sized itself from the old shape redoes its size, then the video plays again by itself; a video you had paused stays paused and a touch during the turn leaves it paused. The black cover during the turn stays. Diagnostics has a new line Turning, the page's own pause. Tested in headless browsers (452 tests) and unit tests, NOT on your tablet and not yet in a clean emulator run: please try the Yahoo Finance video in full screen, turn the tablet both ways, and tell me what you see.

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  88fb79ef ckpt 2427: FB1 + FB2 + FB3 coded: system bars kept by default (Prefs.fullscreenBars, Se
  691a247e ckpt 2426: owner message 47 written into TASKS.md (alpha50 changed nothing on the table
  d941af5c ckpt 2425: alpha50 shipped and recorded (Release kestrel-g-v0.3.0-alpha50, run 43758859
  4415fbaf ckpt 2424: lab run 43753283814 (fsy) read: it says NOTHING about the full-screen code: 
  6689210f ckpt 2423: gated kestrel-g-v0.3.0-alpha50 (gecko code 151) and pushed it: handoff self-
  ca8bc3fb ckpt 2422: pre-ship: alpha50: fixes from an independent check of alpha49 (everything el
  342c62e0 ckpt 2421: compat run 1 (43747605584) read: only 4 sites measured, the emulated phone's
  b0aa0482 ckpt 2420: lab driver: Yahoo Full-button tap x 556 (inside the button for film widths 5
  a88bd7cb ckpt 2419: alpha49 review (a1b60178ab815123f) fixed: RA1 media state forgotten with the
  2de3f20b ckpt 2418: alpha49 shipped and recorded: Release kestrel-g-v0.3.0-alpha49 (run 43751318
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
