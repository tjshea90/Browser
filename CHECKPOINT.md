# CHECKPOINT 2605 — read me first, then TASKS.md

**Written:** 2026-11-17T03:39:03Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `1b63af79` (this checkpoint is the commit after it)

## Just done
LAB4b: ANS60f DONE in code (NOT testable on the Android 10 lab, compile-checked): NativePlayerActivity registers an OnBackInvokedCallback on API 33+ (quality list closes first, then the player finishes; unregistered in onDestroy), because with targetSdk 36 the framework no longer calls onBackPressed. Still waiting for run 6 (baseline) and run 7 (instrumented, hotspot); the start-up and read-back fixes wait for their stack samples

## Do this next
At the wake-up (03:48Z) read run 6; then run 7 stacks; fixes after

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  4ada6834 ckpt 2604: LAB4b: instrumentation step verified: JVM suites :core:test :app:testDebugUn
  d7f8b405 ckpt 2603: LAB3/LAB4a: run 5 (44008297246) READ: all lab fixes held (Features + Securit
  4dc88c0d ckpt 2602: LAB4b step 1 (measure before fixing, evidence rule): app instrumentation onl
  7a95ed47 ckpt 2601: LAB4a: daily.py got two steps: idle-cpu-60-seconds (top -H + dumpsys cpuinfo
  0f3bfce1 ckpt 2600: LAB4 (owner message 63: do the other runs that should be done, then implemen
  dd595cbe ckpt 2599: LAB3e: lab run 4 (44005894797) READ: Security GREEN 16 pass / 0 fail, camera
  025bf31d ckpt 2598: LAB3f: research/LAB3_IMPROVEMENTS.md now opens with the measured table of la
  2d31626a ckpt 2597: LAB3e: lab run 3 (44003337190) READ: all four lab false positives gone, Secu
  d06f3696 ckpt 2596: LAB3e: lab run 3 DISPATCHED ONCE: android.yml run 44003337190 (daily, head b
  b52ed4c3 ckpt 2595: LAB3e: lab run 2 (43997607118) READ. The whole pipeline works on the real em
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
