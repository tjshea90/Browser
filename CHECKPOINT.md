# CHECKPOINT 1117 — read me first, then TASKS.md

**Written:** 2026-11-12T17:33:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `0c8e3bb` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha22 (gecko code 121) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha22 speed: tabs stay ready by free memory (Settings > Tabs kept ready), a tab comes back with its Back stack and scroll position after the app was closed, Back shows the page's picture while a page that cannot be cached loads again, a preview covers a tab that loads again, opening a link no longer loads the old tab too, downloads step aside while a page loads, faster history database, fixes and lighter page scripts in the ad blocker"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha22), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha22 "alpha22 speed: tabs stay ready by free memory (Settings > Tabs kept ready), a tab comes back with its Back stack and scroll position after the app was closed, Back shows the page's picture while a page that cannot be cached loads again, a preview covers a tab that loads again, opening a link no longer loads the old tab too, downloads step aside while a page loads, faster history database, fixes and lighter page scripts in the ad blocker"
NEVER ship kestrel-g-v0.3.0-alpha22 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5c226ea ckpt 1103: AA4: K1 K2 K5 K6 verified on the emulator (evidence in research/SPEED3.md 6.1
  084b5ba ckpt 1101: AA4 speed batch code step: K1 LiveBudget (core, 9 tests) + RAM-adaptive live 
  6fc25a2 ckpt 1077: Reviews saved: research/PERF_KOTLIN.md, research/PERF_JS.md, research/PROMPT_
  7ceb366 ckpt 1075: AA3 audit done by an independent read-only agent: research/PROMPT_AUDIT.md (2
  9c34e86 ckpt 1072: AA1 proven on the emulator (simulated VPN: Network line, trr=0, http3 off, ti
  a0a56be ckpt 1069: AA2 + AA1 core done: Courier closed in the browser (every option OFF by defau
  ac00a1f ckpt 1061: Owner message 14 written into TASKS.md as AA1-AA4 in their words (INBOX has i
  d75864a ckpt 1057: Owner message 13 ANSWERED and SHIPPED: Kestrel Courier alpha3 (kestrel-courie
  5c2891c ckpt 1054: Docs: CLAUDE.md + BRIEF.md record the owner's unmetered/5 Mbit/s facts and th
  95c9050 ckpt 1053: gated kestrel-g-v0.3.0-alpha21 (gecko code 120) and pushed it: handoff self-t
```

(13 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
