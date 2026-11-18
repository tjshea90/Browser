# CHECKPOINT 2685 — read me first, then TASKS.md

**Written:** 2026-11-18T06:48:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `a8b08fa5` (this checkpoint is the commit after it)

## Just done
Lab run 44252661300 (new2) READ: the app behaved right on every check (UC1 visible+exact chars=31; MT1 tray rows=5, switch current=0 tray=false, close tabs=4 tray=true, new tabs=5 current=4 keys=true; TP1 previews all ok=true, wrong 0 at every step; PV1 curtain-up UNPAINTED private=true then down; lr 3 cases ok). 7 'failures' were MY scene comparing True vs the app's lowercase true, and PV1's switch back went to the empty new-tab page (no session to put on the view). Scene fixed (lower-case compare, switchtab 1).

## Do this next
dispatch ONE lab lab_suite=tb on ccr-587430ac-royssd, expect 0 issues; then ship alpha70: bash ship.sh gecko with the note in docs/handoff/ALPHA70_RELEASE_NOTE.txt, ONE dispatch app=gecko full_build=true, verify tag kestrel-g-v0.3.0-alpha70, record-release, answer owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e5fc78d7 ckpt 2684: Lab run 44252661300 (suite new2 = tb + lastrun_round, head d0e7d41d) DISPATC
  d0e7d41d ckpt 2683: Lab run 44248671431 READ: freeze scene proves CR1f on a real Android 10 (SCR
  6617fbe2 ckpt 2682: Full JVM suite 9 GREEN (rc=0) + web 616/616 with TP1, UC1 (dock), MT1, PV1 c
  3dda7232 ckpt 2681: PV1 coded: CurtainRule (core, 3 tests) + curtain view over the shared page v
  51f3e413 ckpt 2680: Owner message PV1 written into TASKS (new private tab flashed an old regular
  92b293d1 ckpt 2679: MT1 coded (mini panel Tabs button + tab tray, TabTrayModel, host wiring, 9 R
  bd1bb9e4 ckpt 2678: UC1 coded + tested: Copy button inside the address pill (Toolbar.copyButton)
  133164bc ckpt 2677: TP1 coded + tested: Tab.url setter drops the old page's preview, ThumbStore 
  337a3f3b ckpt 2676: Owner messages TP1 (wrong tab preview), UC1 (copy button on the url bar), MT
  3fcae0e0 ckpt 2675: Lab run 44248671431 (suite new, head d868b655) DISPATCHED 06:04Z. alpha70 pr
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
