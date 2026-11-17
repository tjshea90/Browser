# CHECKPOINT 2606 — read me first, then TASKS.md

**Written:** 2026-11-17T04:30:36Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `ed44ad38` (this checkpoint is the commit after it)

## Just done
LAB4a: run 6 (44010165696) was CANCELLED after 53 min in the lab step; its artifacts survived the cancel and show why: the site tour's last page, Yahoo Finance, loaded (1.7 s) then froze the UI thread 2089 ms with an ANR (435 script errors), and the emulated phone never recovered (security step took 16 min, system_server died during safe-browsing, hard restart, apk-guard failed). The tour's other 9 sites loaded fine, none blank. Lab fixes: Yahoo now runs ALONE and LAST (step heavy-page-yahoo-finance), the security probes run BEFORE the tour, the idle-CPU parser fixed for toybox top (no PID column, 'Threads:' header, app processes only; parser tested on the real text). Run 7 (INSTRUMENTED app, hotspot, old lab order) is the one that matters for stack samples: its Yahoo freeze should carry 'ui slow-at' lines

## Do this next
1) find run 7's id (list_workflow_runs perPage 1 workflow_dispatch) and wait for it (budget 75 min, hotspot: slow); read health-lines.txt for 'ui slow-at', 'engine burst', 'look read-back'; 2) dispatch run 8 = fast link, INSTRUMENTED app, FIXED lab order = the clean baseline (do not queue it before run 7 starts running or it replaces nothing: only one pending slot); 3) decide fixes from the stacks

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  c2e8ae15 ckpt 2605: LAB4b: ANS60f DONE in code (NOT testable on the Android 10 lab, compile-chec
  4ada6834 ckpt 2604: LAB4b: instrumentation step verified: JVM suites :core:test :app:testDebugUn
  d7f8b405 ckpt 2603: LAB3/LAB4a: run 5 (44008297246) READ: all lab fixes held (Features + Securit
  4dc88c0d ckpt 2602: LAB4b step 1 (measure before fixing, evidence rule): app instrumentation onl
  7a95ed47 ckpt 2601: LAB4a: daily.py got two steps: idle-cpu-60-seconds (top -H + dumpsys cpuinfo
  0f3bfce1 ckpt 2600: LAB4 (owner message 63: do the other runs that should be done, then implemen
  dd595cbe ckpt 2599: LAB3e: lab run 4 (44005894797) READ: Security GREEN 16 pass / 0 fail, camera
  025bf31d ckpt 2598: LAB3f: research/LAB3_IMPROVEMENTS.md now opens with the measured table of la
  2d31626a ckpt 2597: LAB3e: lab run 3 (44003337190) READ: all four lab false positives gone, Secu
  d06f3696 ckpt 2596: LAB3e: lab run 3 DISPATCHED ONCE: android.yml run 44003337190 (daily, head b
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
