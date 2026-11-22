# CHECKPOINT 2952 — read me first, then TASKS.md

**Written:** 2026-11-22T06:37:19Z · **tests:** 1 RED: test_handoff.sh (0 green)
**Branch:** `ccr-144aedec-7apwin` · **builds on:** `fa1d7cd5` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha92: quality choices for pornhub in Kestrel's player, and a Safe drawing switch for the clipped lines. (1) Pornhub gives its page one list per quality, so Kestrel's own player saw a single picture and offered no choice. It now finds the other lists of the same film (same length, one picture each) and lists them under the quality button, in the full-screen player and in the player inside the page: Automatic, then 1080p, 720p, 480p, 240p as the site offers them. A pick swaps the list at the same second. It starts with the biggest quality that fits the smooth size: 720p full screen, 480p in a box, or what you picked last time. Pressing play again on any quality continues where you stopped. (2) The clipped line: your two screenshots show bands of text missing on claude.ai (a table header and the usage sheet), so it is not only the pornhub circle and not a page or Shields rule: the engine's graphics drew only part of the screen. New Settings > Network lab > Safe drawing (ON by default) turns off four of the engine's graphics shortcuts that update only part of the screen; it takes effect after you close Kestrel from the recent-apps list and open it again, and Diagnostics shows the values the engine really has. If bands still appear with it ON, the next upload tells us which shortcut to try next. Not run on the tablet: both changes (the emulator has no Mali graphics and no pornhub).

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ee6c59f9 ckpt 2938: PERF7c step 1: Settings > Network lab 'Safe drawing' (lab_draw, default ON) 
  449480c7 ckpt 2933: PERF7 started on branch ccr-144aedec-7apwin: owner request + 2 screenshots w
  4da54c18 ckpt 2915: alpha91 RELEASED and recorded (run 44987313373 green, Release has the APK). 
  a42e5268 ckpt 2913: alpha91 (code 192) gated (ship.sh green on attempt 3) and ONE dispatch made 
  df3d34d1 ckpt 2912: gated kestrel-g-v0.3.0-alpha91 (gecko code 192) and pushed it: handoff self-
  345be8b0 ckpt 2911: alpha91 ship attempt 3 running: attempt 1 red on OwnerRulesTest pin (10 s wi
  7693a032 ckpt 2908: pre-ship: alpha91: pornhub in Kestrel's own player, and the clipped circle. 
  32a10e12 ckpt 2906: pre-ship: alpha91: pornhub in Kestrel's own player, and the clipped circle. 
  0b23637d ckpt 2903: PERF6a evidence written: hand-over works when the film list is known (inline
  1935dd23 ckpt 2902: Owner sent diagnostics (rule ON): now even 240p won't load (PERF6 in TASKS.m
```

(13 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
