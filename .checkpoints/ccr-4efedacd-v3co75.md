# CHECKPOINT 2744 — read me first, then TASKS.md

**Written:** 2026-11-20T07:07:22Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-4efedacd-v3co75` · **builds on:** `9aa97e09` (this checkpoint is the commit after it)

## Just done
RVW3: security lane back and VERIFIED by me against the code (SC1 SC2 SC4 SC6 SC7 real; SC4 timed: 131 KB hostile body 1015 ms unbounded vs 31 ms bounded). Fix list written into TASKS RVW3b-sec. Baseline gradle run 1 failed only on the missing android-37.0 platform (documented quirk, installed with sdkmanager); baseline 2 running

## Do this next
WAIT for baseline2 (/tmp scratchpad baseline2.log EXIT line; do not start a second gradle). Then fix in order SC1 SC2 SC4 SC6 SC7 SC11 (TASKS RVW3b-sec), one test each; playback + speed lane files research/REVIEW_2026-11-21_{PLAYBACK,SPEED}.md may arrive meanwhile: verify those by reading code before fixing. Heartbeat trig_012BwCyPqhi5Dfz8amdBigFo

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  fe77b529 ckpt 2743: RVW3a launched: three read-only review lanes running as background agents (p
  e4b18cd0 ckpt 2742: RVW3 written into TASKS.md in the owner's words (review + improve speed, vid
  23e83be6 ckpt 2741: Owner decision recorded: keep Firefox's background traffic (P4) as it is; P3
  94c87088 ckpt 2740: alpha73 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha73 (gecko code 174, run
  7f06c310 ckpt 2739: REL73b: alpha73 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  5bf338a5 ckpt 2738: gated kestrel-g-v0.3.0-alpha73 (gecko code 174) and pushed it: handoff self-
  f2a9d669 ckpt 2737: REL73a done: versionCode 174 / 0.3.0-alpha73, ext synced, ALPHA73 release no
  b754bfa7 ckpt 2736: STANDING RULE recorded (owner: Always ship new versions and release without 
  26745261 ckpt 2735: RVW2 DONE: all code items from today's review (F13 F16 F17 F19 F24 V3 V4 V8 
  3b61bea3 ckpt 2734: RVW2 batch 2 in the tree: V3 (Engine noWindow), V4 (resume big unknown-lengt
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
