# CHECKPOINT 3275 — read me first, then TASKS.md

**Written:** 2026-11-24T20:58:15Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `2a7eeeac` (this checkpoint is the commit after it)

## Just done
alpha106 attempt 2 (gated ckpt 3274, head 2a7eeeac) DISPATCHED ONCE at 20:58Z; the first run 45474823903 failed only at Publish the Release (403, not reproduced by probes). Auto-tune hidden (hasAutoTune=false)

## Do this next
Find the new run id (list_workflow_runs android.yml, newest gecko dispatch after 20:58Z), poll its jobs; when build-gecko is green: get_release_by_tag kestrel-g-v0.3.0-alpha106 then bash tools/record-release.sh kestrel-g-v0.3.0-alpha106 with the identical note (tr -d newline of docs/handoff/ALPHA106_RELEASE_NOTE.txt); tick FF1c. If Publish fails with 403 AGAIN: it is not transient - ask the owner to check Settings > Actions > General and Settings > Rules. Meanwhile read the opt lab v2 console (lines starting 'opt:') and fix why the optimizer did not start, then re-dispatch lab opt.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2a7eeeac ckpt 3274: gated kestrel-g-v0.3.0-alpha106 (gecko code 207) and pushed it: handoff self
  89dc777e ckpt 3273: alpha106 attempt 2 prepared: the 403 was NOT reproduced by 3 probes (every r
  bfcc866a ckpt 3271: probe v2 result: EVERY release-creation variant works with the workflow toke
  7e08a8c5 ckpt 3269: probe v2: calls POST /releases directly (draft and real, target sha / branch
  8821fc21 ckpt 3268: alpha106 release publish fails with HTTP 403 twice (token has contents: writ
  8caaa5ba ckpt 3266: OPT1: Auto-tune integrated (BenchServer, Store, Apply, Guard, restart trampo
  59502d88 ckpt 3259: alpha106 (code 207) gated (ckpt 3257, head 0f14013b) and dispatched ONCE: ru
  0f14013b ckpt 3257: gated kestrel-g-v0.3.0-alpha106 (gecko code 207) and pushed it: handoff self
  c510bc41 ckpt 3256: OPT1: the Android-side sources and patch scripts are now saved IN the repo (
  f92c5435 ckpt 3255: OPT1: core done + 25 tests green, bench pages + web test green, tools/optimi
```
