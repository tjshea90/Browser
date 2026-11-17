# CHECKPOINT 2602 — read me first, then TASKS.md

**Written:** 2026-11-17T03:29:36Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `b7c0cb3f` (this checkpoint is the commit after it)

## Just done
LAB4b step 1 (measure before fixing, evidence rule): app instrumentation only, behaviour unchanged, both modules compile (platform android-37.0 installed with sdkmanager + gradle online: this container had no cache): Health watchdog now logs 'ui slow-at <ms> <top 9 frames>' (up to 3 per block, 24 per process) during the 0.7-2 s blocks that used to leave no stack; BrowserActivity start-up burst logs 'engine burst runtime+orientation=.. executor=.. warmUp=.. shields-install=..'; every readBack logs 'look read-back <why> <ms>'. Run-4 Health lines show the start block runs from +0.5 s to +1.9 s, BEFORE the post that logs runtime-create, so the hog may be earlier first-frame work, not the runtime call: the stack samples decide. Run 5 (44008297246) finishing, run 6 (baseline with tour, OLD app) queued

## Do this next
1) check runs: 44008297246 (run 5) and the queued run 6; 2) when run 6 has STARTED dispatch run 7 = the INSTRUMENTED app on lab_net=hotspot (head must include this commit); 3) read run 6 (baseline, old app) and run 7 for the 'ui slow-at' stacks and the 'engine burst' line, THEN decide the start-up fix; 4) in parallel run ./gradlew :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest (online) and cd web-tests && npm test

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  7a95ed47 ckpt 2601: LAB4a: daily.py got two steps: idle-cpu-60-seconds (top -H + dumpsys cpuinfo
  0f3bfce1 ckpt 2600: LAB4 (owner message 63: do the other runs that should be done, then implemen
  dd595cbe ckpt 2599: LAB3e: lab run 4 (44005894797) READ: Security GREEN 16 pass / 0 fail, camera
  025bf31d ckpt 2598: LAB3f: research/LAB3_IMPROVEMENTS.md now opens with the measured table of la
  2d31626a ckpt 2597: LAB3e: lab run 3 (44003337190) READ: all four lab false positives gone, Secu
  d06f3696 ckpt 2596: LAB3e: lab run 3 DISPATCHED ONCE: android.yml run 44003337190 (daily, head b
  b52ed4c3 ckpt 2595: LAB3e: lab run 2 (43997607118) READ. The whole pipeline works on the real em
  e61155d2 ckpt 2594: LAB3f: the speed ledger + 8 NEW ranked candidates are saved in research/LAB3
  a3b36287 ckpt 2593: LAB3 docs: research/LAB3_DAILY.md (how to start, what is matched / not, the 
  a1a955e8 ckpt 2592: LAB3e: lab run 2 DISPATCHED ONCE: android.yml run 43997607118 (app=lab lab_s
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
