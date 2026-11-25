# CHECKPOINT 3321 — read me first, then TASKS.md

**Written:** 2026-11-25T02:34:23Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `c64696f3` (this checkpoint is the commit after it)

## Just done
OPT1c: lab opt v14 dispatched (run 45533868639, head c64696f3)

## Do this next
Wait for run 45533868639 (about 02:55Z; a send_later wake is set; do NOT cancel while a step hangs). Then list_workflow_jobs, get_job_logs of 'lab / lab' (python3 -B -I parse: live:/opt/KestrelRestart/scene/ISSUE and DeadSystemException). Expect KestrelRestart lines for every restart, guard boot=1 after each, a finished last.json. If the system dies again after a quiet restart: option A the lab runs the opt round on an emulator without GPU help (EMU gpu mode off/guest via emu-lab.yml), option B the old process exits by itself instead of being killed. If it finished: Flavor.hasAutoTune=true in GeckoApp.kt, fill RESULT_PLACEHOLDER in docs/handoff/ALPHA107_RELEASE_NOTE.txt, bump geckoapp/build.gradle.kts 0.3.0-alpha107 / code 208, bash tools/sync-ext-version.sh, ship.sh gecko, ONE dispatch app=gecko full_build=true, get_release_by_tag, tools/record-release.sh, tick TASKS, answer the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  c64696f3 ckpt 3320: OPT1c lab v13 read: clean up to the first experiment's restart, then the emu
  d05a3d57 ckpt 3318: OPT1c: lab opt v13 dispatched (run 45530873833, head 86130d11)
  86130d11 ckpt 3317: OPT1c lab v12 read: baseline fine, run stops at the first LIVE experiment (p
  ac1b71e1 ckpt 3314: OPT1c: lab opt v12 dispatched (run 45526821428, head 673b3ac6)
  673b3ac6 ckpt 3313: OPT1c lab v11 read (log readable thanks to the step timeout): first restart 
  ef7e930c ckpt 3311: OPT1c: lab opt v11 dispatched (run 45523035361, head 5fbe60ec)
  5fbe60ec ckpt 3310: OPT1c lab v10 lost: opt step ran to its 17 min cap, then a dead emulator hun
  599acf9b ckpt 3308: OPT1c: lab opt v10 dispatched (run 45517648960, head d8c21030)
  d8c21030 ckpt 3307: OPT1c lab v9 read: the restart after a wedged page was lost because the old 
  11859008 ckpt 3304: OPT1c: lab opt v9 dispatched (run 45514217127, head 4a0912a0)
```
