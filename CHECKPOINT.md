# CHECKPOINT 2617 — read me first, then TASKS.md

**Written:** 2026-11-17T09:14:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `9abc7330` (this checkpoint is the commit after it)

## Just done
LAB5b: pref-sweep tooling started: GeckoPrefs.writeConfig reads debug.kestrel.gp1..gp8 (DEBUG builds only, 'name=value', 'name=-' removes) and logs 'engine sweep'; /bench2 ruler page (8 kinds of work x 3 passes: oo, str, json, alloc+gcmax, dom, layout, canvas, sorts) in soaklab.py + packlab server; compiled. Packaging run ('pack' suite) dispatched at ~09:35Z (id: newest workflow_dispatch). Agents still running

## Do this next
Next: read the pack run (PACK-REPORT.md in artifact emulator-lab-text: install sizes, cold starts A/B/C/D, per-process Pss anon vs file) -> decide omni stored / libs; write tools/emu/prefsweep.py (variants x rounds interleaved, setprop gp1..gp8, force-stop, start /bench2, read 'bench2' reports + smaps_rollup + runtime-ready) once the prefs agent's candidate list arrives

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  97f31512 ckpt 2616: LAB5b: packaging A/B lab built and wired: geckoapp/build.gradle.kts has two 
  1cf2f4fd ckpt 2615: LAB5 (owner message 2026-11-17T06:42:30Z: improve the browser as much as pos
  cc1424d3 ckpt 2614: alpha63 SHIPPED + recorded (run 44046401335 green, release kestrel-g-v0.3.0-
  12cd7f08 ckpt 2613: alpha63 dispatched ONCE (run_workflow app=gecko full_build=true ref ccr-a10e
  d59a6ddb ckpt 2612: gated kestrel-g-v0.3.0-alpha63 (gecko code 164) and pushed it: handoff self-
  3cdec9fe ckpt 2611: run 9 read: degraded run (emulator restarted, no network, Google A/B all 60 
  ede3c889 ckpt 2610: OWNER RESCHEDULE (usage running out, 2026-11-17 04:58Z): ALL work moved to O
  8bf31a36 ckpt 2609: LAB4b: decisions recorded: ANS60e NOT done (would delay every navigation's g
  b68aca47 ckpt 2608: LAB4a/b: run 8 (44017705030, instrumented app, fixed lab order, fast link) i
  edacce50 ckpt 2607: LAB4a: run 7 (44010606968, instrumented app, hotspot, OLD lab order) CANCELL
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
