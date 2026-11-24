# CHECKPOINT 3256 — read me first, then TASKS.md

**Written:** 2026-11-24T19:49:13Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `f92c5435` (this checkpoint is the commit after it)

## Just done
OPT1: the Android-side sources and patch scripts are now saved IN the repo (docs/handoff/opt1-staging/, not compiled) so a cut-off session does not lose them

## Do this next
Same as before, but use docs/handoff/opt1-staging/ (not the scratchpad): after the alpha106 ship gate: python3 -B -I docs/handoff/opt1-staging/apply_opt.py, then apply_opt2.py, then apply_opt3.py; compile; tests; delete docs/handoff/opt1-staging once applied and ticked

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    ?? docs/handoff/opt1-staging/

## Last ten checkpoints
```
  f92c5435 ckpt 3255: OPT1: core done + 25 tests green, bench pages + web test green, tools/optimi
  59efade6 ckpt 3253: FF1: alpha106 prepared (code 207, GeckoView 161): lab smoke GREEN on 161 (ru
  29576308 ckpt 3230: FF1: GeckoView 161.0.20261118180411 + webcompat 161.0 pinned, clean compile,
  e7367846 ckpt 3222: wrote FF1 (new Firefox version into Kestrel, optimised for the hardware) and
  2e56492f ckpt 3221: alpha105 (code 206) SHIPPED: run 45352897624 green, Release kestrel-g-v0.3.0
  6fb30f29 ckpt 3219: alpha105 (code 206) gated (ckpt 3218, head 8b6eebf7) and dispatched ONCE: ru
  8b6eebf7 ckpt 3218: gated kestrel-g-v0.3.0-alpha105 (gecko code 206) and pushed it: handoff self
  c45aae47 ckpt 3217: PERF25: app unit tests green; bumped to alpha105 / code 206, release note do
  512c9e18 ckpt 3215: PERF25c: built the hardware drawing trial (core DrawTrial + DrawTrialTest + 
  6d20d18a ckpt 3203: wrote PERF25 (owner: Brave is smooth on the parlayapi site) into TASKS.md, n
```
