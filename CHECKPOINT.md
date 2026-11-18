# CHECKPOINT 2650 — read me first, then TASKS.md

**Written:** 2026-11-18T01:30:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `c2a91bcb` (this checkpoint is the commit after it)

## Just done
Owner message 2 of this session written into TASKS.md BEFORE any code: DM1 (download manager pops up by itself) and CR1e (the Diagnostics he pasted at 01:00:31Z, 24 s after a cold start: no crash note, no engine crash report, no gecko.kill/crash counters).

## Do this next
DM1a: read the download popup code (downloadPopup.attach/resumed in geckoapp BrowserActivity onStart, MiniTracker, DownloadController.restore / resumeInterrupted, DownloadsView) and list every path that shows it without a new download; then CR1e analysis into research/CR1_YOUTUBE_CRASH.md; lab run 44214894368 + jvm-suite.log still pending, heartbeat trig_014tWDqpP59iW4BtNv5mT9KD at 02:24Z

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  c2a91bcb ckpt 2649: CR1: waiting on two things. (1) lab run 44214894368 (yt suite, ends about 02
  92109d90 ckpt 2648: CR1c CODED: app/diag/LastRun.kt (note of how the last run ended, written onl
  a5cabd37 ckpt 2647: CR1: lab run DISPATCHED ONCE: android.yml run 44214894368 (run_number 223, a
  f346b9f1 ckpt 2646: CR1: added the lab suite 'yt' (tools/emu/soak.py yt_journey + yt_after + mem
  05bdbf31 ckpt 2645: CR1 written into TASKS.md in the owner's words (YouTube crash investigation)
  cef3338a ckpt 2644: gated kestrel-g-v0.3.0-alpha68 (gecko code 169) and pushed it: handoff self-
  e38fb0d8 ckpt 2643: alpha67 SHIPPED and RECORDED (Release kestrel-g-v0.3.0-alpha67 code 168, run
  765d1c8a ckpt 2642: alpha67 DISPATCHED ONCE (android.yml app=gecko full_build=true, ref ccr-e89b
  d8340975 ckpt 2641: gated kestrel-g-v0.3.0-alpha67 (gecko code 168) and pushed it: handoff self-
  fd0ea0b5 ckpt 2640: Diagnostics line 'start oat' added (BrowserActivity.oatSizes: sizes of oat/<
```
