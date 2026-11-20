# CHECKPOINT 2735 — read me first, then TASKS.md

**Written:** 2026-11-20T05:54:19Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-d6f32d19-c3cwzl` · **builds on:** `1b313e7d` (this checkpoint is the commit after it)

## Just done
RVW2 DONE: all code items from today's review (F13 F16 F17 F19 F24 V3 V4 V8 V11 N7 download-S1 S2 P5b P8 P9; P5a decided not to with a measurement); :core:test 1890 tests, :app + :geckoapp unit tests, lint, web npm test 646/646 all green; review STATUS columns updated; NOT released

## Do this next
Nothing is running. Waiting for the owner: say go to ship alpha73 (versionCode 174: bump geckoapp/build.gradle.kts, bash tools/sync-ext-version.sh, write docs/handoff/ALPHA73_RELEASE_NOTE.txt, bash ship.sh gecko, ONE dispatch, record-release). Still open and NOT code: P3 (tablet), P4 (owner decision on Firefox background traffic), S7 marker exemption (kept), N3 leading-verb rule + og:title (decided not to)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/REVIEW_2026-11-20_DOWNLOAD.md
     M research/REVIEW_2026-11-20_FINDER.md
     M research/REVIEW_2026-11-20_SECURITY.md
     M research/REVIEW_2026-11-20_SPEED.md

## Last ten checkpoints
```
  3b61bea3 ckpt 2734: RVW2 batch 2 in the tree: V3 (Engine noWindow), V4 (resume big unknown-lengt
  3b6d5f14 ckpt 2733: RVW2 batch 1 in the tree: F13 F16 (JS + core tested), F17 F19 F24 S2 P8 P9 (
  8dcf45e0 ckpt 2732: RVW2 written into TASKS (owner: Do all the code tasks from today's review): 
  82a4bc7c ckpt 2731: alpha72 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha72 (gecko code 173, run
  e8a65c13 ckpt 2730: REL72: alpha72 build running (run 44678143468: check + test-js green, test-j
  78e8423a ckpt 2729: REL72b: alpha72 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  8b8e8faa ckpt 2728: gated kestrel-g-v0.3.0-alpha72 (gecko code 173) and pushed it: handoff self-
  2cae13e7 ckpt 2727: REL72a done: geckoapp versionCode 173 / 0.3.0-alpha72, ext version synced, d
  10fe1a32 ckpt 2726: REL72 written into TASKS (owner said: Release the next version and tell me w
  bbad6ad4 ckpt 2725: RVW1 whole suite GREEN: :core:test :app:testDebugUnitTest :geckoapp:testDebu
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
