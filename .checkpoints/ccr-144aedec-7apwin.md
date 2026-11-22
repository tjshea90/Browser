# CHECKPOINT 2977 — read me first, then TASKS.md

**Written:** 2026-11-22T07:59:05Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-144aedec-7apwin` · **builds on:** `694a99d8` (this checkpoint is the commit after it)

## Just done
alpha93 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha93 (gecko code 194, run 44999519054 green, Release has APK + sha256 + dm, BUILDLOG line pushed). PERF8a/b/c shipped, NOT confirmed on the tablet

## Do this next
Next: waiting for the owner's tablet test of alpha93. Read the next upload (python3 -B -I tools/diag_digest.py --days 2): (1) native/inline 'siblings ... found after the start' + 'quality-switch' lines and whether the full-screen quality button showed; (2) 'video calm <element> ... overflow=... cutBottom=N' (names the pornhub circle; cutBottom>0 = a page box clips it, none = engine drawing) or its absence (circle not a CSS animation); (3) 'engine draw-prefs' line (Safe drawing applied?); (4) not done: per-list fps in rows, automatic step-down on a stall, claude.ai exclude / FPP canvas A/B (owner decision), pornhub page player 720p stall cause

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a882ac50 ckpt 2974: alpha93 (gecko code 194) gated by ship.sh and ONE dispatch made on ccr-144ae
  5488bb85 ckpt 2973: gated kestrel-g-v0.3.0-alpha93 (gecko code 194) and pushed it: handoff self-
  bf393943 ckpt 2971: pre-ship: alpha93: quality button in the full-screen player, the stuck loadi
  335cc029 ckpt 2968: PERF8 code (not released): (a) VideoFlow.offerSiblings gives the player that
  06774fc1 ckpt 2959: PERF8 written to TASKS.md (owner after alpha92: fullscreen Kestrel player wo
  f41f8f8b ckpt 2958: alpha92 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha92 (gecko code 193, run
  292067e6 ckpt 2955: alpha92 (gecko code 193) gated by ship.sh (core+app suites, lint, handoff se
  df0b4a8a ckpt 2954: gated kestrel-g-v0.3.0-alpha92 (gecko code 193) and pushed it: handoff self-
  2db9f72a ckpt 2952: pre-ship: alpha92: quality choices for pornhub in Kestrel's player, and a Sa
  ee6c59f9 ckpt 2938: PERF7c step 1: Settings > Network lab 'Safe drawing' (lab_draw, default ON) 
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
