# CHECKPOINT 3310 — read me first, then TASKS.md

**Written:** 2026-11-25T01:04:49Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `11770adb` (this checkpoint is the commit after it)

## Just done
OPT1c lab v10 lost: opt step ran to its 17 min cap, then a dead emulator hung the profile step; cancelled run = no log. emu-lab.yml: timeout-minutes 4 on the profile step. Re-dispatching the same code as v11

## Do this next
Dispatch lab v11 (android.yml app=lab lab_suite=opt lab_profile=standard lab_net=full lab_minutes=5 full_build=false ref=claude/kind-brahmagupta-ae3yl8), record run id in a ckpt, send_later ~25 min, read get_job_logs of 'lab / lab' (parse live:/opt/KestrelRestart/scene/ISSUE). DO NOT cancel a run while a step is hung: it loses the log. If the log shows the opt round did not finish, read where it stalled after the first restart (KestrelRestart lines, guard boot=1 lines)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M research/OPTIMIZER.md

## Last ten checkpoints
```
  599acf9b ckpt 3308: OPT1c: lab opt v10 dispatched (run 45517648960, head d8c21030)
  d8c21030 ckpt 3307: OPT1c lab v9 read: the restart after a wedged page was lost because the old 
  11859008 ckpt 3304: OPT1c: lab opt v9 dispatched (run 45514217127, head 4a0912a0)
  4a0912a0 ckpt 3303: OPT1c lab v8 read: first full sample ok; gl page still wedges the emulator's
  1c21a8de ckpt 3301: OPT1c: lab opt v8 dispatched (run 45510570085, head e03c0fb6)
  e03c0fb6 ckpt 3300: OPT1c lab v7 read: the stall was a WebGL probe wedging the page process (not
  a1521080 ckpt 3297: OPT1c: lab opt v7 dispatched (run 45506674935, head b94293e1)
  b94293e1 ckpt 3296: OPT1c lab v6 read (first real samples; compat page hangs inside one probe; s
  1a07b8f7 ckpt 3294: OPT1c: lab opt v6 dispatched (run 45502159321, head ea602fba)
  ea602fba ckpt 3293: OPT1c lab v5 read: rAF still dead in the Auto-tune view on the emulator; scr
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
