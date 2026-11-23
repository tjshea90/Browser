# CHECKPOINT 3091 — read me first, then TASKS.md

**Written:** 2026-11-23T23:18:55Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `3581dee6` (this checkpoint is the commit after it)

## Just done
alpha98 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha98 (gecko code 199, run 45270482635 green on attempt 1, Release has APK + sha256 + dm, BUILDLOG line pushed). PERF15b shipped, NOT confirmed on the tablet

## Do this next
Next: waiting for the owner: install alpha98, switch between tabs after leaving the app and coming back, Settings > Diagnostics > Send now; python3 -B -I tools/diag_digest.py --days 1: TABS section (sleeps by budget, switch-done live vs revive first-frame ms, engine kills); if tabs still reload read the hibernate lines (budget / avail); if the lag is between LIVE tabs read switch-done kind=live and the curtain lines, then fix the session swap (picture of the target during the swap). Still open: FLK1 (harden PhoneBackendTest, ServerTest, StreamEngineTest), PERF14e Calmer redrawing A/B, per-quality fps, claude.ai FPP A/B, second film (PERF9)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  946e2e68 ckpt 3089: alpha98 (gecko code 199) gated by ship.sh and ONE dispatch made on claude/ki
  665bc7a9 ckpt 3088: gated kestrel-g-v0.3.0-alpha98 (gecko code 199) and pushed it: handoff self-
  6b92a4b3 ckpt 3087: pre-ship: alpha98: switching between tabs no longer reloads them. Your diagn
  a5e04498 ckpt 3085: PERF15 coded (not gated): LiveBudget.afterTrim level 60+ follows free memory
  adeb2192 ckpt 3082: owner after alpha97: Back is faster and images attach (CONFIRMED); NEW PERF1
  8a9c210e ckpt 3081: alpha97 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha97 (gecko code 198, run
  a7a096c8 ckpt 3079: alpha97 run 45259809870 FAILED at test-jvm on the GitHub runner: core Stream
  d47330ac ckpt 3078: alpha97 (gecko code 198) gated by ship.sh (core + app + geckoapp suites, han
  80d592db ckpt 3077: gated kestrel-g-v0.3.0-alpha97 (gecko code 198) and pushed it: handoff self-
  fc06903d ckpt 3074: PERF14 coded (not gated): photo attach copy (core PickStore + tests, Browser
```
