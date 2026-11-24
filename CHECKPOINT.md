# CHECKPOINT 3269 — read me first, then TASKS.md

**Written:** 2026-11-24T20:52:26Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `8821fc21` (this checkpoint is the commit after it)

## Just done
probe v2: calls POST /releases directly (draft and real, target sha / branch / default branch) and gh release create with a file after a checkout; cleans up. v1 showed: token CAN create tag refs, rulesets none, tag protection none, repo public; gh 2.104.0 needs a git checkout

## Do this next
Dispatch android.yml app=lab lab_suite=probe full_build=false again, read job log; also read the opt lab result (run 45480427478: soak report auto-tune-quick row) after

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M .github/workflows/android.yml

## Last ten checkpoints
```
  8821fc21 ckpt 3268: alpha106 release publish fails with HTTP 403 twice (token has contents: writ
  8caaa5ba ckpt 3266: OPT1: Auto-tune integrated (BenchServer, Store, Apply, Guard, restart trampo
  59502d88 ckpt 3259: alpha106 (code 207) gated (ckpt 3257, head 0f14013b) and dispatched ONCE: ru
  0f14013b ckpt 3257: gated kestrel-g-v0.3.0-alpha106 (gecko code 207) and pushed it: handoff self
  c510bc41 ckpt 3256: OPT1: the Android-side sources and patch scripts are now saved IN the repo (
  f92c5435 ckpt 3255: OPT1: core done + 25 tests green, bench pages + web test green, tools/optimi
  59efade6 ckpt 3253: FF1: alpha106 prepared (code 207, GeckoView 161): lab smoke GREEN on 161 (ru
  29576308 ckpt 3230: FF1: GeckoView 161.0.20261118180411 + webcompat 161.0 pinned, clean compile,
  e7367846 ckpt 3222: wrote FF1 (new Firefox version into Kestrel, optimised for the hardware) and
  2e56492f ckpt 3221: alpha105 (code 206) SHIPPED: run 45352897624 green, Release kestrel-g-v0.3.0
```
