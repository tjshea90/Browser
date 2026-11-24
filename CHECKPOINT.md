# CHECKPOINT 3283 — read me first, then TASKS.md

**Written:** 2026-11-24T21:29:59Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `e2c177ff` (this checkpoint is the commit after it)

## Just done
lab opt v3 result: Auto-tune started on the emulator, restarted the whole app through the trampoline and the guard ran in the new process (boot=1) - then the new process was ended 2 s later by ProcessLife.maybeExit because a recreated (stale) MainActivity was destroyed and cleared the shared uiAlive flag. Fixes: OptimizerActivity re-asserts uiAlive every second while on screen; restartInto() now finishAffinity()s the old task right after starting the trampoline

## Do this next
Dispatch lab opt v4 (android.yml app=lab lab_suite=opt full_build=false), read its console ('opt:' / 'opt state:' lines and the scene row): expect samples and a finished run; fix what it shows; then hasAutoTune=true, alpha107 note, bump 208, ship.sh, dispatch, record-release

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  416f926f ckpt 3281: OPTIMIZER.md status log added; waiting for lab opt v3 (run 45487350567) to f
  720d5995 ckpt 3280: alpha106 (code 207, GeckoView 161) SHIPPED: Release kestrel-g-v0.3.0-alpha10
  311a0daa ckpt 3278: opt lab v2 found the cause: the shell cannot start the non-exported Optimize
  315d0d7c ckpt 3276: alpha106 attempt 2 run id: 45486211006 (head 2a7eeeac, dispatched 20:57Z); l
  17ce4a0d ckpt 3275: alpha106 attempt 2 (gated ckpt 3274, head 2a7eeeac) DISPATCHED ONCE at 20:58
  2a7eeeac ckpt 3274: gated kestrel-g-v0.3.0-alpha106 (gecko code 207) and pushed it: handoff self
  89dc777e ckpt 3273: alpha106 attempt 2 prepared: the 403 was NOT reproduced by 3 probes (every r
  bfcc866a ckpt 3271: probe v2 result: EVERY release-creation variant works with the workflow toke
  7e08a8c5 ckpt 3269: probe v2: calls POST /releases directly (draft and real, target sha / branch
  8821fc21 ckpt 3268: alpha106 release publish fails with HTTP 403 twice (token has contents: writ
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
