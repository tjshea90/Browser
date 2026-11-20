# CHECKPOINT 2730 — read me first, then TASKS.md

**Written:** 2026-11-20T05:11:43Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-d6f32d19-c3cwzl` · **builds on:** `373a540b` (this checkpoint is the commit after it)

## Just done
REL72: alpha72 build running (run 44678143468: check + test-js green, test-jvm running, build-gecko next); EY-15/16 annotated as partly covered

## Do this next
Wait for tag kestrel-g-v0.3.0-alpha72 (scratchpad tag.log shows TAG) and run 44678143468 all jobs success (mcp__github__actions_list list_workflow_jobs), get_release_by_tag shows the APK, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha72 with the IDENTICAL note (tr newlines to spaces from docs/handoff/ALPHA72_RELEASE_NOTE.txt); do NOT dispatch again

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  78e8423a ckpt 2729: REL72b: alpha72 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  8b8e8faa ckpt 2728: gated kestrel-g-v0.3.0-alpha72 (gecko code 173) and pushed it: handoff self-
  2cae13e7 ckpt 2727: REL72a done: geckoapp versionCode 173 / 0.3.0-alpha72, ext version synced, d
  10fe1a32 ckpt 2726: REL72 written into TASKS (owner said: Release the next version and tell me w
  bbad6ad4 ckpt 2725: RVW1 whole suite GREEN: :core:test :app:testDebugUnitTest :geckoapp:testDebu
  49214eac ckpt 2724: RVW1b-7: F5 shadow-root video watcher + F11 source labels (JS tests green: f
  202b8f0c ckpt 2723: RVW1b-6: F8 JSON-answer reader (Vimeo/JW/Wistia style), P2, P6, P7, P11 done
  6d527b43 ckpt 2722: RVW1b-5 CODED: naming N1-N5 N8, security S1 S3 S6 S7 S8 S9 S10; full core+ap
  e4c77795 ckpt 2721: RVW1b-4: P1 names.js gate, P10 map caps, N6 disposition cut done with web te
  e139e52d ckpt 2720: RVW1b-3 DONE: download engine V1 V2 V6 V7 V10 V12 V13 + S4 S5 fixed with tes
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
