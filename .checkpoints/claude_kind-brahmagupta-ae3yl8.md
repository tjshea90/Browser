# CHECKPOINT 3288 — read me first, then TASKS.md

**Written:** 2026-11-24T22:02:28Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `371afa1e` (this checkpoint is the commit after it)

## Just done
OPT1c: lab opt v5 dispatched (run 45496974270, head 371afa1e, includes setActive fix + no-frames watchdog + live opt timeline)

## Do this next
Poll run 45496974270 (mcp__github__actions_list list_workflow_jobs; job log 404s until the job ends, ~25-30 min from 22:02Z). Then get_job_logs return_content tail_lines 400 and read the 'live:' / 'opt:' / 'opt state:' console lines. If the run finishes with samples ok: set Flavor.hasAutoTune=true in GeckoApp.kt, replace RESULT_PLACEHOLDER in docs/handoff/ALPHA107_RELEASE_NOTE.txt with the real lab result, bump geckoapp/build.gradle.kts to 0.3.0-alpha107 / code 208, bash tools/sync-ext-version.sh, bash ship.sh gecko with the note, ONE dispatch app=gecko full_build=true, get_release_by_tag, bash tools/record-release.sh. If not ok: fix what the live timeline shows (stall after cursor 1 is still unexplained)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  371afa1e ckpt 3287: OPT1c lab v4 read: anim page timed out because the Auto-tune session was nev
  a61c8965 ckpt 3283: lab opt v3 result: Auto-tune started on the emulator, restarted the whole ap
  416f926f ckpt 3281: OPTIMIZER.md status log added; waiting for lab opt v3 (run 45487350567) to f
  720d5995 ckpt 3280: alpha106 (code 207, GeckoView 161) SHIPPED: Release kestrel-g-v0.3.0-alpha10
  311a0daa ckpt 3278: opt lab v2 found the cause: the shell cannot start the non-exported Optimize
  315d0d7c ckpt 3276: alpha106 attempt 2 run id: 45486211006 (head 2a7eeeac, dispatched 20:57Z); l
  17ce4a0d ckpt 3275: alpha106 attempt 2 (gated ckpt 3274, head 2a7eeeac) DISPATCHED ONCE at 20:58
  2a7eeeac ckpt 3274: gated kestrel-g-v0.3.0-alpha106 (gecko code 207) and pushed it: handoff self
  89dc777e ckpt 3273: alpha106 attempt 2 prepared: the 403 was NOT reproduced by 3 probes (every r
  bfcc866a ckpt 3271: probe v2 result: EVERY release-creation variant works with the workflow toke
```
