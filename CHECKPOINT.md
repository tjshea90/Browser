# CHECKPOINT 3278 — read me first, then TASKS.md

**Written:** 2026-11-24T21:04:30Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `fe72d422` (this checkpoint is the commit after it)

## Just done
opt lab v2 found the cause: the shell cannot start the non-exported OptimizerActivity (no 'opt' events, empty files/optimizer). Fix: MainActivity starts it for the lab (debuggable build only, extra auto_tune=quick); soak.py starts through MainActivity and prints what am start says. alpha106 attempt 2 run 45486211006 still building

## Do this next
Dispatch lab opt v3 (android.yml app=lab lab_suite=opt full_build=false), read its console 'opt:' lines; meanwhile poll run 45486211006 for alpha106.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  315d0d7c ckpt 3276: alpha106 attempt 2 run id: 45486211006 (head 2a7eeeac, dispatched 20:57Z); l
  17ce4a0d ckpt 3275: alpha106 attempt 2 (gated ckpt 3274, head 2a7eeeac) DISPATCHED ONCE at 20:58
  2a7eeeac ckpt 3274: gated kestrel-g-v0.3.0-alpha106 (gecko code 207) and pushed it: handoff self
  89dc777e ckpt 3273: alpha106 attempt 2 prepared: the 403 was NOT reproduced by 3 probes (every r
  bfcc866a ckpt 3271: probe v2 result: EVERY release-creation variant works with the workflow toke
  7e08a8c5 ckpt 3269: probe v2: calls POST /releases directly (draft and real, target sha / branch
  8821fc21 ckpt 3268: alpha106 release publish fails with HTTP 403 twice (token has contents: writ
  8caaa5ba ckpt 3266: OPT1: Auto-tune integrated (BenchServer, Store, Apply, Guard, restart trampo
  59502d88 ckpt 3259: alpha106 (code 207) gated (ckpt 3257, head 0f14013b) and dispatched ONCE: ru
  0f14013b ckpt 3257: gated kestrel-g-v0.3.0-alpha106 (gecko code 207) and pushed it: handoff self
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
