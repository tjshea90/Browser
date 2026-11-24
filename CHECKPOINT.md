# CHECKPOINT 3293 — read me first, then TASKS.md

**Written:** 2026-11-24T22:34:06Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `96fbb617` (this checkpoint is the commit after it)

## Just done
OPT1c lab v5 read: rAF still dead in the Auto-tune view on the emulator; screen thread went silent at the first experiment (ANR UpdateJob). v6 = binder calls off the screen thread, richer page error + opt view line, lab-only timer frames (&lab=1), SIGQUIT stack dump on silence in soak.py; web bench 9/9, compile + OwnerRulesTest green

## Do this next
Dispatch lab v6 (android.yml app=lab lab_suite=opt lab_profile=standard lab_net=full lab_minutes=5 full_build=false ref=claude/kind-brahmagupta-ae3yl8), find the run id, wait for the job (logs 404 until done, ~25 min), read 'live:' 'stack:' 'opt:' console lines with python on the saved log file (see tool-results). Decide from the stacks: where the screen thread sits, and what 'opt view'/the anim error say about visibility. Then fix, re-dispatch, and only when a run finishes flip hasAutoTune and ship alpha107

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M research/OPTIMIZER.md

## Last ten checkpoints
```
  66748756 ckpt 3288: OPT1c: lab opt v5 dispatched (run 45496974270, head 371afa1e, includes setAc
  371afa1e ckpt 3287: OPT1c lab v4 read: anim page timed out because the Auto-tune session was nev
  a61c8965 ckpt 3283: lab opt v3 result: Auto-tune started on the emulator, restarted the whole ap
  416f926f ckpt 3281: OPTIMIZER.md status log added; waiting for lab opt v3 (run 45487350567) to f
  720d5995 ckpt 3280: alpha106 (code 207, GeckoView 161) SHIPPED: Release kestrel-g-v0.3.0-alpha10
  311a0daa ckpt 3278: opt lab v2 found the cause: the shell cannot start the non-exported Optimize
  315d0d7c ckpt 3276: alpha106 attempt 2 run id: 45486211006 (head 2a7eeeac, dispatched 20:57Z); l
  17ce4a0d ckpt 3275: alpha106 attempt 2 (gated ckpt 3274, head 2a7eeeac) DISPATCHED ONCE at 20:58
  2a7eeeac ckpt 3274: gated kestrel-g-v0.3.0-alpha106 (gecko code 207) and pushed it: handoff self
  89dc777e ckpt 3273: alpha106 attempt 2 prepared: the 403 was NOT reproduced by 3 probes (every r
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
