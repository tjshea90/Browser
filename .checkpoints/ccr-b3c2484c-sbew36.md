# CHECKPOINT 2491 — read me first, then TASKS.md

**Written:** 2026-11-16T06:41:03Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `461f4da5` (this checkpoint is the commit after it)

## Just done
ALPHA57 CANDIDATE: version 158 / 0.3.0-alpha57 (extension 1.0.158); lab run 2 (43816308648) = 7 of 7 scenes ok, 0 issues on the real engine (research/ANS53_LAB_VP1.md); independent gate review fixed (research/ANS53_GATE_REVIEW.md: audio exempt, MediaSource-only blob hold, stream gets autoplay, tap scoping + 30 s, 1.2 s gesture grace, late-answer wait, fail-closed relay, caps); web suite 551/551, gradle :core :app :geckoapp all green; ANS53c/d/f ticked with lab evidence

## Do this next
dispatch lab run 3 (suite vp) on this head to prove the reviewed gate on the real engine (new: variant 'bar' = press on the engine's own control bar, mse page held through the createObjectURL wrapper, fail-closed relay): mcp__github__actions_run_trigger android.yml ref ccr-b3c2484c-sbew36 app=lab lab_suite=vp lab_minutes=10 lab_net=full full_build=false; poll get_workflow_job; if 0 issues: bash ship.sh gecko "<note from scratchpad ship/note_draft.txt, final>" then ONE fresh dispatch app=gecko full_build=true note=<same>; wait for tag + jobs; get_release_by_tag; bash tools/record-release.sh <tag> "<IDENTICAL note>"; answer the owner (all of message 53) with the Release link on its own line; heartbeat trig_01Y8CFHKVb6hmQmEEM78p6gv

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  152027b8 ckpt 2490: lab run 2 dispatched: 43816308648 (android.yml app=lab lab_suite=vp, head 99
  99c99a77 ckpt 2489: pre-dispatch checks green for lab run 2: web suite 542/542, gradle :core :ap
  a1776ee8 ckpt 2488: lab run 43812820865 read (research/ANS53_LAB_VP1.md): kp 27/27 + cp + hls + 
  55fe4f13 ckpt 2487: lab extras for the NEXT dispatch (the in-flight run 43812820865 uses ckpt 24
  a7dfeb23 ckpt 2486: lab run 43812820865 dispatched (android.yml app=lab lab_suite=vp, ref ccr-b3
  51374ebb ckpt 2485: ANS53d.2 coded + verified: Quality button in the page's full-screen controls
  50a021f2 ckpt 2484: ANS53 wiring verified: player web tests fixed (27/27), full web suite 541/54
  36604289 ckpt 2483: ANS53a/b CODED (not yet run on the emulator): gate-main.js (world MAIN: prel
  36b6723f ckpt 2482: ANS53 first batch CODED (unit tests green: core player + QualityLabel; gecko
  6052291b ckpt 2481: owner message 53 (04:59Z, on alpha56: 'The video player is much better now';
```

(15 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
