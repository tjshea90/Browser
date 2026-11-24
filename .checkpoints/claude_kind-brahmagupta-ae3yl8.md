# CHECKPOINT 3301 — read me first, then TASKS.md

**Written:** 2026-11-24T23:31:08Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `e03c0fb6` (this checkpoint is the commit after it)

## Just done
OPT1c: lab opt v8 dispatched (run 45510570085, head e03c0fb6)

## Do this next
Wait for run 45510570085 (about 23:52Z; a send_later wake is set). Then list_workflow_jobs, get_job_logs of the 'lab / lab' job return_content tail_lines 1000 (saved to a file; parse with python3 -B -I: lines with live: / ' opt ' / scene / ISSUE). Expect: first sample 'opt page gl timed out' + 'opt recover', later samples ok, restarts for gcpar/fps30, finished run with last.json. If it finishes: Flavor.hasAutoTune=true (GeckoApp.kt), fill RESULT_PLACEHOLDER in docs/handoff/ALPHA107_RELEASE_NOTE.txt, bump geckoapp/build.gradle.kts to 0.3.0-alpha107 / code 208, bash tools/sync-ext-version.sh, bash ship.sh gecko with the note, ONE dispatch app=gecko full_build=true, get_release_by_tag, tools/record-release.sh, tick TASKS OPT1c/d/e, answer the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e03c0fb6 ckpt 3300: OPT1c lab v7 read: the stall was a WebGL probe wedging the page process (not
  a1521080 ckpt 3297: OPT1c: lab opt v7 dispatched (run 45506674935, head b94293e1)
  b94293e1 ckpt 3296: OPT1c lab v6 read (first real samples; compat page hangs inside one probe; s
  1a07b8f7 ckpt 3294: OPT1c: lab opt v6 dispatched (run 45502159321, head ea602fba)
  ea602fba ckpt 3293: OPT1c lab v5 read: rAF still dead in the Auto-tune view on the emulator; scr
  66748756 ckpt 3288: OPT1c: lab opt v5 dispatched (run 45496974270, head 371afa1e, includes setAc
  371afa1e ckpt 3287: OPT1c lab v4 read: anim page timed out because the Auto-tune session was nev
  a61c8965 ckpt 3283: lab opt v3 result: Auto-tune started on the emulator, restarted the whole ap
  416f926f ckpt 3281: OPTIMIZER.md status log added; waiting for lab opt v3 (run 45487350567) to f
  720d5995 ckpt 3280: alpha106 (code 207, GeckoView 161) SHIPPED: Release kestrel-g-v0.3.0-alpha10
```
