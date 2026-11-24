# CHECKPOINT 3280 — read me first, then TASKS.md

**Written:** 2026-11-24T21:12:52Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `41af80c7` (this checkpoint is the commit after it)

## Just done
alpha106 (code 207, GeckoView 161) SHIPPED: Release kestrel-g-v0.3.0-alpha106 has the APK (run 45486211006, second attempt; the first failed only at Publish the Release with a transient 403 that three probes could not reproduce), recorded in BUILDLOG; FF1 ticked. Auto-tune is hidden (hasAutoTune=false) until the emulator lab proves it; lab opt v3 (run 45487350567) in flight

## Do this next
Read lab opt v3 console ('opt:' lines, get_job_logs of its lab job): if the run completed fix what it shows; when it passes: flip Flavor.hasAutoTune to true in GeckoApp, write docs/handoff/ALPHA107_RELEASE_NOTE.txt, bump 0.3.0-alpha107 / code 208, bash ship.sh gecko, ONE dispatch, record-release, tick OPT1c-e. Then answer the owner (FF1 shipped; OPT1 status).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  311a0daa ckpt 3278: opt lab v2 found the cause: the shell cannot start the non-exported Optimize
  315d0d7c ckpt 3276: alpha106 attempt 2 run id: 45486211006 (head 2a7eeeac, dispatched 20:57Z); l
  17ce4a0d ckpt 3275: alpha106 attempt 2 (gated ckpt 3274, head 2a7eeeac) DISPATCHED ONCE at 20:58
  2a7eeeac ckpt 3274: gated kestrel-g-v0.3.0-alpha106 (gecko code 207) and pushed it: handoff self
  89dc777e ckpt 3273: alpha106 attempt 2 prepared: the 403 was NOT reproduced by 3 probes (every r
  bfcc866a ckpt 3271: probe v2 result: EVERY release-creation variant works with the workflow toke
  7e08a8c5 ckpt 3269: probe v2: calls POST /releases directly (draft and real, target sha / branch
  8821fc21 ckpt 3268: alpha106 release publish fails with HTTP 403 twice (token has contents: writ
  8caaa5ba ckpt 3266: OPT1: Auto-tune integrated (BenchServer, Store, Apply, Guard, restart trampo
  59502d88 ckpt 3259: alpha106 (code 207) gated (ckpt 3257, head 0f14013b) and dispatched ONCE: ru
```
