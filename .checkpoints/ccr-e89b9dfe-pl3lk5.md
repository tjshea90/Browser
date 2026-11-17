# CHECKPOINT 2615 — read me first, then TASKS.md

**Written:** 2026-11-17T09:03:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `cc1424d3` (this checkpoint is the commit after it)

## Just done
LAB5 (owner message 2026-11-17T06:42:30Z: improve the browser as much as possible from the emulated runs, run more emulations if they help, ultra fast / snappy / lightweight, full permission, no prompts, build the release after all research and coding) written into TASKS.md in his words as LAB5a-e BEFORE any code. State found at 09:00Z: the parallel session ccr-a10e07b7-6zxjmi already read run 9 (degraded: emulator restarted, no network, Google A/B all timeouts; start-up block on the emulator = software-GL render sync + 166 ms runtime create) and SHIPPED alpha63 (code 164, back gesture on Android 13+, diagnostics) at 08:32Z; its work is merged into this branch. The release at the end of THIS pass is alpha64 (code 165)

## Do this next
Research first (LAB5a): spawn read-only research agents in parallel (packaging/omni.ja/startup-prof; UI-thread start path; extension per-frame cost; lab-measurable prefs), meanwhile inspect the alpha63 APK (omni.ja stored or deflated? native libs?) and read research/UPDATE_STORAGE.md, EXT_COST.md, COLD_START.md; then design the lab suites; no app code before the evidence

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  cc1424d3 ckpt 2614: alpha63 SHIPPED + recorded (run 44046401335 green, release kestrel-g-v0.3.0-
  12cd7f08 ckpt 2613: alpha63 dispatched ONCE (run_workflow app=gecko full_build=true ref ccr-a10e
  d59a6ddb ckpt 2612: gated kestrel-g-v0.3.0-alpha63 (gecko code 164) and pushed it: handoff self-
  3cdec9fe ckpt 2611: run 9 read: degraded run (emulator restarted, no network, Google A/B all 60 
  ede3c889 ckpt 2610: OWNER RESCHEDULE (usage running out, 2026-11-17 04:58Z): ALL work moved to O
  8bf31a36 ckpt 2609: LAB4b: decisions recorded: ANS60e NOT done (would delay every navigation's g
  b68aca47 ckpt 2608: LAB4a/b: run 8 (44017705030, instrumented app, fixed lab order, fast link) i
  edacce50 ckpt 2607: LAB4a: run 7 (44010606968, instrumented app, hotspot, OLD lab order) CANCELL
  7f6901e6 ckpt 2606: LAB4a: run 6 (44010165696) was CANCELLED after 53 min in the lab step; its a
  c2e8ae15 ckpt 2605: LAB4b: ANS60f DONE in code (NOT testable on the Android 10 lab, compile-chec
```
