# CHECKPOINT 2684 — read me first, then TASKS.md

**Written:** 2026-11-18T06:37:36Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `d0e7d41d` (this checkpoint is the commit after it)

## Just done
Lab run 44252661300 (suite new2 = tb + lastrun_round, head d0e7d41d) DISPATCHED 06:37Z. alpha70 prepared (171, note items 1-7), NOT shipped. Full JVM suite 9 and web 616 green before this dispatch.

## Do this next
WAIT for run 44252661300 (poll list_workflow_run_artifacts every ~8 min; the ffmpeg step takes ~12 min), read report.md: the tb row 'previews-copy-tabs' checks and the lr cases; fix any app/scene fault (one lab at a time); when green: bash ship.sh gecko "$(cat docs/handoff/ALPHA70_RELEASE_NOTE.txt)", ONE dispatch app=gecko full_build=true with the same note, verify tag kestrel-g-v0.3.0-alpha70 + jobs + release, bash tools/record-release.sh <tag> <same note>, answer owner (TP1, UC1, MT1, PV1, VS2, crash evidence, VS1e)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d0e7d41d ckpt 2683: Lab run 44248671431 READ: freeze scene proves CR1f on a real Android 10 (SCR
  6617fbe2 ckpt 2682: Full JVM suite 9 GREEN (rc=0) + web 616/616 with TP1, UC1 (dock), MT1, PV1 c
  3dda7232 ckpt 2681: PV1 coded: CurtainRule (core, 3 tests) + curtain view over the shared page v
  51f3e413 ckpt 2680: Owner message PV1 written into TASKS (new private tab flashed an old regular
  92b293d1 ckpt 2679: MT1 coded (mini panel Tabs button + tab tray, TabTrayModel, host wiring, 9 R
  bd1bb9e4 ckpt 2678: UC1 coded + tested: Copy button inside the address pill (Toolbar.copyButton)
  133164bc ckpt 2677: TP1 coded + tested: Tab.url setter drops the old page's preview, ThumbStore 
  337a3f3b ckpt 2676: Owner messages TP1 (wrong tab preview), UC1 (copy button on the url bar), MT
  3fcae0e0 ckpt 2675: Lab run 44248671431 (suite new, head d868b655) DISPATCHED 06:04Z. alpha70 pr
  d868b655 ckpt 2674: Lab run 44245067663 read: memory evidence WORKS on Android 10 (per-process R
```
