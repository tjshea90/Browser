# CHECKPOINT 2402 — read me first, then TASKS.md

**Written:** 2026-11-15T17:08:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `5c8563ee` (this checkpoint is the commit after it)

## Just done
lab vc run 2 (43738479064, commit b5fd15c4) on the REAL Firefox engine: VC1 bar shown on naked / shadow-root / shadow-root + frame over it, correctly NOT shown where the page has its own controls (own); LP1 long press = Save video sheet (landscape.mp4, 360p THIS VIDEO, Download 1.9 MB, Copy video address) on all four lab pages and never the floating panel; pictures 03/04 in the run show the bar over a shadow-root player and the sheet; the live Yahoo quote page was not conclusive (layout changed, my tap opened an article): the scene now uses finance.yahoo.com/videos/ (player in a shadow root at the top: probe) and only photographs the quote page

## Do this next
dispatch lab vc a third time (Yahoo /videos/ player: tap + long press), run the ship gates meanwhile; then hour run; ship alpha48

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b5fd15c4 ckpt 2401: lab vc run 1 (43737466605) read: LP1 long press menu on all four lab pages a
  0acf028c ckpt 2400: lab driver: hotspot shaping without the emulator delay option (it made hacke
  4e233ebe ckpt 2399: VC1/LP1/RV1-RV9 verified: Kotlin suite (core + app + geckoapp tests + geckoa
  31e0d580 ckpt 2398: RV3 (updater spoof: fromUpdater needs the app-private download kind), RV4 (r
  a61cff67 ckpt 2397: RV1 (site orientation lock = Android constants, verified in the GeckoView 16
  fa81920c ckpt 2396: VC1 + LP1 first working version: vfind.js (finds videos in shadow roots / un
  6b3d7aba ckpt 2395: independent review of the alpha48 diff read (research/REVIEW_ALPHA48.md, 9 f
  a74e3649 ckpt 2394: wrote owner message 44 into TASKS.md in the owner's words: VC1 (video has no
  9ae7442b ckpt 2393: re-dispatched the one-hour hotspot run WITH the lab-server shaping: android.
  13ac03b9 ckpt 2392: lab server follows the hotspot too: soaklab.py LAB_RATE_KBPS / LAB_DELAY_MS 
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
