# CHECKPOINT 221 — read me first, then TASKS.md

**Written:** 2026-11-11T04:49:18Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `85eab84` (this checkpoint is the commit after it)

## Just done
ship.sh gecko OK; dispatched android.yml app=gecko full_build=true: run 42961586675 on 85eab84, started 04:48Z (alpha3, code 102). Emulator smoke test passed before shipping.

## Do this next
Wait for run 42961586675 (~5 min): actions_list list_workflow_jobs; green -> get_release_by_tag kestrel-g-v0.2.0-alpha3 -> bash tools/record-release.sh kestrel-g-v0.2.0-alpha3 <note> -> tell the owner (what to test: typing, claude.ai, Menu>Diagnostics>Refresh screen). Then B1a: pure-Kotlin download engine in core/ (research/DOWNLOADS.md sections 2-4), tests first.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  85eab84 ckpt 220: gated kestrel-g-v0.2.0-alpha3 (gecko code 102) and pushed it: handoff self-tes
  57a6d62 ckpt 219: alpha3 candidate: freeze fixes verified on emulator (worker bridge, cosmetics 
  04dab0c ckpt 214: A2/A4 part: gecko-prefs.txt -> YAML config (AV1 off, memory/cache budget, no a
  1739d76 ckpt 209: A3 evidence layer: core RingLog (+tests), app Health (watchdog, frame jank, re
  9c46642 ckpt 175: A3 P0 part 1 (Kestrel G): GeckoView attached ONCE (ntp/error as overlays, setS
  21706f6 ckpt 119: A1 part 1: native UI animations off - instant pressed-state overlay instead of
  3cd7e40 ckpt 117: M1-M2 done: tools/sync.sh (auto-merge other sessions), per-branch cards .check
  cb69194 ckpt 109: Owner request 2 recorded: INBOX captured by the hook, TASKS.md boxes M1-M4 R1-
  286f09a ckpt 104: GitHub Actions pipeline proven: run 42951742468 built, signed, verified and pu
  ecfd90c ckpt 98: ship.sh gecko OK (local gates green, pushed). Dispatched android.yml app=both f
```
