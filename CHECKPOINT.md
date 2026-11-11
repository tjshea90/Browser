# CHECKPOINT 119 — read me first, then TASKS.md

**Written:** 2026-11-11T03:37:38Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `d8817da` (this checkpoint is the commit after it)

## Just done
A1 part 1: native UI animations off - instant pressed-state overlay instead of RippleDrawable, popups animationStyle 0, windowAnimationStyle null + animation-free alert dialog theme; app compiles.

## Do this next
Wait for research agents; then A2/A3 from their notes (Gecko prefs, filter work off the UI thread). Page-level animations come with the Gecko prefs/extension (ui.prefersReducedMotion).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  3cd7e40 ckpt 117: M1-M2 done: tools/sync.sh (auto-merge other sessions), per-branch cards .check
  cb69194 ckpt 109: Owner request 2 recorded: INBOX captured by the hook, TASKS.md boxes M1-M4 R1-
  286f09a ckpt 104: GitHub Actions pipeline proven: run 42951742468 built, signed, verified and pu
  ecfd90c ckpt 98: ship.sh gecko OK (local gates green, pushed). Dispatched android.yml app=both f
  cdbefdf ckpt 97: gated kestrel-g-v0.2.0-alpha2 (gecko code 101) and pushed it: handoff self-test
  d41eb73 ckpt 94: Ported the owner's save system (Launcher/Portfolio) into Browser: tools/{ckpt,a
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
