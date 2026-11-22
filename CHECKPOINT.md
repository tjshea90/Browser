# CHECKPOINT 2958 — read me first, then TASKS.md

**Written:** 2026-11-22T07:06:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-144aedec-7apwin` · **builds on:** `30b8692b` (this checkpoint is the commit after it)

## Just done
alpha92 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha92 (gecko code 193, run 44995587209 green: check, test-js, test-jvm, build-gecko incl. pref-name check; Release has APK + sha256 + dm; BUILDLOG line pushed). PERF7a done; PERF7b / PERF7c / PERF5b shipped, NOT confirmed on the tablet

## Do this next
Next: waiting for the owner's tablet test of alpha92. When the next upload arrives (python3 -B -I tools/diag_digest.py): (1) Diagnostics 'Engine prefs' must show gfx.webrender.scissored-cache-clears.enabled=false, batched-texture-uploads=false, max-partial-present-rects=0, allow-partial-present-buffer-age=false (proves Safe drawing reached the engine) plus the engine defaults of pbo-uploads and layer-compositor; if the owner still sees bands with Safe drawing ON try gfx.webrender.pbo-uploads / layer-compositor / GPU process off, then software WebRender; (2) pornhub: 'video siblings' / 'quality-switch' lines (native + inline), hand-over / hand-declined lines after a real press; (3) claude.ai: rejected: lines (photo upload), consider default exclude of claude.ai from the content scripts (A/B up 41 s vs down 24 s) and an FPP canvas A/B; (4) not done: per-list fps in the quality rows, automatic step-down on a stall

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  292067e6 ckpt 2955: alpha92 (gecko code 193) gated by ship.sh (core+app suites, lint, handoff se
  df0b4a8a ckpt 2954: gated kestrel-g-v0.3.0-alpha92 (gecko code 193) and pushed it: handoff self-
  2db9f72a ckpt 2952: pre-ship: alpha92: quality choices for pornhub in Kestrel's player, and a Sa
  ee6c59f9 ckpt 2938: PERF7c step 1: Settings > Network lab 'Safe drawing' (lab_draw, default ON) 
  449480c7 ckpt 2933: PERF7 started on branch ccr-144aedec-7apwin: owner request + 2 screenshots w
  4da54c18 ckpt 2915: alpha91 RELEASED and recorded (run 44987313373 green, Release has the APK). 
  a42e5268 ckpt 2913: alpha91 (code 192) gated (ship.sh green on attempt 3) and ONE dispatch made 
  df3d34d1 ckpt 2912: gated kestrel-g-v0.3.0-alpha91 (gecko code 192) and pushed it: handoff self-
  345be8b0 ckpt 2911: alpha91 ship attempt 3 running: attempt 1 red on OwnerRulesTest pin (10 s wi
  7693a032 ckpt 2908: pre-ship: alpha91: pornhub in Kestrel's own player, and the clipped circle. 
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
