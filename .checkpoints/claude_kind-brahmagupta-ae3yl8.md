# CHECKPOINT 3300 — read me first, then TASKS.md

**Written:** 2026-11-24T23:30:35Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `3717f435` (this checkpoint is the commit after it)

## Just done
OPT1c lab v7 read: the stall was a WebGL probe wedging the page process (not the screen thread). v8: gl.html own optional last page + gl_ok guard in Decider, recoverEngine() ends engine helper processes when a page never answers, fast limits capped 40 s, scroll fast wait 3 s; core optimizer + OwnerRulesTest + compile green, web bench 10/10

## Do this next
Dispatch lab v8 (android.yml app=lab lab_suite=opt lab_profile=standard lab_net=full lab_minutes=5 full_build=false ref=claude/kind-brahmagupta-ae3yl8), record the run id in a ckpt, send_later ~22 min, then get_job_logs of 'lab / lab' (saved file; parse with python3 -B -I for lines containing live: / opt / scene / ISSUE). Expected now: first sample hits 'opt page gl timed out' then 'opt recover', later samples ok, restarts for gcpar/fps30 and a finished run with last.json. If it finishes: Flavor.hasAutoTune=true, fill RESULT_PLACEHOLDER in docs/handoff/ALPHA107_RELEASE_NOTE.txt, bump 0.3.0-alpha107 / code 208, tools/sync-ext-version.sh, ship.sh gecko, ONE dispatch, record-release

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a1521080 ckpt 3297: OPT1c: lab opt v7 dispatched (run 45506674935, head b94293e1)
  b94293e1 ckpt 3296: OPT1c lab v6 read (first real samples; compat page hangs inside one probe; s
  1a07b8f7 ckpt 3294: OPT1c: lab opt v6 dispatched (run 45502159321, head ea602fba)
  ea602fba ckpt 3293: OPT1c lab v5 read: rAF still dead in the Auto-tune view on the emulator; scr
  66748756 ckpt 3288: OPT1c: lab opt v5 dispatched (run 45496974270, head 371afa1e, includes setAc
  371afa1e ckpt 3287: OPT1c lab v4 read: anim page timed out because the Auto-tune session was nev
  a61c8965 ckpt 3283: lab opt v3 result: Auto-tune started on the emulator, restarted the whole ap
  416f926f ckpt 3281: OPTIMIZER.md status log added; waiting for lab opt v3 (run 45487350567) to f
  720d5995 ckpt 3280: alpha106 (code 207, GeckoView 161) SHIPPED: Release kestrel-g-v0.3.0-alpha10
  311a0daa ckpt 3278: opt lab v2 found the cause: the shell cannot start the non-exported Optimize
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
