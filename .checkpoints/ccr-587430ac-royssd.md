# CHECKPOINT 2658 — read me first, then TASKS.md

**Written:** 2026-11-18T03:19:23Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `1e2374d9` (this checkpoint is the commit after it)

## Just done
QL1 + VS1 + DM2 coded and self-tested. QL1: page side (quality-main.js, quality.js, player.js q/qc/qf + ql command, manifest), core PageLevels (+7 tests), PlayerController/BrowserActivity (Quality button from the page's own list), lab scene ql (lab page fsbox yt=1), web tests 593/593 green, JVM suite green, research/QL1_QUALITY.md. VS1: VideoFlow.labDownloadBest now takes an HLS/DASH candidate through list + first-row download (lab videodl-stream lines); lab server vidlab_srv.py got /watch3?v=auto|click|nest|worker; lab scene vs (soak.py vs_scene). DM2: lab scene dm (soak.py dm_scene: new download -> banner; kill -9 front/background + cold start -> no banner/window/open; launch intent with OPEN_DOWNLOADS opens once, replay from history ignored). Lab suites ql, vs, dm added to android.yml + emu-lab.yml (YAML apostrophe bug I introduced was fixed first).

## Do this next
dispatch THREE lab runs ONCE each on ref ccr-587430ac-royssd (inputs app=lab lab_profile=standard lab_net=full lab_minutes=5): lab_suite=ql, lab_suite=vs, lab_suite=dm; read each report.md (sections 'A vidsrc-style embed', 'The download window and list at a start'); alpha69 build run 44224672021 is still polled by tagpoll69.log (tag kestrel-g-v0.3.0-alpha69, then tools/record-release.sh with the note in docs/handoff/ALPHA69_RELEASE_NOTE.txt)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  96a653ee ckpt 2657: Owner message 3 of this session written into TASKS.md BEFORE any code: VS1 (
  1587c92f ckpt 2656: QL1 page side DONE: quality-main.js (MAIN: notes hls.js / dash.js / Shaka pl
  b73892a9 ckpt 2655: QL1 written into TASKS.md in the owner's words (quality choice in full scree
  8033e054 ckpt 2654: alpha69 DISPATCHED ONCE: android.yml run 44224672021 (run_number 225, app=ge
  cba14e7e ckpt 2653: gated kestrel-g-v0.3.0-alpha69 (gecko code 170) and pushed it: handoff self-
  f8747df3 ckpt 2652: CR1: lab run 44214894368 READ: inconclusive (emulated system died twice, You
  250c6e4b ckpt 2651: DM1 coded: (1) BrowserActivity.openDownloadsAsked + core ManagerIntent (a re
  70eca79a ckpt 2650: Owner message 2 of this session written into TASKS.md BEFORE any code: DM1 (
  c2a91bcb ckpt 2649: CR1: waiting on two things. (1) lab run 44214894368 (yt suite, ends about 02
  92109d90 ckpt 2648: CR1c CODED: app/diag/LastRun.kt (note of how the last run ended, written onl
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
