# CHECKPOINT 2608 — read me first, then TASKS.md

**Written:** 2026-11-17T04:57:03Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `14a36a24` (this checkpoint is the commit after it)

## Just done
LAB4a/b: run 8 (44017705030, instrumented app, fixed lab order, fast link) is the CLEAN BASELINE: written to research/lab-baseline.json and docs/handoff/LABREPORT_0.3.0-alpha62.md (Stability / Security 17-0 / Features / Memory GREEN, Speed AMBER = the 0.9-1.4 s start-up block). Stack samples + burst timing + read-back timings REFUTE candidates 2 and 3 as designed (read-back 1-10 ms; the burst costs 56 ms; the block is GeckoRuntime.create on the UI thread + Android's first-draw renderer sync); idle audit works now: 5.4 % CPU idle, RenderThread 3.8 % = the owner-requested Neon pulse (a choice for the owner). Added for run 9: lab-only 40 ms start-up profiler in Health (only when Health is mirrored to logcat = debug builds) and the Shields on/off A/B on the Google results page; compile + selftest ok

## Do this next
Dispatch run 9 ONCE (fast link, same inputs) now; read it (health-lines 'ui prof t=' lines name where the 1 s start block goes; shields_ab_google decides whether Google needs a rule); implement only what the data supports; ANS60e (JS, web test) is the one open lab-found item with a ready design; then JVM + web tests, after-run vs research/lab-baseline.json, version bump + ship.sh gecko + ONE dispatch + record-release.sh

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M research/LAB3_IMPROVEMENTS.md
    ?? docs/handoff/LABREPORT_0.3.0-alpha62.md
    ?? research/lab-baseline.json

## Last ten checkpoints
```
  edacce50 ckpt 2607: LAB4a: run 7 (44010606968, instrumented app, hotspot, OLD lab order) CANCELL
  7f6901e6 ckpt 2606: LAB4a: run 6 (44010165696) was CANCELLED after 53 min in the lab step; its a
  c2e8ae15 ckpt 2605: LAB4b: ANS60f DONE in code (NOT testable on the Android 10 lab, compile-chec
  4ada6834 ckpt 2604: LAB4b: instrumentation step verified: JVM suites :core:test :app:testDebugUn
  d7f8b405 ckpt 2603: LAB3/LAB4a: run 5 (44008297246) READ: all lab fixes held (Features + Securit
  4dc88c0d ckpt 2602: LAB4b step 1 (measure before fixing, evidence rule): app instrumentation onl
  7a95ed47 ckpt 2601: LAB4a: daily.py got two steps: idle-cpu-60-seconds (top -H + dumpsys cpuinfo
  0f3bfce1 ckpt 2600: LAB4 (owner message 63: do the other runs that should be done, then implemen
  dd595cbe ckpt 2599: LAB3e: lab run 4 (44005894797) READ: Security GREEN 16 pass / 0 fail, camera
  025bf31d ckpt 2598: LAB3f: research/LAB3_IMPROVEMENTS.md now opens with the measured table of la
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
