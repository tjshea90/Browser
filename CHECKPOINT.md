# CHECKPOINT 3059 — read me first, then TASKS.md

**Written:** 2026-11-23T20:06:06Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `e65a2a38` (this checkpoint is the commit after it)

## Just done
PERF14a research written: photo attach root cause (GeckoView 160 FilePrompt.confirm resolves content URIs to RAW PATHS the engine opens itself: needs storage permission on Android 10+, so the File is unreadable: probe page-got decode=failed bytes=other) -> research/ATTACH_ROOT_CAUSE.md; Back (alpha96 left=refused-loaded while the engine's load state is still loading) -> research/INSTANT_BACK.md section 5

## Do this next
Next: code alpha97: (1) core PickStore (safe name, copy, sweep, wipe) + tests; BrowserActivity.onActivityResult copies picks to cache/picks and answers file:// URIs; sweep/wipe hooks; remove READ_EXTERNAL_STORAGE/READ_MEDIA_IMAGES from geckoapp manifest + the Settings row + Flavor.hasFilePermission (update OwnerRulesTest perf10); (2) Back: content.js click listener window.stop() + background bkm answer + BackLab.pageHide(+stop) + Settings Instant Back text + web tests; (3) speed: Settings lab switch layout.frame_rate=30 (default off) + digest setting A/B; research/PAGE_SPEED_2026-11-23.md with software drawing cost per site (claude.ai first paint 2.0 s -> 14.6 s, others unchanged); then ship alpha97

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M research/INSTANT_BACK.md

## Last ten checkpoints
```
  678153fe ckpt 3057: PERF14 written to TASKS.md (owner after alpha96: Back still slow although th
  d8a9f0e7 ckpt 3056: alpha96 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha96 (gecko code 197, run
  ca317a33 ckpt 3054: alpha96 run 45245013667 FAILED at test-jvm on the GitHub runner: two timing 
  48e2535b ckpt 3053: alpha96 (gecko code 197) gated by ship.sh (core + app + geckoapp suites, han
  09eaca29 ckpt 3052: gated kestrel-g-v0.3.0-alpha96 (gecko code 197) and pushed it: handoff self-
  bd5370ef ckpt 3049: PERF12 + PERF13 coded and tested (targeted): cut-short loads, page loop, qui
  fab351e4 ckpt 3039: PERF13 written to TASKS.md (owner: instant first load and instant Back, find
  d41227d4 ckpt 3034: PERF12 written to TASKS.md (owner 2026-11-23: look at the latest diagnostics
  b375e29e ckpt 3033: PERF11a done + research written (alpha95 uploads: Safe drawing + PBO off app
  489d966d ckpt 3032: PERF11 written to TASKS.md (owner after alpha95: invisible blocks covering w
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
