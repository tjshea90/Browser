# CHECKPOINT 2656 — read me first, then TASKS.md

**Written:** 2026-11-18T03:06:39Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `f96eabef` (this checkpoint is the commit after it)

## Just done
QL1 page side DONE: quality-main.js (MAIN: notes hls.js / dash.js / Shaka players), quality.js (ISOLATED: yt, jw, vjs, hls, dash, shaka, <source> adapters), player.js reports q/qc/qf and runs the 'ql' command, manifest lists both, web-tests/quality.test.js 12 green (mocks only, honest header). NOT done yet: Kotlin side (PlayerController parses q, hasPageLevels/setPageQuality, BrowserActivity qualityAvailable + openPageQuality rows + pagePicked), core PageLevels.clean + test, lab scene, research/QL1_QUALITY.md, release alpha70. alpha69 build (run 44224672021) still being polled by tagpoll69.log.

## Do this next
QL1 Kotlin: core/player/PageLevels.kt (clean ids/labels, max 12 rows, Automatic first) + PageLevelsTest; PlayerController.onMessage 'vst' parses q (JSONArray of [id,label]) + qc + qf, pageLevels()/hasPageLevels()/setPageQuality(id) -> commands.add({a:ql,q:id}); BrowserActivity: qualityAvailable lambda || player.hasPageLevels(), openPageQuality uses page levels when present, pagePicked handles PageLevels tag; then npm test + gradle tests; research/QL1_QUALITY.md; lab scene

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b73892a9 ckpt 2655: QL1 written into TASKS.md in the owner's words (quality choice in full scree
  8033e054 ckpt 2654: alpha69 DISPATCHED ONCE: android.yml run 44224672021 (run_number 225, app=ge
  cba14e7e ckpt 2653: gated kestrel-g-v0.3.0-alpha69 (gecko code 170) and pushed it: handoff self-
  f8747df3 ckpt 2652: CR1: lab run 44214894368 READ: inconclusive (emulated system died twice, You
  250c6e4b ckpt 2651: DM1 coded: (1) BrowserActivity.openDownloadsAsked + core ManagerIntent (a re
  70eca79a ckpt 2650: Owner message 2 of this session written into TASKS.md BEFORE any code: DM1 (
  c2a91bcb ckpt 2649: CR1: waiting on two things. (1) lab run 44214894368 (yt suite, ends about 02
  92109d90 ckpt 2648: CR1c CODED: app/diag/LastRun.kt (note of how the last run ended, written onl
  a5cabd37 ckpt 2647: CR1: lab run DISPATCHED ONCE: android.yml run 44214894368 (run_number 223, a
  f346b9f1 ckpt 2646: CR1: added the lab suite 'yt' (tools/emu/soak.py yt_journey + yt_after + mem
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
