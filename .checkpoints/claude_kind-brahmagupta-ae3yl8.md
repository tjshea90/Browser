# CHECKPOINT 3167 — read me first, then TASKS.md

**Written:** 2026-11-24T04:27:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `d965b9fa` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha102: the animations on that page. (1) NO PLAY BAR OVER ANIMATIONS: a clip that is silent, loops and has no controls of its own is the page's animation, not a film. Tapping it no longer brings up Kestrel's play, seek and mute bar over it, not even with the video controls row set to Always. A film, or a looping clip with sound, still gets the bar. (2) LESS LAG: I loaded the page from your screenshot in a browser engine here that also draws on the processor, with the processor slowed down to stand in for your tablet. The page is a sliding strip of 60 small looping clips. With its clips paused it drew about 60 frames a second, with even one clip playing about 36, with four playing about 32, so every playing clip slows the whole page, including the text above it. Now such a clip plays only while it is on screen, and at most two play at once (the most visible ones); the others keep their picture and start again when there is room. Settings, Limit looping page clips, turns this off. It never touches a clip that has sound, controls or does not loop, never starts a clip that your press to play rule holds back, and leaves a page alone that keeps fighting it. Diagnostics shows a line, Decorative page clips, with how often it acted. Not run on the tablet: it is tested in a desktop browser engine and in the unit tests, not in Firefox on your tablet. Said plainly: clips beyond two stay still (a dark box where the page gave no picture), and the strip itself still slides; Calm pages (Settings, Video) and Calmer redrawing (Network lab) can calm that on pages that allow it.

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  15d8bb97 ckpt 3165: owner request PERF22 (deep research: popular browsers' speed techniques, cod
  d452ebef ckpt 3163: PERF21 coded: no bar on decorative clips (vfind decorative + videobar judge)
  09db2cad ckpt 3153: owner request PERF21 (screenshot: a page of looping picture clips is very la
  2057b01b ckpt 3152: alpha101 SHIPPED and recorded: run 45312620908 green on attempt 1 (check, te
  1e465904 ckpt 3150: alpha101 (code 202) gated (ckpt 3148, head 94c20d92 + the corrected note fil
  94c20d92 ckpt 3148: gated kestrel-g-v0.3.0-alpha101 (gecko code 202) and pushed it: handoff self
  9d6056a5 ckpt 3146: PERF20 coded and green: TrimGate (core) + TrimGateContext (app, Robolectric-
  f082d388 ckpt 3138: owner request PERF20 (research how the browser can be as fast as possible us
  2b1119f3 ckpt 3137: TASKS.md closed for PERF17e/PERF18, PERF17 left open for the c/d fixes (clai
  7a224ab8 ckpt 3135: alpha100 SHIPPED and recorded: run 45303459458 green on attempt 1 (check, te
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
