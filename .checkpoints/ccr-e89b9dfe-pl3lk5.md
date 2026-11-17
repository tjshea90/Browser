# CHECKPOINT 2618 — read me first, then TASKS.md

**Written:** 2026-11-17T09:17:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `2c0bfa9d` (this checkpoint is the commit after it)

## Just done
LAB5b: more lab tooling committed (no run yet): suite 'sweep' = tools/emu/prefsweep.py (variants from tools/emu/sweep-variants.txt, base + base2 twin = noise floor, rotated rounds, /bench2 ruler, smaps_rollup per process, config read-back; selftest ok, report dry-run ok), debug-only GeckoPrefs override channel debug.kestrel.gp1..gp8, ART profile collection step at the end of every non-pack/sweep lab run (SIGUSR1 + cmd package dump-profiles + profman --dump-classes-and-methods into lab-out/profile/). The 'pack' run (44053244976, A today / B omni stored / C libs in APK / D both) is running; agents: start path, extension, prefs, packaging running

## Do this next
1) read the pack run when done (PACK-REPORT.md); 2) digest the 4 agent reports; 3) write the first sweep-variants.txt from the prefs agent's list and dispatch lab_suite=sweep lab_minutes=5 (rounds); 4) dispatch a daily run to collect the ART profile raw material, convert to geckoapp/src/main/baseline-prof.txt + profileinstaller, A/B in packlab (cmd package compile -m speed-profile)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  96e097e3 ckpt 2617: LAB5b: pref-sweep tooling started: GeckoPrefs.writeConfig reads debug.kestre
  97f31512 ckpt 2616: LAB5b: packaging A/B lab built and wired: geckoapp/build.gradle.kts has two 
  1cf2f4fd ckpt 2615: LAB5 (owner message 2026-11-17T06:42:30Z: improve the browser as much as pos
  cc1424d3 ckpt 2614: alpha63 SHIPPED + recorded (run 44046401335 green, release kestrel-g-v0.3.0-
  12cd7f08 ckpt 2613: alpha63 dispatched ONCE (run_workflow app=gecko full_build=true ref ccr-a10e
  d59a6ddb ckpt 2612: gated kestrel-g-v0.3.0-alpha63 (gecko code 164) and pushed it: handoff self-
  3cdec9fe ckpt 2611: run 9 read: degraded run (emulator restarted, no network, Google A/B all 60 
  ede3c889 ckpt 2610: OWNER RESCHEDULE (usage running out, 2026-11-17 04:58Z): ALL work moved to O
  8bf31a36 ckpt 2609: LAB4b: decisions recorded: ANS60e NOT done (would delay every navigation's g
  b68aca47 ckpt 2608: LAB4a/b: run 8 (44017705030, instrumented app, fixed lab order, fast link) i
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
