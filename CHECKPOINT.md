# CHECKPOINT 3294 — read me first, then TASKS.md

**Written:** 2026-11-24T22:34:43Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `ea602fba` (this checkpoint is the commit after it)

## Just done
OPT1c: lab opt v6 dispatched (run 45502159321, head ea602fba)

## Do this next
Wait for run 45502159321 (job log 404s until it ends, about 22:56Z; a send_later wake is set). Then get_job_logs job of 'lab / lab' return_content tail_lines 700 (saved to a file: parse with python3 -B -I, replace backslash-n, print lines with 'live:' 'stack:' 'opt' 'scene' 'ISSUE'). Look for: 'opt view' line (size/shown/attached/windowFocus), anim error text (visibility), 'stack:' lines of the main thread when silent, whether a sample with timer frames is ok and the run reaches cursor 2+ and restarts for gcpar/fps30. Fix, re-dispatch; only a finished run lets me flip Flavor.hasAutoTune and ship alpha107

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ea602fba ckpt 3293: OPT1c lab v5 read: rAF still dead in the Auto-tune view on the emulator; scr
  66748756 ckpt 3288: OPT1c: lab opt v5 dispatched (run 45496974270, head 371afa1e, includes setAc
  371afa1e ckpt 3287: OPT1c lab v4 read: anim page timed out because the Auto-tune session was nev
  a61c8965 ckpt 3283: lab opt v3 result: Auto-tune started on the emulator, restarted the whole ap
  416f926f ckpt 3281: OPTIMIZER.md status log added; waiting for lab opt v3 (run 45487350567) to f
  720d5995 ckpt 3280: alpha106 (code 207, GeckoView 161) SHIPPED: Release kestrel-g-v0.3.0-alpha10
  311a0daa ckpt 3278: opt lab v2 found the cause: the shell cannot start the non-exported Optimize
  315d0d7c ckpt 3276: alpha106 attempt 2 run id: 45486211006 (head 2a7eeeac, dispatched 20:57Z); l
  17ce4a0d ckpt 3275: alpha106 attempt 2 (gated ckpt 3274, head 2a7eeeac) DISPATCHED ONCE at 20:58
  2a7eeeac ckpt 3274: gated kestrel-g-v0.3.0-alpha106 (gecko code 207) and pushed it: handoff self
```
