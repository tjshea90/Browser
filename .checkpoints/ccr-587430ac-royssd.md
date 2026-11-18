# CHECKPOINT 2679 — read me first, then TASKS.md

**Written:** 2026-11-18T06:19:49Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `7ed8e669` (this checkpoint is the commit after it)

## Just done
MT1 coded (mini panel Tabs button + tab tray, TabTrayModel, host wiring, 9 Robolectric tests, all 178 ui tests green). UC1 moved into the REAL address bar (BottomDock + ui/CopyChip, DockCopyTest 6); the unused Toolbar class restored. TP1 done. Lab hooks added (minipanel, minitabs, previews, opengrid, switchtab) + Diagnostics line Tab previews. Lab run 44248671431 not read yet.

## Do this next
1) read lab 44248671431 (list_workflow_run_artifacts); 2) write soak.py tb_scene for TP1/UC1/MT1 using the new lab commands and add suite tb to android.yml + emu-lab.yml; 3) full JVM suite alone; 4) lab run covering new+tb; 5) ship alpha70 (version 171 already in the tree, rewrite ALPHA70 note to add TP1/UC1/MT1)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  bd1bb9e4 ckpt 2678: UC1 coded + tested: Copy button inside the address pill (Toolbar.copyButton)
  133164bc ckpt 2677: TP1 coded + tested: Tab.url setter drops the old page's preview, ThumbStore 
  337a3f3b ckpt 2676: Owner messages TP1 (wrong tab preview), UC1 (copy button on the url bar), MT
  3fcae0e0 ckpt 2675: Lab run 44248671431 (suite new, head d868b655) DISPATCHED 06:04Z. alpha70 pr
  d868b655 ckpt 2674: Lab run 44245067663 read: memory evidence WORKS on Android 10 (per-process R
  220d6189 ckpt 2673: Container restarted at ~05:52Z (a background wait was lost; repo and checkpo
  c6dba2a4 ckpt 2672: Lab run 44245067663 (suite new = lr freeze/kill + vs incl. keep + ql) DISPAT
  5ce8de0c ckpt 2671: CR1g memory evidence in the Last run note (VmRSS of every app process, MemFr
  97f69909 ckpt 2670: Owner answered CR1f-c: the app vanished to the home screen = process death, 
  1ac8a776 ckpt 2669: Owner message VS2 written into TASKS (doraby: vidsrc + 111movies find no dow
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
