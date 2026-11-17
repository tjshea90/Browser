# CHECKPOINT 2616 — read me first, then TASKS.md

**Written:** 2026-11-17T09:11:08Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `5c924328` (this checkpoint is the commit after it)

## Just done
LAB5b: packaging A/B lab built and wired: geckoapp/build.gradle.kts has two OFF-by-default switches (-PomniStored=true stores assets/omni.ja uncompressed, verified locally: stored 14.51 MB; -PlibsInApk=true keeps native libs in the APK), tools/emu/packlab.py (fresh install per variant, warm+cold-cache starts via am start -W + Health marks, smaps_rollup Pss anon vs file per app process, install/package/lib/oat sizes; parsers selftested, page server tested), workflow suite 'pack' builds A (today) B (omni stored) C (libs in APK) D (both) and runs it (emu-lab.yml + android.yml options). FINDING that motivated it, from the released alpha63 APK: assets/omni.ja (Gecko's 14.5 MB bundle) is DEFLATED in the APK (13.96 -> 14.51 MB, nothing gained): Gecko must inflate a deflated entry into a private heap buffer in EVERY Gecko process, a stored one is mapped from the APK as shared evictable pages. Four research agents are running in the background (start path, extension footprint, Gecko prefs, packaging/ART) and a local debug build confirmed the DSL

## Do this next
1) dispatch the 'pack' suite ONCE (android.yml app=lab lab_suite=pack lab_net=full, ref ccr-e89b9dfe-pl3lk5), id via list_workflow_runs perPage 1 workflow_dispatch; 2) digest the four agent reports into research/LAB5_*.md as they arrive; 3) design the pref sweep + baseline-profile lab from the agents' candidates

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  1cf2f4fd ckpt 2615: LAB5 (owner message 2026-11-17T06:42:30Z: improve the browser as much as pos
  cc1424d3 ckpt 2614: alpha63 SHIPPED + recorded (run 44046401335 green, release kestrel-g-v0.3.0-
  12cd7f08 ckpt 2613: alpha63 dispatched ONCE (run_workflow app=gecko full_build=true ref ccr-a10e
  d59a6ddb ckpt 2612: gated kestrel-g-v0.3.0-alpha63 (gecko code 164) and pushed it: handoff self-
  3cdec9fe ckpt 2611: run 9 read: degraded run (emulator restarted, no network, Google A/B all 60 
  ede3c889 ckpt 2610: OWNER RESCHEDULE (usage running out, 2026-11-17 04:58Z): ALL work moved to O
  8bf31a36 ckpt 2609: LAB4b: decisions recorded: ANS60e NOT done (would delay every navigation's g
  b68aca47 ckpt 2608: LAB4a/b: run 8 (44017705030, instrumented app, fixed lab order, fast link) i
  edacce50 ckpt 2607: LAB4a: run 7 (44010606968, instrumented app, hotspot, OLD lab order) CANCELL
  7f6901e6 ckpt 2606: LAB4a: run 6 (44010165696) was CANCELLED after 53 min in the lab step; its a
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
