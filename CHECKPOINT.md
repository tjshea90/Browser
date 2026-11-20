# CHECKPOINT 2743 — read me first, then TASKS.md

**Written:** 2026-11-20T06:51:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-4efedacd-v3co75` · **builds on:** `e4b18cd0` (this checkpoint is the commit after it)

## Just done
RVW3a launched: three read-only review lanes running as background agents (playback, security, speed) writing research/REVIEW_2026-11-21_{PLAYBACK,SECURITY,SPEED}.md. Heartbeat trigger trig_012BwCyPqhi5Dfz8amdBigFo created (every 2 h, delete when RVW3 shipped)

## Do this next
If research/REVIEW_2026-11-21_*.md exist: read them, verify each finding against the code myself, fix VERIFIED ones with a test each. If none exist the lanes were cut off: re-launch only the missing ones (brief is in TASKS RVW3a). Then RVW3c whole suite (:core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest :app:lintDebug + cd web-tests && npm test), ship alpha74 (versionCode 175), delete heartbeat trig_012BwCyPqhi5Dfz8amdBigFo

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e4b18cd0 ckpt 2742: RVW3 written into TASKS.md in the owner's words (review + improve speed, vid
  23e83be6 ckpt 2741: Owner decision recorded: keep Firefox's background traffic (P4) as it is; P3
  94c87088 ckpt 2740: alpha73 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha73 (gecko code 174, run
  7f06c310 ckpt 2739: REL73b: alpha73 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  5bf338a5 ckpt 2738: gated kestrel-g-v0.3.0-alpha73 (gecko code 174) and pushed it: handoff self-
  f2a9d669 ckpt 2737: REL73a done: versionCode 174 / 0.3.0-alpha73, ext synced, ALPHA73 release no
  b754bfa7 ckpt 2736: STANDING RULE recorded (owner: Always ship new versions and release without 
  26745261 ckpt 2735: RVW2 DONE: all code items from today's review (F13 F16 F17 F19 F24 V3 V4 V8 
  3b61bea3 ckpt 2734: RVW2 batch 2 in the tree: V3 (Engine noWindow), V4 (resume big unknown-lengt
  3b6d5f14 ckpt 2733: RVW2 batch 1 in the tree: F13 F16 (JS + core tested), F17 F19 F24 S2 P8 P9 (
```
