# CHECKPOINT 2745 — read me first, then TASKS.md

**Written:** 2026-11-20T07:30:22Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-4efedacd-v3co75` · **builds on:** `ebada8d2` (this checkpoint is the commit after it)

## Just done
RVW3 fixes CODED + compile green (:geckoapp:compileDebugKotlin EXIT 0): security SC1 SC2 SC4 SC6 SC7 SC11, playback PB1 PB3 PB5 PB7, speed SP1. Proven so far: web background.test.js 122/122 (SC4 test fails on the old regex: 4099 ms), finder2.test.js 13/13, engine.test.js 10/10; real-list old-vs-new engine differential 11.8 M checks 0 differences (-33 % per check). Baseline before my edits: :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest EXIT 0. Full suite with my edits RUNNING in background (log in the session scratchpad full.log, ends with an EXIT line)

## Do this next
1) Wait for the full gradle run (:core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest :app:lintDebug --continue) to print EXIT in scratchpad full.log; fix any failure it names. 2) then cd web-tests && npm test (NOT while gradle runs: CPU flakes). 3) tick RVW3b/RVW3c in TASKS.md with that evidence, bump geckoapp versionCode 175 / 0.3.0-alpha74, bash tools/sync-ext-version.sh, write docs/handoff/ALPHA74_RELEASE_NOTE.txt, bash ship.sh gecko, ONE dispatch (app=gecko full_build=true), record-release, answer the owner, delete heartbeat trig_012BwCyPqhi5Dfz8amdBigFo

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/REVIEW_2026-11-21_SECURITY.md

## Last ten checkpoints
```
  7496a10a ckpt 2744: RVW3: security lane back and VERIFIED by me against the code (SC1 SC2 SC4 SC
  fe77b529 ckpt 2743: RVW3a launched: three read-only review lanes running as background agents (p
  e4b18cd0 ckpt 2742: RVW3 written into TASKS.md in the owner's words (review + improve speed, vid
  23e83be6 ckpt 2741: Owner decision recorded: keep Firefox's background traffic (P4) as it is; P3
  94c87088 ckpt 2740: alpha73 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha73 (gecko code 174, run
  7f06c310 ckpt 2739: REL73b: alpha73 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  5bf338a5 ckpt 2738: gated kestrel-g-v0.3.0-alpha73 (gecko code 174) and pushed it: handoff self-
  f2a9d669 ckpt 2737: REL73a done: versionCode 174 / 0.3.0-alpha73, ext synced, ALPHA73 release no
  b754bfa7 ckpt 2736: STANDING RULE recorded (owner: Always ship new versions and release without 
  26745261 ckpt 2735: RVW2 DONE: all code items from today's review (F13 F16 F17 F19 F24 V3 V4 V8 
```

(43 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
