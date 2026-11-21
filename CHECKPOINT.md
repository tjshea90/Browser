# CHECKPOINT 2777 — read me first, then TASKS.md

**Written:** 2026-11-21T01:21:05Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `e04b99e6` (this checkpoint is the commit after it)

## Just done
REL79b: alpha79 DISPATCHED ONCE (android.yml app=gecko full_build=true ref claude/sweet-gates-gr3bj5, note = docs/handoff/ALPHA79_RELEASE_NOTE.txt, gecko code 180) at about 01:20Z. DO NOT DISPATCH AGAIN.

## Do this next
REL79c: wait tag kestrel-g-v0.3.0-alpha79 (git ls-remote --tags origin) + ALL jobs success (mcp__github__actions_list list_workflow_runs android.yml branch claude/sweet-gates-gr3bj5 -> run id -> list_workflow_jobs), get_release_by_tag shows the APK, THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha79 "$(cat docs/handoff/ALPHA79_RELEASE_NOTE.txt)"; then answer the owner (ATT1): Release link on its own line, not proven, the Back up and restore check, the Diagnostics line 'File chooser' to send

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e04b99e6 ckpt 2776: gated kestrel-g-v0.3.0-alpha79 (gecko code 180) and pushed it: handoff self-
  79a107c9 ckpt 2775: ATT1: file-chooser hardening coded + tested (core FilePickRulesTest + OwnerR
  299f3c4b ckpt 2774: ATT1 written into TASKS.md in the owner's words (new owner message: attachin
  9a0d1e24 ckpt 2773: alpha78 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha78, gecko code 179, run
  c8ad5f40 ckpt 2772: REL78b: alpha78 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  74fab19a ckpt 2771: gated kestrel-g-v0.3.0-alpha78 (gecko code 179) and pushed it: handoff self-
  afb6d504 ckpt 2770: YTC2: cause NOT proven. Read every tap path again (ControlsMachine, tap matc
  2ffbb683 ckpt 2769: YTC2 written into TASKS.md in the owner's words: alpha77 did not change the 
  36547fc5 ckpt 2768: alpha77 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha77, gecko code 178, run
  3f0787f7 ckpt 2767: REL77b: alpha77 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
```
