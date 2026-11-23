# CHECKPOINT 3081 — read me first, then TASKS.md

**Written:** 2026-11-23T20:58:58Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `e2cf5160` (this checkpoint is the commit after it)

## Just done
alpha97 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha97 (gecko code 198, run 45259809870 green on attempt 2, Release has APK + sha256 + dm, BUILDLOG line pushed). PERF14b/c/e shipped, NOT confirmed on the tablet; PERF14a/d done

## Do this next
Next: waiting for the owner: install alpha97; (1) attach one picture in claude.ai, then Settings > Diagnostics > Send now: python3 -B -I tools/diag_digest.py --days 1 FILE PICKS must show filepick copied + page-got decode=ok; (2) use Back on ordinary pages (tap a link, then Back): the BACK section shows left=kept+stop or refused-...+stop; (3) optionally Settings > Network lab > Calmer redrawing ON: SETTING A/B section. Then decide: if left is still refused-loaded+stop the page stays blocked by something else (research/INSTANT_BACK.md section 4 item 2); FLK1: harden the three flaky tests (PhoneBackendTest, ServerTest, StreamEngineTest) in their own commit

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  a7a096c8 ckpt 3079: alpha97 run 45259809870 FAILED at test-jvm on the GitHub runner: core Stream
  d47330ac ckpt 3078: alpha97 (gecko code 198) gated by ship.sh (core + app + geckoapp suites, han
  80d592db ckpt 3077: gated kestrel-g-v0.3.0-alpha97 (gecko code 198) and pushed it: handoff self-
  fc06903d ckpt 3074: PERF14 coded (not gated): photo attach copy (core PickStore + tests, Browser
  75f112b7 ckpt 3059: PERF14a research written: photo attach root cause (GeckoView 160 FilePrompt.
  678153fe ckpt 3057: PERF14 written to TASKS.md (owner after alpha96: Back still slow although th
  d8a9f0e7 ckpt 3056: alpha96 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha96 (gecko code 197, run
  ca317a33 ckpt 3054: alpha96 run 45245013667 FAILED at test-jvm on the GitHub runner: two timing 
  48e2535b ckpt 3053: alpha96 (gecko code 197) gated by ship.sh (core + app + geckoapp suites, han
  09eaca29 ckpt 3052: gated kestrel-g-v0.3.0-alpha96 (gecko code 197) and pushed it: handoff self-
```
