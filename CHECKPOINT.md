# CHECKPOINT 2823 — read me first, then TASKS.md

**Written:** 2026-11-21T17:37:38Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `7131d543` (this checkpoint is the commit after it)

## Just done
LD4c done: diag_digest.py LOAD ANALYSIS (per site: loads, load/first paint/first byte, requests + cached share, 3 s+ requests, lateness share, verdict CPU-bound / NETWORK-bound / SHIELDS-heavy with numbers, slowest requests, Shields A/B, cost per request incl. a fallback that reads the older uploads' Blocker text, the tablet's numbers); test_handoff 83/83

## Do this next
Gate: ./gradlew -q :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest :app:lintDebug (background, poll log for GRADLE_EXIT, check XML), then cd web-tests && npm test (background); then version bump alpha85 (geckoapp/build.gradle.kts versionCode 186 / 0.3.0-alpha85), bash tools/sync-ext-version.sh, docs/handoff/ALPHA85_RELEASE_NOTE.txt, bash ship.sh gecko, ONE dispatch, record-release. Then update research/LIVE_DIAG.md + SLOW_LOADS.md with what shipped, tick LD4a-e, answer the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  0c525795 ckpt 2822: LD4b app side done and compiling: GeckoEngine.networkProfileCode, ShieldsBri
  ca1f0fe2 ckpt 2821: LD4b extension side done: pagetime.js (ES5, top frame, document_start, laten
  029be5ca ckpt 2820: LD4b core done: DiagScrub caps (event 480, snapshot lines 1600: the CPU/Bloc
  0b7be92b ckpt 2819: LD4a done: research/SLOW_LOADS.md written from the real uploads: claude.ai 1
  a6b43548 ckpt 2818: LD4 written into TASKS.md in the owner's words (richer live diagnostics + cl
  db4c9602 ckpt 2817: alpha84 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/
  bcfe8a73 ckpt 2816: alpha84 build running: run id 44924979560 (run_number 265, head 2cf128f4), d
  4a42d19d ckpt 2815: alpha84 gated (core+app tests, gecko compile, lint 0 errors, self-test 80/80
  2cf128f4 ckpt 2814: gated kestrel-g-v0.3.0-alpha84 (gecko code 185) and pushed it: handoff self-
  0efd4ff7 ckpt 2813: ALPHA84 release note written (docs/handoff/ALPHA84_RELEASE_NOTE.txt); platfo
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
