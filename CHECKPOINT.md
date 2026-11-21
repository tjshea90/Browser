# CHECKPOINT 2793 — read me first, then TASKS.md

**Written:** 2026-11-21T06:57:34Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `c8e71ca4` (this checkpoint is the commit after it)

## Just done
LD1e: full gate green (:core:test, :app:testDebugUnitTest, :geckoapp:compileDebugKotlin, :app:lintDebug exit 0); research/LIVE_DIAG.md + CLAUDE.md/BRIEF decision written; version bumped to 182 / 0.3.0-alpha81, ALPHA81 release note written (ASCII, no quotes)

## Do this next
SHIP alpha81: bash ship.sh gecko with the note from docs/handoff/ALPHA81_RELEASE_NOTE.txt, ONE dispatch of android.yml app=gecko full_build=true ref=claude/sweet-gates-gr3bj5, verify tag + jobs + release, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha81 with the identical note; then answer the owner (alpha80 Back + LD1 answer, token how-to, what was NOT run on the tablet, open items)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/build.gradle.kts
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  a6c87bc8 ckpt 2792: LD1d done: tools/diag_digest.py (crashes/kills, slow sites by host, playback
  2a79bf47 ckpt 2791: LD1c tested: LiveDiagTest 13/13 (switch off = nothing kept, switch survives 
  2050ef12 ckpt 2790: LD1c: app side written + geckoapp compiles: diag/LiveDiag.kt (master switch,
  5d077b35 ckpt 2789: LD1b: core livediag package written + tested (DiagScrub: links -> host, sens
  808aa4f6 ckpt 2787: alpha80 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha80, gecko code 181, run
  f12802a2 ckpt 2786: LD1 written into TASKS.md in the owner's words (live diagnostics engine with
  5c4edd25 ckpt 2785: REL80b: alpha80 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  0c2b1e08 ckpt 2784: gated kestrel-g-v0.3.0-alpha80 (gecko code 181) and pushed it: handoff self-
  0f713e4a ckpt 2783: BK10 PROVEN on the real engine (lab run 44873006097): no-store page + Instan
  08d2776b ckpt 2782: BK10 lab run 44872447722 (suite back): all three rounds failed before any Ba
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
