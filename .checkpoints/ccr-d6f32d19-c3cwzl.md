# CHECKPOINT 2733 — read me first, then TASKS.md

**Written:** 2026-11-20T05:31:23Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-d6f32d19-c3cwzl` · **builds on:** `7f74965d` (this checkpoint is the commit after it)

## Just done
RVW2 batch 1 in the tree: F13 F16 (JS + core tested), F17 F19 F24 S2 P8 P9 (Kotlin coded, test run started: scratchpad r2b.log), P5b done with test, P5a decided not to (measured)

## Do this next
Read scratchpad r2b.log (EXIT 0?) for MediaList/MediaRank/GoneDocs/AuthNames/FinderCorpus tests + geckoapp/app compile; then RVW2b: V8 (Dash.plan Period-level templates), V11 (Init key + decrypt), V3, V4 (resume big single segments), N7 (FileNames extension), download S1 (DownloadService onTimeout)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M app/src/main/kotlin/app/kestrel/browser/diag/Health.kt
     M geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt

## Last ten checkpoints
```
  8dcf45e0 ckpt 2732: RVW2 written into TASKS (owner: Do all the code tasks from today's review): 
  82a4bc7c ckpt 2731: alpha72 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha72 (gecko code 173, run
  e8a65c13 ckpt 2730: REL72: alpha72 build running (run 44678143468: check + test-js green, test-j
  78e8423a ckpt 2729: REL72b: alpha72 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  8b8e8faa ckpt 2728: gated kestrel-g-v0.3.0-alpha72 (gecko code 173) and pushed it: handoff self-
  2cae13e7 ckpt 2727: REL72a done: geckoapp versionCode 173 / 0.3.0-alpha72, ext version synced, d
  10fe1a32 ckpt 2726: REL72 written into TASKS (owner said: Release the next version and tell me w
  bbad6ad4 ckpt 2725: RVW1 whole suite GREEN: :core:test :app:testDebugUnitTest :geckoapp:testDebu
  49214eac ckpt 2724: RVW1b-7: F5 shadow-root video watcher + F11 source labels (JS tests green: f
  202b8f0c ckpt 2723: RVW1b-6: F8 JSON-answer reader (Vimeo/JW/Wistia style), P2, P6, P7, P11 done
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
