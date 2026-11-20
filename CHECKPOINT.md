# CHECKPOINT 2734 — read me first, then TASKS.md

**Written:** 2026-11-20T05:34:26Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-d6f32d19-c3cwzl` · **builds on:** `0dd9ad48` (this checkpoint is the commit after it)

## Just done
RVW2 batch 2 in the tree: V3 (Engine noWindow), V4 (resume big unknown-length parts, RESUME_MIN 1 MiB), V8 (Dash Period-level addressing), V11 (Hls.Init key + decryptInit), N7 (FileNames video/audio extension rules), download-S1 (DownloadService.onTimeout + Notifier.timeLimit); tests written for each; full :core:test + android compile running (scratchpad r2c.log)

## Do this next
Read scratchpad r2c.log (EXIT 0?). If a test fails read core/build/test-results/test/*.xml. Then: V8 error message in VideoFlow (plan null is reported as WebM/VP9 'cannot save yet'), run :app:testDebugUnitTest :geckoapp:testDebugUnitTest + lint + web npm test, update review STATUS columns and TASKS RVW2a-e, answer the owner (no release unless asked)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  3b6d5f14 ckpt 2733: RVW2 batch 1 in the tree: F13 F16 (JS + core tested), F17 F19 F24 S2 P8 P9 (
  8dcf45e0 ckpt 2732: RVW2 written into TASKS (owner: Do all the code tasks from today's review): 
  82a4bc7c ckpt 2731: alpha72 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha72 (gecko code 173, run
  e8a65c13 ckpt 2730: REL72: alpha72 build running (run 44678143468: check + test-js green, test-j
  78e8423a ckpt 2729: REL72b: alpha72 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  8b8e8faa ckpt 2728: gated kestrel-g-v0.3.0-alpha72 (gecko code 173) and pushed it: handoff self-
  2cae13e7 ckpt 2727: REL72a done: geckoapp versionCode 173 / 0.3.0-alpha72, ext version synced, d
  10fe1a32 ckpt 2726: REL72 written into TASKS (owner said: Release the next version and tell me w
  bbad6ad4 ckpt 2725: RVW1 whole suite GREEN: :core:test :app:testDebugUnitTest :geckoapp:testDebu
  49214eac ckpt 2724: RVW1b-7: F5 shadow-root video watcher + F11 source labels (JS tests green: f
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
