# CHECKPOINT 2681 — read me first, then TASKS.md

**Written:** 2026-11-18T06:25:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `464dcf17` (this checkpoint is the commit after it)

## Just done
PV1 coded: CurtainRule (core, 3 tests) + curtain view over the shared page view, up on a swap to an unpainted session or the other tab kind, down at first paint; lab checks in scene tb. TP1/UC1/MT1 coded earlier. jvm-suite8 started before the last edits (rerun alone before shipping). Lab 44248671431 unread.

## Do this next
1) read lab 44248671431 artifacts; 2) rerun the FULL JVM suite alone (core app geckoapp) after all edits; 3) web tests 616; 4) lab: suite tb (and new if lr/vs were not green); 5) ship alpha70 (version 171, note ALPHA70 has items 1-7)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M docs/handoff/ALPHA70_RELEASE_NOTE.txt
     M research/TP1_UC1.md

## Last ten checkpoints
```
  51f3e413 ckpt 2680: Owner message PV1 written into TASKS (new private tab flashed an old regular
  92b293d1 ckpt 2679: MT1 coded (mini panel Tabs button + tab tray, TabTrayModel, host wiring, 9 R
  bd1bb9e4 ckpt 2678: UC1 coded + tested: Copy button inside the address pill (Toolbar.copyButton)
  133164bc ckpt 2677: TP1 coded + tested: Tab.url setter drops the old page's preview, ThumbStore 
  337a3f3b ckpt 2676: Owner messages TP1 (wrong tab preview), UC1 (copy button on the url bar), MT
  3fcae0e0 ckpt 2675: Lab run 44248671431 (suite new, head d868b655) DISPATCHED 06:04Z. alpha70 pr
  d868b655 ckpt 2674: Lab run 44245067663 read: memory evidence WORKS on Android 10 (per-process R
  220d6189 ckpt 2673: Container restarted at ~05:52Z (a background wait was lost; repo and checkpo
  c6dba2a4 ckpt 2672: Lab run 44245067663 (suite new = lr freeze/kill + vs incl. keep + ql) DISPAT
  5ce8de0c ckpt 2671: CR1g memory evidence in the Last run note (VmRSS of every app process, MemFr
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
