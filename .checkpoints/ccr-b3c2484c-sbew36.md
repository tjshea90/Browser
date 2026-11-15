# CHECKPOINT 2401 — read me first, then TASKS.md

**Written:** 2026-11-15T16:58:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `ba56d61c` (this checkpoint is the commit after it)

## Just done
lab vc run 1 (43737466605) read: LP1 long press menu on all four lab pages and no panel on the real engine; VC1 not judged because my lab page route was wrong (the generic /video/ route swallowed /video/vc: fixed); the live Yahoo Finance page in Kestrel (Gecko, lab picture 17-yahoo-04): the player shows its OWN controls while a content video plays and the dock download button lights up (finder found it): the owner's no-controls case is the ad phase; yahoo scene now taps and long presses the player (4 swipes down, y 320/290); hotspot cancelled run 109 showed the emulator delay option wrecks page loads (dropped)

## Do this next
dispatch lab vc again (android.yml app=lab lab_suite=vc), read it; then hour run; ship alpha48

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  0acf028c ckpt 2400: lab driver: hotspot shaping without the emulator delay option (it made hacke
  4e233ebe ckpt 2399: VC1/LP1/RV1-RV9 verified: Kotlin suite (core + app + geckoapp tests + geckoa
  31e0d580 ckpt 2398: RV3 (updater spoof: fromUpdater needs the app-private download kind), RV4 (r
  a61cff67 ckpt 2397: RV1 (site orientation lock = Android constants, verified in the GeckoView 16
  fa81920c ckpt 2396: VC1 + LP1 first working version: vfind.js (finds videos in shadow roots / un
  6b3d7aba ckpt 2395: independent review of the alpha48 diff read (research/REVIEW_ALPHA48.md, 9 f
  a74e3649 ckpt 2394: wrote owner message 44 into TASKS.md in the owner's words: VC1 (video has no
  9ae7442b ckpt 2393: re-dispatched the one-hour hotspot run WITH the lab-server shaping: android.
  13ac03b9 ckpt 2392: lab server follows the hotspot too: soaklab.py LAB_RATE_KBPS / LAB_DELAY_MS 
  9f97e3b4 ckpt 2391: gates for alpha48 are green: web tests 418/418 (npm test, final scripts), JV
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
