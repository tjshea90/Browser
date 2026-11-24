# CHECKPOINT 3297 — read me first, then TASKS.md

**Written:** 2026-11-24T23:04:39Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `b94293e1` (this checkpoint is the commit after it)

## Just done
OPT1c: lab opt v7 dispatched (run 45506674935, head b94293e1)

## Do this next
Wait for run 45506674935 (about 20 min from 23:04Z; a send_later wake is set). Then list_workflow_jobs, get_job_logs of the 'lab / lab' job return_content tail_lines 900 (saved to a file; parse with python3 -B -I: lines with live: / stack: / ' opt ' / 'ui ' / scene / ISSUE). Read the 'ui STALL ... at' stack = where the screen thread sits at the first live experiment, and the 'opt page compat ... last title' = which probe hangs. Fix, re-dispatch. Only a finished run lets me flip Flavor.hasAutoTune and ship alpha107

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b94293e1 ckpt 3296: OPT1c lab v6 read (first real samples; compat page hangs inside one probe; s
  1a07b8f7 ckpt 3294: OPT1c: lab opt v6 dispatched (run 45502159321, head ea602fba)
  ea602fba ckpt 3293: OPT1c lab v5 read: rAF still dead in the Auto-tune view on the emulator; scr
  66748756 ckpt 3288: OPT1c: lab opt v5 dispatched (run 45496974270, head 371afa1e, includes setAc
  371afa1e ckpt 3287: OPT1c lab v4 read: anim page timed out because the Auto-tune session was nev
  a61c8965 ckpt 3283: lab opt v3 result: Auto-tune started on the emulator, restarted the whole ap
  416f926f ckpt 3281: OPTIMIZER.md status log added; waiting for lab opt v3 (run 45487350567) to f
  720d5995 ckpt 3280: alpha106 (code 207, GeckoView 161) SHIPPED: Release kestrel-g-v0.3.0-alpha10
  311a0daa ckpt 3278: opt lab v2 found the cause: the shell cannot start the non-exported Optimize
  315d0d7c ckpt 3276: alpha106 attempt 2 run id: 45486211006 (head 2a7eeeac, dispatched 20:57Z); l
```
