# CHECKPOINT 2767 — read me first, then TASKS.md

**Written:** 2026-11-20T21:06:05Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `2a4e281c` (this checkpoint is the commit after it)

## Just done
REL77b: alpha77 DISPATCHED ONCE (android.yml app=gecko full_build=true ref claude/sweet-gates-gr3bj5, note = docs/handoff/ALPHA77_RELEASE_NOTE.txt, gecko code 178) at about 21:05Z. DO NOT DISPATCH AGAIN.

## Do this next
REL77c: wait tag kestrel-g-v0.3.0-alpha77 (git ls-remote --tags origin) + ALL jobs success (mcp__github__actions_list list_workflow_runs android.yml branch claude/sweet-gates-gr3bj5 -> run id -> list_workflow_jobs), get_release_by_tag shows the APK, THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha77 "$(cat docs/handoff/ALPHA77_RELEASE_NOTE.txt)"; then answer the owner: alpha76 link https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha76 and alpha77 link (each on its own line), YTC1 two questions, private clone denial, open items

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2a4e281c ckpt 2766: gated kestrel-g-v0.3.0-alpha77 (gecko code 178) and pushed it: handoff self-
  855b173d ckpt 2765: YTC1: not reproduced (the YouTube film does not start behind its bot check i
  1cfd914e ckpt 2764: YTC1 written into TASKS.md in the owner's words (mid-turn owner message: You
  61ca6460 ckpt 2763: REL76b: alpha76 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  5eb5e6aa ckpt 2762: gated kestrel-g-v0.3.0-alpha76 (gecko code 177) and pushed it: handoff self-
  7df4d1c8 ckpt 2761: RVW5 gates all green: :core:test 1916 (OwnerRulesTest guard kept: VideoLevel
  8665429e ckpt 2760: RVW5: coded + tested in core (media tests 524 of 524 green): NA1 title clean
  58b7c173 ckpt 2759: RVW5 written into TASKS.md in the owner's words (new levers: speed, playback
  80ae116d ckpt 2758: RVW4 DONE AND SHIPPED: alpha75 RELEASED AND RECORDED = kestrel-g-v0.3.0-alph
  a0ad95ec ckpt 2757: REL75b: alpha75 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
```
