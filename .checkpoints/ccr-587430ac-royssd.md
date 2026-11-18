# CHECKPOINT 2683 — read me first, then TASKS.md

**Written:** 2026-11-18T06:36:58Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `b64f5ad9` (this checkpoint is the commit after it)

## Just done
Lab run 44248671431 READ: freeze scene proves CR1f on a real Android 10 (SCREEN THREAD WAS FROZEN + stall stack + per-process memory), vs all ok incl. keep (capture) / click / mse / msel / msel0 held, ql ok. 2 scene faults fixed (lr check read the earlier-run block; one start no-engine = overloaded emulator). New suite new2 = tb (TP1 previews, UC1 copy, MT1 tray, PV1 curtain) + lastrun_round.

## Do this next
dispatch ONE lab: android.yml app=lab lab_suite=new2 lab_profile=standard lab_net=full lab_minutes=5 on ccr-587430ac-royssd (head = latest ckpt); read report.md: tb checks (TP1 wrong=0 at every step, UC1 copyurl visible+exact, MT1 tray rows/switch/close/new, PV1 curtain-up UNPAINTED private=true then down) + lr 3 cases; fix faults; then ship alpha70 (tree is 171 with note items 1-7; ship.sh gecko with cat of docs/handoff/ALPHA70_RELEASE_NOTE.txt)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6617fbe2 ckpt 2682: Full JVM suite 9 GREEN (rc=0) + web 616/616 with TP1, UC1 (dock), MT1, PV1 c
  3dda7232 ckpt 2681: PV1 coded: CurtainRule (core, 3 tests) + curtain view over the shared page v
  51f3e413 ckpt 2680: Owner message PV1 written into TASKS (new private tab flashed an old regular
  92b293d1 ckpt 2679: MT1 coded (mini panel Tabs button + tab tray, TabTrayModel, host wiring, 9 R
  bd1bb9e4 ckpt 2678: UC1 coded + tested: Copy button inside the address pill (Toolbar.copyButton)
  133164bc ckpt 2677: TP1 coded + tested: Tab.url setter drops the old page's preview, ThumbStore 
  337a3f3b ckpt 2676: Owner messages TP1 (wrong tab preview), UC1 (copy button on the url bar), MT
  3fcae0e0 ckpt 2675: Lab run 44248671431 (suite new, head d868b655) DISPATCHED 06:04Z. alpha70 pr
  d868b655 ckpt 2674: Lab run 44245067663 read: memory evidence WORKS on Android 10 (per-process R
  220d6189 ckpt 2673: Container restarted at ~05:52Z (a background wait was lost; repo and checkpo
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
