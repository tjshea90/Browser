# CHECKPOINT 2677 — read me first, then TASKS.md

**Written:** 2026-11-18T06:09:06Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `9c8fdae8` (this checkpoint is the commit after it)

## Just done
TP1 coded + tested: Tab.url setter drops the old page's preview, ThumbStore keeps a .page record and refuses a mismatch (old unrecorded files are not trusted), captureThumb keeps a picture only for the same page / same load / settled (core ThumbRule + 10 tests green). Lab run 44248671431 still running.

## Do this next
1) read lab 44248671431 when its artifacts exist and finish alpha70; 2) UC1 copy button: read how the address bar handles a press (UrlBar / onUrlFocus / select-all) and design the floating Copy row; 3) MT1 design note research/MT1_MINI_TABS.md then build; alpha70 must also include TP1/UC1/MT1 or ship alpha70 first and alpha71 after

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  337a3f3b ckpt 2676: Owner messages TP1 (wrong tab preview), UC1 (copy button on the url bar), MT
  3fcae0e0 ckpt 2675: Lab run 44248671431 (suite new, head d868b655) DISPATCHED 06:04Z. alpha70 pr
  d868b655 ckpt 2674: Lab run 44245067663 read: memory evidence WORKS on Android 10 (per-process R
  220d6189 ckpt 2673: Container restarted at ~05:52Z (a background wait was lost; repo and checkpo
  c6dba2a4 ckpt 2672: Lab run 44245067663 (suite new = lr freeze/kill + vs incl. keep + ql) DISPAT
  5ce8de0c ckpt 2671: CR1g memory evidence in the Last run note (VmRSS of every app process, MemFr
  97f69909 ckpt 2670: Owner answered CR1f-c: the app vanished to the home screen = process death, 
  1ac8a776 ckpt 2669: Owner message VS2 written into TASKS (doraby: vidsrc + 111movies find no dow
  676dd260 ckpt 2668: alpha69 RELEASED: tag kestrel-g-v0.3.0-alpha69, run 44239780214 all jobs gre
  cf070608 ckpt 2667: alpha69 DISPATCHED ONCE at 04:47Z: android.yml run 44239780214 (run_number 2
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
