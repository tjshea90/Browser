# CHECKPOINT 2680 — read me first, then TASKS.md

**Written:** 2026-11-18T06:22:50Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `8a29bc96` (this checkpoint is the commit after it)

## Just done
Owner message PV1 written into TASKS (new private tab flashed an old regular tab's page when loading). jvm-suite8 running; lab 44248671431 unread; alpha70 not shipped; TP1/UC1/MT1 coded + tested.

## Do this next
PV1a: read BrowserActivity navigateTo / ensureSession / attachContent / show / RestoreOverlay and GeckoView session switching; find what shows the previous tab's frame

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  92b293d1 ckpt 2679: MT1 coded (mini panel Tabs button + tab tray, TabTrayModel, host wiring, 9 R
  bd1bb9e4 ckpt 2678: UC1 coded + tested: Copy button inside the address pill (Toolbar.copyButton)
  133164bc ckpt 2677: TP1 coded + tested: Tab.url setter drops the old page's preview, ThumbStore 
  337a3f3b ckpt 2676: Owner messages TP1 (wrong tab preview), UC1 (copy button on the url bar), MT
  3fcae0e0 ckpt 2675: Lab run 44248671431 (suite new, head d868b655) DISPATCHED 06:04Z. alpha70 pr
  d868b655 ckpt 2674: Lab run 44245067663 read: memory evidence WORKS on Android 10 (per-process R
  220d6189 ckpt 2673: Container restarted at ~05:52Z (a background wait was lost; repo and checkpo
  c6dba2a4 ckpt 2672: Lab run 44245067663 (suite new = lr freeze/kill + vs incl. keep + ql) DISPAT
  5ce8de0c ckpt 2671: CR1g memory evidence in the Last run note (VmRSS of every app process, MemFr
  97f69909 ckpt 2670: Owner answered CR1f-c: the app vanished to the home screen = process death, 
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
