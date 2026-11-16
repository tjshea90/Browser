# CHECKPOINT 2466 — read me first, then TASKS.md

**Written:** 2026-11-16T02:46:25Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `7ff493c9` (this checkpoint is the commit after it)

## Just done
owner message 52 (rotation still laggy / glitchy, status bar icons stuck in the middle and video cropped for a moment, video very laggy in full screen, research what other browsers do for video acceleration and whether third-party code helps) written into TASKS.md as ANS52 + rows a-f BEFORE any code; reading: the stuck icons and the cropped video are one symptom (old 600 px buffers shown in the new 1024 px display until our window draws its first frame at the new size; our UI thread is blocked in GeckoView's synchronous resize: ui late-beat 2021 ms)

## Do this next
(1) spawn 3 research agents (browsers' video pipelines, third-party players, device hardware + Gecko prefs) writing research/VIDEO_ACCEL_*.md; (2) meanwhile ANS52a: read BrowserActivity layout of geckoView/content/turnCover and TurnSettler, add Health 'turn first-frame N ms' and keep GeckoView at its old size for the first traversal of a turn, release after the first draw, machine must not settle while the resize is pending, tests; (3) the alpha56 review agent a8e537c38b2ece4e6 still running; compat run 43784927532 ends ~03:25Z (read 03:30Z), kp run 43793291701 pending; send_later trig_015YaAfScTQXziSPN7zQ5u86 04:20Z

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  609cad71 ckpt 2465: evidence file corrected (ONE full screen with four control sessions, three w
  82385186 ckpt 2464: ANS51h: no page-look read-back while a page is full screen (the tablet's log
  c7e7d87b ckpt 2463: ANS51b-f ticked in TASKS.md; kp lab scene extended (soak.py kp_scene: after 
  a6585051 ckpt 2462: ANS51b-f CODED + tested: (b) BrowserActivity.filmPicture feeds the film's w/
  dd7a66a8 ckpt 2461: owner message 51 (the real tablet's Diagnostics of alpha55): read in researc
  36982a2b ckpt 2460: stuck-screen safety net (review F1 beyond the Settings toggle): the app's an
  0c425e84 ckpt 2459: owner message 50 (the alpha51 half of message 49 sent again: 'some of the fi
  7ee3a4a1 ckpt 2458: alpha55 RELEASED and recorded: Release kestrel-g-v0.3.0-alpha55 (run 4378408
  90d4d6e8 ckpt 2457: gated kestrel-g-v0.3.0-alpha55 (gecko code 156) and pushed it: handoff self-
  6d36261e ckpt 2456: independent review of the alpha55 code (agent, 12 findings) ALL addressed wi
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
