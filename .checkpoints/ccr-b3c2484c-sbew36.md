# CHECKPOINT 2567 — read me first, then TASKS.md

**Written:** 2026-11-16T22:31:33Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `3791947b` (this checkpoint is the commit after it)

## Just done
review of alpha61 applied (5 small fixes, unit tests 26/26 in PlayerGesturesTest): jump text after a seek no longer says twice the jump (VideoGestures.seekCommit takes the target first), a quick new touch clears the old jump text, the middle strip keeps the 5 % dead start (a sideways drag that begins with a wobble is still a seek), PlayerGestures.vertical ends an armed finger's arming in the dead strip, PlaybackMeter uses a 2 s window instead of a flag (the first real stall after a buffered seek was swallowed); lab kp/np double taps now played by the app (Phone.double_tap, exact 90 ms gap); lab vp run 43966056564 on head 8addd28f: 365 checks ok, 2 failed (the kp double tap with two parallel input tap processes), cp scene all green

## Do this next
1) full JVM suites (./gradlew --offline -q :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest) and web tests (cd web-tests && npm test) on this head; 2) dispatch lab vp on the new head (final release gate: gp 27/27, iq 10/10, np stats line, kp double tap with Phone.double_tap, cp) and read the report; 3) fill TESTED_PLACEHOLDER in docs/handoff/ALPHA61_RELEASE_NOTE.txt, ship.sh gecko, ONE run_workflow app=gecko full_build=true, verify tag + jobs + release, record-release.sh with the identical note; 4) tick ANS60a-d, answer owner 60, delete heartbeat trig_01WwqLTpgB4vKso9Rn5q9WV1

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/ANS60_PLAYBACK_OPTIMISATION.md

## Last ten checkpoints
```
  d98f2fd2 ckpt 2566: lab vp dispatched on head 8addd28f (cp warm-up page): run 43966056564 starte
  8addd28f ckpt 2565: lab cp scene made robust against a race found by reading the extension (firs
  abc3386d ckpt 2564: lab vp run 43960432251 (head 5beb6f7e) finished: new ANS60 checks all pass (
  c952601c ckpt 2563: docs for ANS60: CLAUDE.md decisions (gestures without a long press, shared P
  203d9ece ckpt 2562: lab vp (all video suites incl. gp + iq) dispatched: run 43960432251 on head 
  5beb6f7e ckpt 2561: lab run 43958301833 (gp + iq, head 3bf6994e): ALL 26 gesture checks OK (A: p
  c905a3c3 ckpt 2560: JVM suites (core, app, geckoapp) green on the ANS60 head; lab gp + iq dispat
  f0dbcacd ckpt 2559: lab scenes for ANS60 written (not yet run): soak.py gp_scene = A: plain side
  d8aa91f1 ckpt 2558: ANS60d CODED (the emulator cannot show speed, only that nothing broke): Play
  c8fca4df ckpt 2557: ANS60c CODED (lab proof pending): quality list in the player inside the page
```

(13 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
