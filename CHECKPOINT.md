# CHECKPOINT 2399 — read me first, then TASKS.md

**Written:** 2026-11-15T16:47:32Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `ad78dddc` (this checkpoint is the commit after it)

## Just done
VC1/LP1/RV1-RV9 verified: Kotlin suite (core + app + geckoapp tests + geckoapp compile) EXIT 0, web tests 430/430 + videobar 13/13 (relay bug found by the tests: a removed var), guard tests for RV2 and VC1/LP1, HANDOFF map part 2, lab vc suite + cling tap + system restart recovery

## Do this next
cancel hour run 43732896160 (old commit), dispatch android.yml app=lab lab_suite=vc lab_minutes=5 lab_net=full on this commit; read artifacts (report.md section Video controls, screenshots vbar-*.png and yahoo-*.png via tools/emu/fetch_artifact.py); fix what the real engine shows; then dispatch the hour run (lab_suite=all lab_minutes=60 lab_net=hotspot) and ship alpha48 in parallel

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  31e0d580 ckpt 2398: RV3 (updater spoof: fromUpdater needs the app-private download kind), RV4 (r
  a61cff67 ckpt 2397: RV1 (site orientation lock = Android constants, verified in the GeckoView 16
  fa81920c ckpt 2396: VC1 + LP1 first working version: vfind.js (finds videos in shadow roots / un
  6b3d7aba ckpt 2395: independent review of the alpha48 diff read (research/REVIEW_ALPHA48.md, 9 f
  a74e3649 ckpt 2394: wrote owner message 44 into TASKS.md in the owner's words: VC1 (video has no
  9ae7442b ckpt 2393: re-dispatched the one-hour hotspot run WITH the lab-server shaping: android.
  13ac03b9 ckpt 2392: lab server follows the hotspot too: soaklab.py LAB_RATE_KBPS / LAB_DELAY_MS 
  9f97e3b4 ckpt 2391: gates for alpha48 are green: web tests 418/418 (npm test, final scripts), JV
  a7ef675c ckpt 2390: hotspot-shaped network option for the lab (--net hotspot = 5 Mbit/s each way
  64223f1f ckpt 2389: run 5 (all, 20 min) = android.yml run 43731411440 in progress; run 6 (all, 6
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
