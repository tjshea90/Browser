# CHECKPOINT 3273 — read me first, then TASKS.md

**Written:** 2026-11-24T20:56:36Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `8b895cb5` (this checkpoint is the commit after it)

## Just done
alpha106 attempt 2 prepared: the 403 was NOT reproduced by 3 probes (every release variant incl. the kestrel-g-v* tag shape and a notes file works), so it looks transient; Auto-tune entry hidden (Flavor.hasAutoTune=false) because the emulator lab has not yet proved it; release note updated with one sentence about it

## Do this next
Run bash ship.sh gecko with the note of docs/handoff/ALPHA106_RELEASE_NOTE.txt (background, ~15 min), ONE dispatch (the old run 45474823903 is finished and failed at publish), watch the tag, record-release, tick FF1c. In parallel read the lab opt v2 console lines starting 'opt:' (get_job_logs of its lab job) to see why the optimizer did not start.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  bfcc866a ckpt 3271: probe v2 result: EVERY release-creation variant works with the workflow toke
  7e08a8c5 ckpt 3269: probe v2: calls POST /releases directly (draft and real, target sha / branch
  8821fc21 ckpt 3268: alpha106 release publish fails with HTTP 403 twice (token has contents: writ
  8caaa5ba ckpt 3266: OPT1: Auto-tune integrated (BenchServer, Store, Apply, Guard, restart trampo
  59502d88 ckpt 3259: alpha106 (code 207) gated (ckpt 3257, head 0f14013b) and dispatched ONCE: ru
  0f14013b ckpt 3257: gated kestrel-g-v0.3.0-alpha106 (gecko code 207) and pushed it: handoff self
  c510bc41 ckpt 3256: OPT1: the Android-side sources and patch scripts are now saved IN the repo (
  f92c5435 ckpt 3255: OPT1: core done + 25 tests green, bench pages + web test green, tools/optimi
  59efade6 ckpt 3253: FF1: alpha106 prepared (code 207, GeckoView 161): lab smoke GREEN on 161 (ru
  29576308 ckpt 3230: FF1: GeckoView 161.0.20261118180411 + webcompat 161.0 pinned, clean compile,
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
