# CHECKPOINT 2501 — read me first, then TASKS.md

**Written:** 2026-11-16T08:25:20Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `590f5687` (this checkpoint is the commit after it)

## Just done
ANS54e: lab run 43826954106 (suite ho) read: overlay/hls/unknown/never ok, file+bad failed (a tap on a native video controls reaches no page listener on the real engine). Fixed in gate-main.js nativePlay (trusted play event of a closed controls element = the press; again after a yes needs a tap; viaPlay guard), 4 new page tests (handover.test.js 15 ok, mutation-checked), full web suite 566/566; lab ho scene logs the page's tap events + presses file twice; TASKS ANS54e.7 + HANDOFF alpha58 bullet written

## Do this next
dispatch lab suite ho on this head (mcp__github__actions_run_trigger run_workflow workflow android.yml ref ccr-b3c2484c-sbew36 inputs app=lab lab_suite=ho), ~9 min; read artifact emulator-lab-text (fetch_artifact.py) section 'Web videos play in Kestrel own player' + the list of tap events; if file/bad pass: ONE vp run, then bump 159 / alpha58, note docs/handoff/ALPHA58_RELEASE_NOTE.txt, ship

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  00b6ff6b ckpt 2500: lab run 43826954106 dispatched: suite ho (head 97294fcb): web videos play in
  97294fcb ckpt 2499: ANS54e: full web suite 562/562, gradle all green (before the late-hand-over 
  368a9961 ckpt 2498: ANS54e coded, compiles: core WebHandOver + test (10 ok), Prefs web_in_kestre
  a90fb92d ckpt 2497: ANS54 design written (research/ANS54_DESIGN.md) and sub-rows ANS54e.1-7 in T
  519b8caa ckpt 2496: owner message 54 written into TASKS.md as ANS54a-f in his words (a: page pla
  201e537f ckpt 2495: alpha57 SHIPPED and recorded: Release kestrel-g-v0.3.0-alpha57 (code 158, AP
  eb5df864 ckpt 2494: alpha57 RELEASE DISPATCHED (ONE time): android.yml run 43820755532 (app=geck
  efc4f774 ckpt 2493: gated kestrel-g-v0.3.0-alpha57 (gecko code 158) and pushed it: handoff self-
  411b667f ckpt 2492: lab run 3 dispatched: 43818471174 (suite vp, head c002903d = ckpt 2491 = the
  c002903d ckpt 2491: ALPHA57 CANDIDATE: version 158 / 0.3.0-alpha57 (extension 1.0.158); lab run 
```

(10 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
