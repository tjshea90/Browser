# CHECKPOINT 2687 — read me first, then TASKS.md

**Written:** 2026-11-18T06:50:02Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `30a0825b` (this checkpoint is the commit after it)

## Just done
alpha70 GATED locally (ship.sh rc=0: handoff self-test, unit suites, versionCode 171 > 170) and pushed (ckpt 2686) but NOT dispatched. Lab run 44254127252 (suite tb, head caf37927) is running: it must finish FIRST (a release dispatch would cancel it).

## Do this next
1) WAIT for lab 44254127252 (list_workflow_run_artifacts; ~20 min, ffmpeg step 12 min); read report.md; expect 0 issues for TP1 / UC1 / MT1 / PV1; fix any app fault (then re-run ship.sh); 2) THEN ONE dispatch: mcp__github__actions_run_trigger run_workflow android.yml ref ccr-587430ac-royssd inputs app=gecko full_build=true note=<contents of docs/handoff/ALPHA70_RELEASE_NOTE.txt, identical>; 3) verify tag kestrel-g-v0.3.0-alpha70 (git ls-remote --tags origin), all jobs green, mcp__github__get_release_by_tag; 4) bash tools/record-release.sh kestrel-g-v0.3.0-alpha70 <same note>; 5) answer owner in plain words (TP1 previews, UC1 copy, MT1 tabs in the panel, PV1 private flash, VS2 capture, crash evidence CR1f/g, VS1e) with what is NOT proven on the tablet; release link on its own line

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  30a0825b ckpt 2686: gated kestrel-g-v0.3.0-alpha70 (gecko code 171) and pushed it: handoff self-
  caf37927 ckpt 2685: Lab run 44252661300 (new2) READ: the app behaved right on every check (UC1 v
  e5fc78d7 ckpt 2684: Lab run 44252661300 (suite new2 = tb + lastrun_round, head d0e7d41d) DISPATC
  d0e7d41d ckpt 2683: Lab run 44248671431 READ: freeze scene proves CR1f on a real Android 10 (SCR
  6617fbe2 ckpt 2682: Full JVM suite 9 GREEN (rc=0) + web 616/616 with TP1, UC1 (dock), MT1, PV1 c
  3dda7232 ckpt 2681: PV1 coded: CurtainRule (core, 3 tests) + curtain view over the shared page v
  51f3e413 ckpt 2680: Owner message PV1 written into TASKS (new private tab flashed an old regular
  92b293d1 ckpt 2679: MT1 coded (mini panel Tabs button + tab tray, TabTrayModel, host wiring, 9 R
  bd1bb9e4 ckpt 2678: UC1 coded + tested: Copy button inside the address pill (Toolbar.copyButton)
  133164bc ckpt 2677: TP1 coded + tested: Tab.url setter drops the old page's preview, ThumbStore 
```
