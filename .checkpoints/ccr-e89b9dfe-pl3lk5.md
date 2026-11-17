# CHECKPOINT 2601 — read me first, then TASKS.md

**Written:** 2026-11-17T03:25:51Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `a09de4bd` (this checkpoint is the commit after it)

## Just done
LAB4a: daily.py got two steps: idle-cpu-60-seconds (top -H + dumpsys cpuinfo, raw text kept to check the parser, candidate 5) and site-tour-ten-public-sites (10 public sites, Yahoo last, blank first-screen check via screen_stats, LAB2d/LAB2f); buildreport.py got the idle-CPU and blank-first-screen findings. Keyboard-resize (candidate 7) is NOT measurable in the lab: the emulator has a hardware keyboard so no soft keyboard opens. Run 5 (44008297246) is finishing (wake-up trig_01M81FL9EBuVKoGbGeoGxuD1 03:28Z)

## Do this next
1) dispatch run 6 = fast-link daily with the tour (the real BASELINE) now (it queues behind run 5), 2) when it starts dispatch run 7 = lab_net=hotspot (pending slot), 3) read run 5 only to confirm the five lab fixes, 4) meanwhile implement LAB4b: read BrowserActivity.kt around lines 521-531 (start-up burst) and leavingPage/captureThumb/readBack (~1451, ~1966-2000) and TabDelegates.kt:143

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M tools/emu/daily.py

## Last ten checkpoints
```
  0f3bfce1 ckpt 2600: LAB4 (owner message 63: do the other runs that should be done, then implemen
  dd595cbe ckpt 2599: LAB3e: lab run 4 (44005894797) READ: Security GREEN 16 pass / 0 fail, camera
  025bf31d ckpt 2598: LAB3f: research/LAB3_IMPROVEMENTS.md now opens with the measured table of la
  2d31626a ckpt 2597: LAB3e: lab run 3 (44003337190) READ: all four lab false positives gone, Secu
  d06f3696 ckpt 2596: LAB3e: lab run 3 DISPATCHED ONCE: android.yml run 44003337190 (daily, head b
  b52ed4c3 ckpt 2595: LAB3e: lab run 2 (43997607118) READ. The whole pipeline works on the real em
  e61155d2 ckpt 2594: LAB3f: the speed ledger + 8 NEW ranked candidates are saved in research/LAB3
  a3b36287 ckpt 2593: LAB3 docs: research/LAB3_DAILY.md (how to start, what is matched / not, the 
  a1a955e8 ckpt 2592: LAB3e: lab run 2 DISPATCHED ONCE: android.yml run 43997607118 (app=lab lab_s
  c59a2c9a ckpt 2591: LAB3 run 1 (43997001918) failed at the Gradle step: Mozilla publishes NO gec
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
