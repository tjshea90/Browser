# CHECKPOINT 2396 — read me first, then TASKS.md

**Written:** 2026-11-15T16:31:05Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `441df0e1` (this checkpoint is the commit after it)

## Just done
VC1 + LP1 first working version: vfind.js (finds videos in shadow roots / under pointer-events-less layers, covered-by-frame lookup), videobar.js (Kestrel's own bar: play/pause, seek track with drag, time, mute, full screen with the engine's own controls for a bare film), manifest, background relays (ft frame relay, vbk counters, guard vb/vba flags), content.js long-press handlers use VF, Kotlin: Prefs.videoControls + Settings row + flags 64/128 + vbk counters + Diagnostics line; web-tests/videobar.test.js 12/12 green

## Do this next
run all web tests (npm test, log in scratchpad webtests3.log), add openspace/content cases if any fail; then RV1-RV9 fixes (research/REVIEW_ALPHA48.md), lab driver (cling tap, system restart recovery, lighter scene mix, Yahoo-like lab page /video/shadow + scenes), dispatch the lab, ship alpha48 with VC1/LP1; lab hour run 43732896160 may be running on the old commit: cancel it when the driver fix is ready

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6b3d7aba ckpt 2395: independent review of the alpha48 diff read (research/REVIEW_ALPHA48.md, 9 f
  a74e3649 ckpt 2394: wrote owner message 44 into TASKS.md in the owner's words: VC1 (video has no
  9ae7442b ckpt 2393: re-dispatched the one-hour hotspot run WITH the lab-server shaping: android.
  13ac03b9 ckpt 2392: lab server follows the hotspot too: soaklab.py LAB_RATE_KBPS / LAB_DELAY_MS 
  9f97e3b4 ckpt 2391: gates for alpha48 are green: web tests 418/418 (npm test, final scripts), JV
  a7ef675c ckpt 2390: hotspot-shaped network option for the lab (--net hotspot = 5 Mbit/s each way
  64223f1f ckpt 2389: run 5 (all, 20 min) = android.yml run 43731411440 in progress; run 6 (all, 6
  a5ac2866 ckpt 2388: lab runs 3+4 read (artifacts via tools/emu/fetch_artifact.py): FS1 WORKS on 
  b486edd4 ckpt 2387: added: Back leaves full screen first (handleBack: exitFullScreen), RX4-X2 re
  e29a9127 ckpt 2386: RX4-M0e done (opt-in 'Measure scrolling on real pages': Settings toggle defa
```

(16 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
