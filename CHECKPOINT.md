# CHECKPOINT 2604 — read me first, then TASKS.md

**Written:** 2026-11-17T03:38:11Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `d7f8b405` (this checkpoint is the commit after it)

## Just done
LAB4b: instrumentation step verified: JVM suites :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest = 1770 tests, 0 failures, 0 errors, 9 skipped (gradle exit 0); web tests 579 / 579 pass; both app modules compile. Run 6 (44010165696 baseline, old app) and run 7 (instrumented app, hotspot) still in the lab queue; wake-up trig_01U2KpAG37HX65UUc4HdKmE1 at 03:48Z

## Do this next
At the wake-up read run 6 (jobs, artifact emulator-lab-text, buildreport --notes, write research/lab-baseline.json + docs/handoff/LABREPORT_0.3.0-alpha62.md from run 6 only); then run 7 for 'ui slow-at' / 'engine burst' / 'look read-back' lines in health-lines.txt; THEN choose the fixes (start-up block: whatever the stack names; candidate 2 only if read-back durations are meaningful); optional low-risk extra: ANS60f (OnBackInvokedCallback in NativePlayerActivity for API 33+)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d7f8b405 ckpt 2603: LAB3/LAB4a: run 5 (44008297246) READ: all lab fixes held (Features + Securit
  4dc88c0d ckpt 2602: LAB4b step 1 (measure before fixing, evidence rule): app instrumentation onl
  7a95ed47 ckpt 2601: LAB4a: daily.py got two steps: idle-cpu-60-seconds (top -H + dumpsys cpuinfo
  0f3bfce1 ckpt 2600: LAB4 (owner message 63: do the other runs that should be done, then implemen
  dd595cbe ckpt 2599: LAB3e: lab run 4 (44005894797) READ: Security GREEN 16 pass / 0 fail, camera
  025bf31d ckpt 2598: LAB3f: research/LAB3_IMPROVEMENTS.md now opens with the measured table of la
  2d31626a ckpt 2597: LAB3e: lab run 3 (44003337190) READ: all four lab false positives gone, Secu
  d06f3696 ckpt 2596: LAB3e: lab run 3 DISPATCHED ONCE: android.yml run 44003337190 (daily, head b
  b52ed4c3 ckpt 2595: LAB3e: lab run 2 (43997607118) READ. The whole pipeline works on the real em
  e61155d2 ckpt 2594: LAB3f: the speed ledger + 8 NEW ranked candidates are saved in research/LAB3
```
