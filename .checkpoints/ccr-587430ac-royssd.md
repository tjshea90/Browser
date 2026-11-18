# CHECKPOINT 2652 — read me first, then TASKS.md

**Written:** 2026-11-18T02:27:18Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `220b4cbe` (this checkpoint is the commit after it)

## Just done
CR1: lab run 44214894368 READ: inconclusive (emulated system died twice, YouTube pages never loaded in 90 s on the software-GL emulator, ANR in HardwareRenderer.nSyncAndDrawFrame; no app restart / Java crash / engine process death; recorded in research/CR1_YOUTUBE_CRASH.md). Added lab suite 'lr' (soak.py lastrun_round: kill -9 the app in front and in the background, the next Diagnostics must say 'ended WHILE ON SCREEN' / 'in the background when it ended'), Health lines downloads banner/window/open-by-tap/open-ignored for DM1; compile + tests green.

## Do this next
dispatch ONCE: android.yml app=lab lab_suite=lr lab_minutes=5 lab_profile=standard lab_net=full ref ccr-587430ac-royssd (about 10 min); read report.md section 'How the last run ended'; then alpha69: versionCode 170 / 0.3.0-alpha69 in geckoapp/build.gradle.kts, bash tools/sync-ext-version.sh, docs/handoff/ALPHA69_RELEASE_NOTE.txt, bash ship.sh gecko, ONE dispatch app=gecko full_build=true, verify, tools/record-release.sh

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/CR1_YOUTUBE_CRASH.md

## Last ten checkpoints
```
  250c6e4b ckpt 2651: DM1 coded: (1) BrowserActivity.openDownloadsAsked + core ManagerIntent (a re
  70eca79a ckpt 2650: Owner message 2 of this session written into TASKS.md BEFORE any code: DM1 (
  c2a91bcb ckpt 2649: CR1: waiting on two things. (1) lab run 44214894368 (yt suite, ends about 02
  92109d90 ckpt 2648: CR1c CODED: app/diag/LastRun.kt (note of how the last run ended, written onl
  a5cabd37 ckpt 2647: CR1: lab run DISPATCHED ONCE: android.yml run 44214894368 (run_number 223, a
  f346b9f1 ckpt 2646: CR1: added the lab suite 'yt' (tools/emu/soak.py yt_journey + yt_after + mem
  05bdbf31 ckpt 2645: CR1 written into TASKS.md in the owner's words (YouTube crash investigation)
  cef3338a ckpt 2644: gated kestrel-g-v0.3.0-alpha68 (gecko code 169) and pushed it: handoff self-
  e38fb0d8 ckpt 2643: alpha67 SHIPPED and RECORDED (Release kestrel-g-v0.3.0-alpha67 code 168, run
  765d1c8a ckpt 2642: alpha67 DISPATCHED ONCE (android.yml app=gecko full_build=true, ref ccr-e89b
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
