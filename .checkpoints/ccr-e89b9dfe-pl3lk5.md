# CHECKPOINT 2607 — read me first, then TASKS.md

**Written:** 2026-11-17T04:31:13Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `7f6901e6` (this checkpoint is the commit after it)

## Just done
LAB4a: run 7 (44010606968, instrumented app, hotspot, OLD lab order) CANCELLED 12 min in: it would hit the same Yahoo ANR and takes over an hour on the hotspot shape. Run 8 DISPATCHED ONCE instead: fast link, INSTRUMENTED app (stack samples + engine burst + read-back timings), FIXED lab order (security before the tour, Yahoo alone and last, idle parser fixed) = the clean baseline; ~25-30 min, wake-up trig_01KvBgj78V7bqMyeh5w6GVxo 04:53Z. Heartbeat trig_01K9rvGwTaZNy2NbUQeBeWMg still active (2-hourly)

## Do this next
At 04:53Z read run 8 (id: list_workflow_runs perPage 1 filter workflow_dispatch): download emulator-lab-text; grep 'ui slow-at|engine burst|look read-back' in health-lines.txt; name the three hogs (cold-start block, long-article block, Yahoo block) from the frames; report with --notes; write research/lab-baseline.json + docs/handoff/LABREPORT_0.3.0-alpha62.md; then implement the fixes the stacks justify (LAB4b step 2), tests, after-run, ship

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  7f6901e6 ckpt 2606: LAB4a: run 6 (44010165696) was CANCELLED after 53 min in the lab step; its a
  c2e8ae15 ckpt 2605: LAB4b: ANS60f DONE in code (NOT testable on the Android 10 lab, compile-chec
  4ada6834 ckpt 2604: LAB4b: instrumentation step verified: JVM suites :core:test :app:testDebugUn
  d7f8b405 ckpt 2603: LAB3/LAB4a: run 5 (44008297246) READ: all lab fixes held (Features + Securit
  4dc88c0d ckpt 2602: LAB4b step 1 (measure before fixing, evidence rule): app instrumentation onl
  7a95ed47 ckpt 2601: LAB4a: daily.py got two steps: idle-cpu-60-seconds (top -H + dumpsys cpuinfo
  0f3bfce1 ckpt 2600: LAB4 (owner message 63: do the other runs that should be done, then implemen
  dd595cbe ckpt 2599: LAB3e: lab run 4 (44005894797) READ: Security GREEN 16 pass / 0 fail, camera
  025bf31d ckpt 2598: LAB3f: research/LAB3_IMPROVEMENTS.md now opens with the measured table of la
  2d31626a ckpt 2597: LAB3e: lab run 3 (44003337190) READ: all four lab false positives gone, Secu
```
