# CHECKPOINT 2769 — read me first, then TASKS.md

**Written:** 2026-11-20T22:05:58Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `36547fc5` (this checkpoint is the commit after it)

## Just done
YTC2 written into TASKS.md in the owner's words: alpha77 did not change the symptom (controls on YouTube still pop up then vanish at once; owner did not answer the two questions). Plan: ground truth in the emulator lab with a tap-and-sample scene, ON vs OFF.

## Do this next
YTC2a: read research/CR1_YOUTUBE_CRASH.md lab section (did the real YouTube films play on the GitHub emulator?), then add a lab scene to tools/emu/soak.py (yt suite) that taps the playing film and samples YouTube's player class / controls opacity + Kestrel's messages every 100 ms for 6 s, with the extension features on and off

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  36547fc5 ckpt 2768: alpha77 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha77, gecko code 178, run
  3f0787f7 ckpt 2767: REL77b: alpha77 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  2a4e281c ckpt 2766: gated kestrel-g-v0.3.0-alpha77 (gecko code 178) and pushed it: handoff self-
  855b173d ckpt 2765: YTC1: not reproduced (the YouTube film does not start behind its bot check i
  1cfd914e ckpt 2764: YTC1 written into TASKS.md in the owner's words (mid-turn owner message: You
  61ca6460 ckpt 2763: REL76b: alpha76 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  5eb5e6aa ckpt 2762: gated kestrel-g-v0.3.0-alpha76 (gecko code 177) and pushed it: handoff self-
  7df4d1c8 ckpt 2761: RVW5 gates all green: :core:test 1916 (OwnerRulesTest guard kept: VideoLevel
  8665429e ckpt 2760: RVW5: coded + tested in core (media tests 524 of 524 green): NA1 title clean
  58b7c173 ckpt 2759: RVW5 written into TASKS.md in the owner's words (new levers: speed, playback
```
