# CHECKPOINT 2649 — read me first, then TASKS.md

**Written:** 2026-11-18T01:28:55Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `92109d90` (this checkpoint is the commit after it)

## Just done
CR1: waiting on two things. (1) lab run 44214894368 (yt suite, ends about 02:25Z). (2) full JVM suite in background (scratchpad jvm-suite.log, 'finished rc=0' at the end). Heartbeat: send_later trigger trig_014tWDqpP59iW4BtNv5mT9KD fires 02:24Z into this session.

## Do this next
at 02:24Z (or when the jvm-suite notification arrives): read jvm-suite.log; read lab run 44214894368 (list_workflow_jobs, artifact emulator-lab-text, report.md section 'YouTube: leaving the app'); fill research/CR1_YOUTUBE_CRASH.md 'Lab result'; tick CR1a/b/c; then alpha69 by the CLAUDE.md recipe (versionCode 170, name 0.3.0-alpha69, tools/sync-ext-version.sh, docs/handoff/ALPHA69_RELEASE_NOTE.txt ASCII, bash ship.sh gecko, ONE dispatch app=gecko full_build=true, verify, tools/record-release.sh) and answer the owner in plain words incl. what to send (Diagnostics lines 'Last run', 'EARLIER CRASH', 'Engine crash reports')

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  92109d90 ckpt 2648: CR1c CODED: app/diag/LastRun.kt (note of how the last run ended, written onl
  a5cabd37 ckpt 2647: CR1: lab run DISPATCHED ONCE: android.yml run 44214894368 (run_number 223, a
  f346b9f1 ckpt 2646: CR1: added the lab suite 'yt' (tools/emu/soak.py yt_journey + yt_after + mem
  05bdbf31 ckpt 2645: CR1 written into TASKS.md in the owner's words (YouTube crash investigation)
  cef3338a ckpt 2644: gated kestrel-g-v0.3.0-alpha68 (gecko code 169) and pushed it: handoff self-
  e38fb0d8 ckpt 2643: alpha67 SHIPPED and RECORDED (Release kestrel-g-v0.3.0-alpha67 code 168, run
  765d1c8a ckpt 2642: alpha67 DISPATCHED ONCE (android.yml app=gecko full_build=true, ref ccr-e89b
  d8340975 ckpt 2641: gated kestrel-g-v0.3.0-alpha67 (gecko code 168) and pushed it: handoff self-
  fd0ea0b5 ckpt 2640: Diagnostics line 'start oat' added (BrowserActivity.oatSizes: sizes of oat/<
  63fdb931 ckpt 2639: LAB7 CONFIRMATION lab run DISPATCHED ONCE: android.yml run 44137227924 (run_
```
