# CHECKPOINT 2654 — read me first, then TASKS.md

**Written:** 2026-11-18T02:38:32Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `cba14e7e` (this checkpoint is the commit after it)

## Just done
alpha69 DISPATCHED ONCE: android.yml run 44224672021 (run_number 225, app=gecko full_build=true, ref ccr-587430ac-royssd, head cba14e7e, started 02:37:51Z, note = docs/handoff/ALPHA69_RELEASE_NOTE.txt). DO NOT DISPATCH ALPHA69 AGAIN. A background loop polls git ls-remote --tags for kestrel-g-v0.3.0-alpha69 (the tag appears when the build is green). Contents: CR1c LastRun note + Diagnostics 'Last run' (proven on the emulator by lab run 44223382840 suite lr: both kill cases said so), DM1 (replayed notification intent ignored + small window waits for restored rows and never lists old rows), Health lines downloads banner/window/open-by-tap/open-ignored.

## Do this next
when the tag exists: mcp__github__list_jobs via actions_list list_workflow_jobs 44224672021 (all green?), mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha69 (APK + sha256 + dm), then bash tools/record-release.sh kestrel-g-v0.3.0-alpha69 "$(tr -d '\n' < docs/handoff/ALPHA69_RELEASE_NOTE.txt)" (identical note), tick TASKS CR1d/DM1c, HANDOFF.md alpha69 entry, answer the owner with the Release link on its own line (plain words; open items: tablet test of DM1 + send Diagnostics 'Last run' if the crash returns; activity 2591 ms question)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  cba14e7e ckpt 2653: gated kestrel-g-v0.3.0-alpha69 (gecko code 170) and pushed it: handoff self-
  f8747df3 ckpt 2652: CR1: lab run 44214894368 READ: inconclusive (emulated system died twice, You
  250c6e4b ckpt 2651: DM1 coded: (1) BrowserActivity.openDownloadsAsked + core ManagerIntent (a re
  70eca79a ckpt 2650: Owner message 2 of this session written into TASKS.md BEFORE any code: DM1 (
  c2a91bcb ckpt 2649: CR1: waiting on two things. (1) lab run 44214894368 (yt suite, ends about 02
  92109d90 ckpt 2648: CR1c CODED: app/diag/LastRun.kt (note of how the last run ended, written onl
  a5cabd37 ckpt 2647: CR1: lab run DISPATCHED ONCE: android.yml run 44214894368 (run_number 223, a
  f346b9f1 ckpt 2646: CR1: added the lab suite 'yt' (tools/emu/soak.py yt_journey + yt_after + mem
  05bdbf31 ckpt 2645: CR1 written into TASKS.md in the owner's words (YouTube crash investigation)
  cef3338a ckpt 2644: gated kestrel-g-v0.3.0-alpha68 (gecko code 169) and pushed it: handoff self-
```
