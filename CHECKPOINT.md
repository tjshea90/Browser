# CHECKPOINT 2397 — read me first, then TASKS.md

**Written:** 2026-11-15T16:32:55Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `34f23f83` (this checkpoint is the commit after it)

## Just done
RV1 (site orientation lock = Android constants, verified in the GeckoView 160 bytecode), RV2 (full screen ends when its tab dies / sleeps / closes / is replaced; Back forces the exit after 600 ms), RV8 (orientation reset in onCreate/onDestroy) coded, not yet compiled

## Do this next
compile + run JVM tests (background gradle: core + app + geckoapp), then RV3 (updater spoof: BrowserActivity.openDownload fromUpdater), RV4, RV5, RV6, RV7, RV9; web tests running in background (webtests3.log)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt

## Last ten checkpoints
```
  fa81920c ckpt 2396: VC1 + LP1 first working version: vfind.js (finds videos in shadow roots / un
  6b3d7aba ckpt 2395: independent review of the alpha48 diff read (research/REVIEW_ALPHA48.md, 9 f
  a74e3649 ckpt 2394: wrote owner message 44 into TASKS.md in the owner's words: VC1 (video has no
  9ae7442b ckpt 2393: re-dispatched the one-hour hotspot run WITH the lab-server shaping: android.
  13ac03b9 ckpt 2392: lab server follows the hotspot too: soaklab.py LAB_RATE_KBPS / LAB_DELAY_MS 
  9f97e3b4 ckpt 2391: gates for alpha48 are green: web tests 418/418 (npm test, final scripts), JV
  a7ef675c ckpt 2390: hotspot-shaped network option for the lab (--net hotspot = 5 Mbit/s each way
  64223f1f ckpt 2389: run 5 (all, 20 min) = android.yml run 43731411440 in progress; run 6 (all, 6
  a5ac2866 ckpt 2388: lab runs 3+4 read (artifacts via tools/emu/fetch_artifact.py): FS1 WORKS on 
  b486edd4 ckpt 2387: added: Back leaves full screen first (handleBack: exitFullScreen), RX4-X2 re
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
