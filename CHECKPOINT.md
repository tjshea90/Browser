# CHECKPOINT 615 — read me first, then TASKS.md

**Written:** 2026-11-12T00:26:20Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `0cebcca` (this checkpoint is the commit after it)

## Just done
alpha14 RE-DISPATCHED: run 43127174913 (android.yml, head 0cebcca, 00:25:31Z, expect green about 00:32Z). The first run 43126355049 FAILED only in two StreamEngineTest assertions that raced the store (view COMPLETED before the store row/hidden record landed): fixed with an eventually() wait, 3x green under 6 CPU hogs. No tag/release was created by the failed run. DO NOT dispatch again unless this run fails.

## Do this next
WAIT for run 43127174913 (mcp__github__actions_list list_workflow_jobs 43127174913). If green: get_release_by_tag kestrel-g-v0.3.0-alpha14 shows the APK; VERIFY THE RESCUE with mcp__github__list_releases perPage 8 page 1 (kestrel-g-v0.3.0-alpha14 must be inside the first 8); bash tools/record-release.sh kestrel-g-v0.3.0-alpha14 "$(cat /tmp/claude-0/-home-user-Browser/30a5aa00-41c9-5bd3-87d1-b10052a52d84/scratchpad/note14.txt)"; ckpt; tell the owner. Work in progress meanwhile (uncommitted-safe, autosaved): X6 popup guard: geckoapp/.../kestrel-shields/guard-main.js (written, 15 Chromium tests pass in web-tests/guard.test.js) - NOT yet wired: needs manifest/background registration (contentScripts.register world MAIN), content.js relay (__kst_pop_v1 / __kst_cfg_v1 / __kst_ready_v1 events), cfg flags, Kotlin ShieldsBridge 'pop'/'guard' handlers, Prefs.blockPopups, onLoadRequest/newPopupSession native policy, chip UI.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  0cebcca ckpt 614: gated kestrel-g-v0.3.0-alpha14 (gecko code 113) and pushed it: handoff self-te
  fec7b3d ckpt 609: alpha14 DISPATCHED: run 43126355049 (android.yml, head 3ed1b75, started 00:18:
  3ed1b75 ckpt 608: gated kestrel-g-v0.3.0-alpha14 (gecko code 113) and pushed it: handoff self-te
  b646b31 ckpt 606: X1 DONE in code and tests: updater root cause found (GitHub lists releases by 
  6400e12 ckpt 600: owner request 9 written into TASKS.md as X1-X10 in the owner's words (INBOX ha
  f1f1d48 ckpt 597: alpha13 SHIPPED and recorded (run 43123971432 green, BUILDLOG written): https:
  e860a70 ckpt 595: alpha13 DISPATCHED on GitHub Actions: run 43123971432 (android.yml, head 19e3a
  19e3a6d ckpt 594: gated kestrel-g-v0.2.0-alpha13 (gecko code 112) and pushed it: handoff self-te
  16223f8 ckpt 592: D1 code complete and verified in unit tests: real-lists differential (63,378 r
  4316802 ckpt 571: D1 steps 1+2 DONE and verified: core/.../filter/ExtImage.kt (KEX1 little-endia
```
