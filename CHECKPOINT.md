# CHECKPOINT 2453 — read me first, then TASKS.md

**Written:** 2026-11-16T00:00:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `02a03429` (this checkpoint is the commit after it)

## Just done
kp lab run 3 (43776782679, good boot 512 MB heap): 18 of 21 checks pass on the REAL engine (controls draw in portrait/landscape, tap shows/hides, page never gets the taps, double tap right/left/middle, mute, exit, page chrome put away AND given back, pictures kp-1/5/9/10 look right). Three failures fixed: (1) a second press on the big button asked 'pause' again after the film had paused: PlayerModel now numbers its commands (Cmd.id, json i), player.js echoes the last carried out as ak in every report, a report older than the command cannot end the optimistic state (3 s fallback), tests added (PlayerModelTest 2 new, player.test.js ak test); (2) 10 s button check tapped the wrong place (lab: +-102 px, back button check added); (3) Hide mode: Android 10 sends NO callback for a transient navigation bar (picture kp-11 shows the bar over the film), so BrowserActivity.navBarPx() keeps the bar height free at the bottom of the controls the whole time the bars are hidden (listener removed), lab check 'player inset ... free at the bottom'. Full Kotlin suites + web-tests (470) green before these last changes; player + OwnerRules tests green after.

## Do this next
dispatch android.yml app=lab lab_suite=kp lab_minutes=10 lab_net=full ref=ccr-b3c2484c-sbew36 (run 4); unpack (actions_list list_workflow_run_artifacts -> actions_get download_workflow_run_artifact -> tools/emu/fetch_artifact.py into the scratchpad); check health-lines.txt heap line first; expect 23 checks ok incl. 'and plays again', '10 s button back', 'free at the bottom'; look at kp-10/kp-11 pictures (bottom row clear of the nav bar); then full suites again (gradle :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest + web-tests npm test), TASKS.md VP rows tick with the run id, HANDOFF.md (ack/ak, navBarPx), ship alpha55 (versionCode 156, 0.3.0-alpha55), final answer to owner message 49; heartbeat trig_01Y8CFHKVb6hmQmEEM78p6gv

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  4b0bd9aa ckpt 2452: kp lab run 2 (43775890786) read: it says NOTHING about the controls: the emu
  05a26786 ckpt 2451: VP1 lab scene kp fixed after the first real-engine run (soak.py kp_scene: pa
  a7ae2f8b ckpt 2450: VP1 lab scene kp written (soak.py kp_scene with exact multi-taps via the tou
  ca648b5d ckpt 2449: VP1 first cut compiles and is tested: player.js (state reports, tap shield, 
  df745dca ckpt 2448: gated kestrel-g-v0.3.0-alpha54 (gecko code 155) and pushed it: handoff self-
  17392233 ckpt 2447: pre-ship: alpha54: fixes from an independent check of alpha53 (the new video
  aa9dfa02 ckpt 2446: owner message 49 (video player audit + overhaul request, with observations o
  4e91027c ckpt 2445: ROT3 coded (not shipped): FullscreenTurner waits 650 ms of the tablet stayin
  f1e253aa ckpt 2444: alpha53 RELEASED and recorded (run 43770059374 green); owner message 48 ANSW
  f4fbfe56 ckpt 2443: alpha53 (ROT2: the cover and pause of a turn wait until the page has the new
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
