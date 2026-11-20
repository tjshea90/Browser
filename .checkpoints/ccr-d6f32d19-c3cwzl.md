# CHECKPOINT 2741 — read me first, then TASKS.md

**Written:** 2026-11-20T06:32:01Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-d6f32d19-c3cwzl` · **builds on:** `94c87088` (this checkpoint is the commit after it)

## Just done
Owner decision recorded: keep Firefox's background traffic (P4) as it is; P3 (shader precache) explained to the owner, no code

## Do this next
Nothing is running. Waiting for the owner's tablet test of alpha72/alpha73. P3 stays open (needs tablet numbers: cold-start paint mark with gfx.webrender.precache-shaders true vs false through the debug YAML hook). Any new finished work: ship without asking (CLAUDE.md standing rule)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/REVIEW_2026-11-20_SPEED.md

## Last ten checkpoints
```
  94c87088 ckpt 2740: alpha73 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha73 (gecko code 174, run
  7f06c310 ckpt 2739: REL73b: alpha73 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  5bf338a5 ckpt 2738: gated kestrel-g-v0.3.0-alpha73 (gecko code 174) and pushed it: handoff self-
  f2a9d669 ckpt 2737: REL73a done: versionCode 174 / 0.3.0-alpha73, ext synced, ALPHA73 release no
  b754bfa7 ckpt 2736: STANDING RULE recorded (owner: Always ship new versions and release without 
  26745261 ckpt 2735: RVW2 DONE: all code items from today's review (F13 F16 F17 F19 F24 V3 V4 V8 
  3b61bea3 ckpt 2734: RVW2 batch 2 in the tree: V3 (Engine noWindow), V4 (resume big unknown-lengt
  3b6d5f14 ckpt 2733: RVW2 batch 1 in the tree: F13 F16 (JS + core tested), F17 F19 F24 S2 P8 P9 (
  8dcf45e0 ckpt 2732: RVW2 written into TASKS (owner: Do all the code tasks from today's review): 
  82a4bc7c ckpt 2731: alpha72 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha72 (gecko code 173, run
```
