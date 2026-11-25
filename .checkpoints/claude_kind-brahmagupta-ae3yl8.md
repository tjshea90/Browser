# CHECKPOINT 3320 — read me first, then TASKS.md

**Written:** 2026-11-25T02:33:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `9cfd32ab` (this checkpoint is the commit after it)

## Just done
OPT1c lab v13 read: clean up to the first experiment's restart, then the emulator's whole system died ~100 s later (DeadSystemException); the first restart always works, later ones (page view drawing) kill the emulator's graphics. v14: restartQuietly (view GONE + session closed, kill 1.8 s later); compile + OwnerRulesTest green

## Do this next
Dispatch lab v14 (android.yml app=lab lab_suite=opt lab_profile=standard lab_net=full lab_minutes=5 full_build=false ref=claude/kind-brahmagupta-ae3yl8), record run id in a ckpt, send_later ~21 min, read get_job_logs of 'lab / lab' (python3 -B -I parse: live:/opt/KestrelRestart/scene/ISSUE + 'DeadSystemException'). Expect KestrelRestart lines for every restart, guard boot=1 after each, a finished last.json. If the system dies again after a quiet restart: option A the lab runs the opt round on an emulator with -gpu off/guest (EMU_EXTRA / emu-lab.yml), option B the app lets the old process exit by itself instead of being killed

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M research/OPTIMIZER.md

## Last ten checkpoints
```
  d05a3d57 ckpt 3318: OPT1c: lab opt v13 dispatched (run 45530873833, head 86130d11)
  86130d11 ckpt 3317: OPT1c lab v12 read: baseline fine, run stops at the first LIVE experiment (p
  ac1b71e1 ckpt 3314: OPT1c: lab opt v12 dispatched (run 45526821428, head 673b3ac6)
  673b3ac6 ckpt 3313: OPT1c lab v11 read (log readable thanks to the step timeout): first restart 
  ef7e930c ckpt 3311: OPT1c: lab opt v11 dispatched (run 45523035361, head 5fbe60ec)
  5fbe60ec ckpt 3310: OPT1c lab v10 lost: opt step ran to its 17 min cap, then a dead emulator hun
  599acf9b ckpt 3308: OPT1c: lab opt v10 dispatched (run 45517648960, head d8c21030)
  d8c21030 ckpt 3307: OPT1c lab v9 read: the restart after a wedged page was lost because the old 
  11859008 ckpt 3304: OPT1c: lab opt v9 dispatched (run 45514217127, head 4a0912a0)
  4a0912a0 ckpt 3303: OPT1c lab v8 read: first full sample ok; gl page still wedges the emulator's
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
