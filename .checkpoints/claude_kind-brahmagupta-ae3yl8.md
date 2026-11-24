# CHECKPOINT 3296 — read me first, then TASKS.md

**Written:** 2026-11-24T23:03:52Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `f594ba99` (this checkpoint is the commit after it)

## Just done
OPT1c lab v6 read (first real samples; compat page hangs inside one probe; screen thread silent at first live experiment). v7: compat probes sequential with 4 s each + title, last-title log on timeout, lab timer shim races every frame, UI watchdog in the Auto-tune screen (stall stacks), soak shows ui lines + adb root for kill -3; web bench 9/9, compile + OwnerRulesTest green

## Do this next
Dispatch lab v7 (android.yml app=lab lab_suite=opt lab_profile=standard lab_net=full lab_minutes=5 full_build=false ref=claude/kind-brahmagupta-ae3yl8), wait ~20 min, get_job_logs of 'lab / lab' (saved to a file; parse with python3 -B -I: lines with live: stack: opt ui scene ISSUE). Read: 'ui STALL ... at' lines = where the screen thread sits at the first live experiment; 'opt page compat ... last title' = which probe hangs. Fix, re-dispatch. Only a finished run lets me flip Flavor.hasAutoTune and ship alpha107

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M research/OPTIMIZER.md

## Last ten checkpoints
```
  1a07b8f7 ckpt 3294: OPT1c: lab opt v6 dispatched (run 45502159321, head ea602fba)
  ea602fba ckpt 3293: OPT1c lab v5 read: rAF still dead in the Auto-tune view on the emulator; scr
  66748756 ckpt 3288: OPT1c: lab opt v5 dispatched (run 45496974270, head 371afa1e, includes setAc
  371afa1e ckpt 3287: OPT1c lab v4 read: anim page timed out because the Auto-tune session was nev
  a61c8965 ckpt 3283: lab opt v3 result: Auto-tune started on the emulator, restarted the whole ap
  416f926f ckpt 3281: OPTIMIZER.md status log added; waiting for lab opt v3 (run 45487350567) to f
  720d5995 ckpt 3280: alpha106 (code 207, GeckoView 161) SHIPPED: Release kestrel-g-v0.3.0-alpha10
  311a0daa ckpt 3278: opt lab v2 found the cause: the shell cannot start the non-exported Optimize
  315d0d7c ckpt 3276: alpha106 attempt 2 run id: 45486211006 (head 2a7eeeac, dispatched 20:57Z); l
  17ce4a0d ckpt 3275: alpha106 attempt 2 (gated ckpt 3274, head 2a7eeeac) DISPATCHED ONCE at 20:58
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
