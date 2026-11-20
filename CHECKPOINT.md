# CHECKPOINT 2772 — read me first, then TASKS.md

**Written:** 2026-11-20T22:28:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `74fab19a` (this checkpoint is the commit after it)

## Just done
REL78b: alpha78 DISPATCHED ONCE (android.yml app=gecko full_build=true ref claude/sweet-gates-gr3bj5, note = docs/handoff/ALPHA78_RELEASE_NOTE.txt, gecko code 179) at about 22:28Z. DO NOT DISPATCH AGAIN.

## Do this next
REL78c: wait tag kestrel-g-v0.3.0-alpha78 (git ls-remote --tags origin) + ALL jobs success (mcp__github__actions_list list_workflow_runs android.yml branch claude/sweet-gates-gr3bj5 -> run id -> list_workflow_jobs), get_release_by_tag shows the APK, THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha78 "$(cat docs/handoff/ALPHA78_RELEASE_NOTE.txt)"; then answer the owner (YTC2): Release link on its own line, say it is NOT proven, give the Settings bisect list, ask the two questions

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  74fab19a ckpt 2771: gated kestrel-g-v0.3.0-alpha78 (gecko code 179) and pushed it: handoff self-
  afb6d504 ckpt 2770: YTC2: cause NOT proven. Read every tap path again (ControlsMachine, tap matc
  2ffbb683 ckpt 2769: YTC2 written into TASKS.md in the owner's words: alpha77 did not change the 
  36547fc5 ckpt 2768: alpha77 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha77, gecko code 178, run
  3f0787f7 ckpt 2767: REL77b: alpha77 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  2a4e281c ckpt 2766: gated kestrel-g-v0.3.0-alpha77 (gecko code 178) and pushed it: handoff self-
  855b173d ckpt 2765: YTC1: not reproduced (the YouTube film does not start behind its bot check i
  1cfd914e ckpt 2764: YTC1 written into TASKS.md in the owner's words (mid-turn owner message: You
  61ca6460 ckpt 2763: REL76b: alpha76 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  5eb5e6aa ckpt 2762: gated kestrel-g-v0.3.0-alpha76 (gecko code 177) and pushed it: handoff self-
```
