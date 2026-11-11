# CHECKPOINT 419 — read me first, then TASKS.md

**Written:** 2026-11-11T17:52:34Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `5f068ba` (this checkpoint is the commit after it)

## Just done
alpha7 build DISPATCHED: Actions run 43072284547 (android.yml, ref ccr-a57c163e-mtq8js, app=gecko, full_build=true) started 17:52Z; R1 neon, R3 pull-to-refresh, R2 Smoother scroll feel default, S1/S2 verified on the emulator and in this release; also Neon.covered (render thread 38% -> 3% under Downloads)

## Do this next
WAIT for run 43072284547 (a background until-loop watches git ls-remote for tag kestrel-g-v0.2.0-alpha7; check with mcp__github__actions_list list_workflow_runs android.yml); when conclusion=success: mcp__github__get_release_by_tag kestrel-g-v0.2.0-alpha7 (APK present), bash tools/record-release.sh kestrel-g-v0.2.0-alpha7 '<note from ckpt 418>', ckpt, then reply to the owner with the Release link on its own line (S1 how to use Try again, S2, R1 neon where to look, R2 Smoother + Compare, R3 how to pull; emulator-only caveats; open items: D1 blocking engine, P5 cold-start glitch, swipe-to-close in tab grid, Settings restyle, SEC-1/LIC-1/UI-2/UI-3, Cromite A/B, PullTracker JVM tests)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5f068ba ckpt 418: gated kestrel-g-v0.2.0-alpha7 (gecko code 106) and pushed it: handoff self-tes
  903dc4e ckpt 413: R2 first pass DONE on the emulator: Gecko source says display-port multipliers
  6cbe9d9 ckpt 406: R3 pull-down-to-refresh DONE + emulator-verified (KestrelGeckoView pull tracki
  a2797c7 ckpt 400: R1 neon DONE on the emulator (system animations off): pulse no longer gated on
  dec2a93 ckpt 392: S1+S2 code done: updater storage plan (core UpdateLogic.storagePlan + tests; U
  f0bff4b ckpt 384: Owner request 7 recorded (S1 in-app update blocked by INSTALL_FAILED_INSUFFICI
  fcf63d9 ckpt 382: Owner request 6 recorded in TASKS.md (R1 neon not visible, R2 scrolling stutte
  4ac5081 ckpt 376: alpha6 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha6, gecko code 105, run 430556
  69d37b2 ckpt 372: gated kestrel-g-v0.2.0-alpha6 (gecko code 105) and pushed it: handoff self-tes
  3c8f9fe ckpt 371: pre-ship: alpha6: a page that Android frees to save memory now comes back wher
```
