# CHECKPOINT 2452 — read me first, then TASKS.md

**Written:** 2026-11-15T23:48:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `0ffaaa1a` (this checkpoint is the commit after it)

## Just done
kp lab run 2 (43775890786) read: it says NOTHING about the controls: the emulator boot gave the app a 16 MB java heap limit (growth limit 16777216), the app died of OutOfMemoryError in Neon.tick in a crash loop (second time after run 43753283814); lab now throws such a boot away (soak.py start_app: heap_limit_mb() from the app's 'start heap' Health line, hard_restart quiet, twice at most); new in the app: PlayerControlsView caches its scrim shaders, redraws only when something drawn changed, buffering ring by the clock at 20 fps in its own rect; player.js reports once a second while hidden; player.js putAway/bringBack hides the page's own player chrome (YouTube/Shaka/video.js/JW/Plyr/MediaElement classes) and the engine's native controls while Kestrel's controls own the film (tests 14 pass); lab page fsbox chrome=1 + two new kp checks; HANDOFF.md alpha54/55 entry

## Do this next
re-dispatch android.yml app=lab lab_suite=kp lab_minutes=10 lab_net=full ref=ccr-b3c2484c-sbew36 (owner tjshea90 repo Browser); unpack with tools/emu/fetch_artifact.py (actions_list list_workflow_run_artifacts -> actions_get download_workflow_run_artifact); FIRST look at health-lines.txt for 'java heap limit' (16 MB = bad boot: dispatch again) then report.md kp table + pictures kp-1..kp-11; then full suites (Kotlin + web-tests), TASKS.md VP rows with evidence, ship alpha55 (versionCode 156, 0.3.0-alpha55), final answer to owner message 49; heartbeat trig_01Y8CFHKVb6hmQmEEM78p6gv

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  05a26786 ckpt 2451: VP1 lab scene kp fixed after the first real-engine run (soak.py kp_scene: pa
  a7ae2f8b ckpt 2450: VP1 lab scene kp written (soak.py kp_scene with exact multi-taps via the tou
  ca648b5d ckpt 2449: VP1 first cut compiles and is tested: player.js (state reports, tap shield, 
  df745dca ckpt 2448: gated kestrel-g-v0.3.0-alpha54 (gecko code 155) and pushed it: handoff self-
  17392233 ckpt 2447: pre-ship: alpha54: fixes from an independent check of alpha53 (the new video
  aa9dfa02 ckpt 2446: owner message 49 (video player audit + overhaul request, with observations o
  4e91027c ckpt 2445: ROT3 coded (not shipped): FullscreenTurner waits 650 ms of the tablet stayin
  f1e253aa ckpt 2444: alpha53 RELEASED and recorded (run 43770059374 green); owner message 48 ANSW
  f4fbfe56 ckpt 2443: alpha53 (ROT2: the cover and pause of a turn wait until the page has the new
  848ef753 ckpt 2442: gated kestrel-g-v0.3.0-alpha53 (gecko code 154) and pushed it: handoff self-
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
