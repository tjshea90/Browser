# CHECKPOINT 2398 — read me first, then TASKS.md

**Written:** 2026-11-15T16:44:21Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `ff16fe86` (this checkpoint is the commit after it)

## Just done
RV3 (updater spoof: fromUpdater needs the app-private download kind), RV4 (redirected links are the page's own), RV5 (pendingNav 15 s + Stop), RV6 (open check on Io.fast, one at a time), RV7 (one self-started question per tab, decline per site), RV9 (no counters for private tabs) coded; lab driver: cling tap, system_server pid check + hard emulator restart (--emu-cmd/--apk from emu-lab.yml), lighter site mix (no nytimes/espn/twitch/youtube-home), Yahoo Finance + lite YouTube pages, new vc suite (vbar_round on lab pages /video/vc?kind=naked|shadow|own, yahoo_scan 14 pictures), Health lines panel open-space / video long-press-menu / sheet-shown; guard test updated for RV2; web tests 430/430; Kotlin suite re-running (one flaky courier ServerTest: CourierServer.stop toList race)

## Do this next
when scratchpad jvm_rv3.log says EXIT: cancel hour run 43732896160 (old commit) and dispatch android.yml app=lab lab_suite=vc (about 8 min) to see VC1/LP1 on the real engine, read it (screenshots vbar-*, yahoo-*), then fix; then the hour run on the final commit; ship alpha48 with VC1 + LP1 + RV fixes; owner answer; delete trigger trig_01Y8CFHKVb6hmQmEEM78p6gv after the ship

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a61cff67 ckpt 2397: RV1 (site orientation lock = Android constants, verified in the GeckoView 16
  fa81920c ckpt 2396: VC1 + LP1 first working version: vfind.js (finds videos in shadow roots / un
  6b3d7aba ckpt 2395: independent review of the alpha48 diff read (research/REVIEW_ALPHA48.md, 9 f
  a74e3649 ckpt 2394: wrote owner message 44 into TASKS.md in the owner's words: VC1 (video has no
  9ae7442b ckpt 2393: re-dispatched the one-hour hotspot run WITH the lab-server shaping: android.
  13ac03b9 ckpt 2392: lab server follows the hotspot too: soaklab.py LAB_RATE_KBPS / LAB_DELAY_MS 
  9f97e3b4 ckpt 2391: gates for alpha48 are green: web tests 418/418 (npm test, final scripts), JV
  a7ef675c ckpt 2390: hotspot-shaped network option for the lab (--net hotspot = 5 Mbit/s each way
  64223f1f ckpt 2389: run 5 (all, 20 min) = android.yml run 43731411440 in progress; run 6 (all, 6
  a5ac2866 ckpt 2388: lab runs 3+4 read (artifacts via tools/emu/fetch_artifact.py): FS1 WORKS on 
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
