# CHECKPOINT 2467 — read me first, then TASKS.md

**Written:** 2026-11-16T02:55:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `3b85ec83` (this checkpoint is the commit after it)

## Just done
ANS52a/a2 CODED + tested: (a) BrowserActivity.holdGeckoView/releaseGeckoHold keep the engine's view at its OLD size for the first traversal of a turn that gets the cover, so the app window draws the cover and the bars at the new size at once and the blocking GeckoView resize (0.9-2 s on the tablet) happens behind the cover; TurnSettler.firstFrame + Health turn-first-frame + 'first frame at the new size' in the Diagnostics line; Host.resizePending keeps the cover while the view waits for its size; (a2) SILENT_MS 3 s -> 6 s and the 1 px nudge only for a page that spoke with a wrong size; kp lab scene gets 2 checks (turn-hold/released + first frame < 3 s); core + geckoapp suites green; research agents (browsers, third-party, device/rotation) and the alpha56 review agent still running

## Do this next
(1) dispatch ONE kp lab run on this commit (app=lab lab_suite=kp; it replaces the pending run 43793291701 of the older commit; the compat run 43784927532 still holds the lab until ~03:25Z); (2) when the research agents report (research/VIDEO_ACCEL_BROWSERS.md, VIDEO_ACCEL_THIRDPARTY.md, VIDEO_ACCEL_DEVICE.md) write research/VIDEO_ACCELERATION.md = ranked plan + decisions, TASKS ANS52e rows, implement the cheap safe steps; (3) review agent a8e537c38b2ece4e6 findings -> verify, fix, test; (4) 03:30Z compat read (trig_019ZZh4wE2UHrvMLbwxEFmrV); then bump 157/alpha56, ship, record, final answers to messages 51 AND 52; send_later trig_015YaAfScTQXziSPN7zQ5u86 04:20Z; heartbeat trig_01Y8CFHKVb6hmQmEEM78p6gv

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5235b647 ckpt 2466: owner message 52 (rotation still laggy / glitchy, status bar icons stuck in 
  609cad71 ckpt 2465: evidence file corrected (ONE full screen with four control sessions, three w
  82385186 ckpt 2464: ANS51h: no page-look read-back while a page is full screen (the tablet's log
  c7e7d87b ckpt 2463: ANS51b-f ticked in TASKS.md; kp lab scene extended (soak.py kp_scene: after 
  a6585051 ckpt 2462: ANS51b-f CODED + tested: (b) BrowserActivity.filmPicture feeds the film's w/
  dd7a66a8 ckpt 2461: owner message 51 (the real tablet's Diagnostics of alpha55): read in researc
  36982a2b ckpt 2460: stuck-screen safety net (review F1 beyond the Settings toggle): the app's an
  0c425e84 ckpt 2459: owner message 50 (the alpha51 half of message 49 sent again: 'some of the fi
  7ee3a4a1 ckpt 2458: alpha55 RELEASED and recorded: Release kestrel-g-v0.3.0-alpha55 (run 4378408
  90d4d6e8 ckpt 2457: gated kestrel-g-v0.3.0-alpha55 (gecko code 156) and pushed it: handoff self-
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
