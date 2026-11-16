# CHECKPOINT 2559 — read me first, then TASKS.md

**Written:** 2026-11-16T21:13:59Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `044cccad` (this checkpoint is the commit after it)

## Just done
lab scenes for ANS60 written (not yet run): soak.py gp_scene = A: plain sideways drag seeks (3 cm = about 15 s, no long press), hints per side via a still finger ('gesture-hint' lines with the owner's words), rest + drag and no-rest drags give the same jumps, B (inside the page): plain drags on the right / left side = volume / brightness without the page scrolling, plain sideways = seek, a rest on the right side = 'Slide up or down for volume', a vertical drag that starts in the middle strip scrolls the page and changes no level, double tap; then Settings switch 'gesturehold 1' (new debug command): a plain drag scrolls the page, rest + drag = level ('gesture-armed'); new iq_scene (suites ip, gp, gv, vp) = quality list inside the page on an HLS film (soaklab variant inlinehls): Quality button place from the 'shown' line, real taps open the list and pick row 3, 'quality-closed', Back closes only the list, then the film; the 'stats' line with 'playable after'; np scene checks the Diagnostics line has 'playable ... waited for the network ... link estimate'; InlinePlayer logs 'quality-closed'; JVM suites running in the background

## Do this next
1) read the JVM suite result (tasks/bncpx0e2b.output ends with gradle-tests-exit=); 2) dispatch lab gp (gp + iq in one boot: android.yml app=lab lab_suite=gp lab_minutes=1 lab_net=full) and read report.md (sections gestures + quality list); fix what fails; 3) lab vp (all video suites incl. np with the refactored quality list) before shipping; 4) web tests (cd web-tests && npm test, no JS changed: run once at the end), release note docs/handoff/ALPHA61_RELEASE_NOTE.txt, versionCode 162 / 0.3.0-alpha61, tools/sync-ext-version.sh, ship.sh gecko, ONE dispatch, record-release.sh, answer owner 60; heartbeat trig_01WwqLTpgB4vKso9Rn5q9WV1

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d8aa91f1 ckpt 2558: ANS60d CODED (the emulator cannot show speed, only that nothing broke): Play
  c8fca4df ckpt 2557: ANS60c CODED (lab proof pending): quality list in the player inside the page
  756afc69 ckpt 2556: ANS60a+b CODED (lab proof pending): core PlayerGestures has NO long press an
  6d684f43 ckpt 2555: heartbeat trig_01WwqLTpgB4vKso9Rn5q9WV1 created (cron every 2 h at :45, boun
  00db0efd ckpt 2554: owner message 60 written into TASKS.md (ANS60a-d) in his words: gestures wit
  ce6ca1f7 ckpt 2553: alpha60 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha60 (code 161, ru
  a1b11269 ckpt 2552: RELEASE kestrel-g-v0.3.0-alpha60 (gecko code 161) DISPATCHED ONCE: android.y
  47d2920c ckpt 2551: gated kestrel-g-v0.3.0-alpha60 (gecko code 161) and pushed it: handoff self-
  4f54303f ckpt 2550: pre-ship: alpha60: your messages 55, 57, 58 and 59 (the player inside the we
  2ef63cb0 ckpt 2549: lab vp (all video suites kp np cp ap ho ip gp pv, one boot) dispatched: run 
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
