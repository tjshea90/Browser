# CHECKPOINT 2405 — read me first, then TASKS.md

**Written:** 2026-11-15T17:19:13Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `281c48f4` (this checkpoint is the commit after it)

## Just done
lab vc run 3 (43739450975, commit ecbdecc3) all green on the REAL engine incl. the live Yahoo /videos/ player: tap = own (the page had its own controls), long press = the Save video sheet and no floating panel (picture 11-yahoo-videos-after-press: 'This is a live stream. Live streams cannot be saved'); courier CourierServer.stop() race fixed (flaky ServerTest NoSuchElementException); ship.sh gecko gates green and pushed; DISPATCHED: release run 43740388928 (app=gecko full_build=true, commit 281c48f4, tag kestrel-g-v0.3.0-alpha48) and the hour soak run 43740398387 (all, 60 min, hotspot 5 Mbit/s without the emulator delay)

## Do this next
1) wait for release run 43740388928: actions_list list_workflow_jobs all success + git ls-remote --tags origin shows kestrel-g-v0.3.0-alpha48 + mcp__github__get_release_by_tag shows the APK; THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha48 "$(cat scratchpad note48.one)" (the note text is in the scratchpad file note48.one, identical to the dispatched input), add the Release link on its own line in the owner answer; 2) hour run 43740398387 (ends about 18:35 container clock): download artifacts, write research/SOAK_RUN1.md, TASKS rows per finding (SOAK1c/SOAK1d), tick RX4-E1 / RX4-X2 / FS1d / VC1 / LP1 with claim.sh --done + evidence; 3) answer the owner in plain words (what was NOT run on the tablet), re-list open items (RX4-V2 question; owner-only GitHub steps); 4) delete heartbeat trig_01Y8CFHKVb6hmQmEEM78p6gv

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2211e3fd ckpt 2404: gated kestrel-g-v0.3.0-alpha48 (gecko code 149) and pushed it: handoff self-
  b619f02e ckpt 2403: pre-ship: alpha48: videos with no controls now get Kestrel's own: tap a vide
  ecbdecc3 ckpt 2402: lab vc run 2 (43738479064, commit b5fd15c4) on the REAL Firefox engine: VC1 
  b5fd15c4 ckpt 2401: lab vc run 1 (43737466605) read: LP1 long press menu on all four lab pages a
  0acf028c ckpt 2400: lab driver: hotspot shaping without the emulator delay option (it made hacke
  4e233ebe ckpt 2399: VC1/LP1/RV1-RV9 verified: Kotlin suite (core + app + geckoapp tests + geckoa
  31e0d580 ckpt 2398: RV3 (updater spoof: fromUpdater needs the app-private download kind), RV4 (r
  a61cff67 ckpt 2397: RV1 (site orientation lock = Android constants, verified in the GeckoView 16
  fa81920c ckpt 2396: VC1 + LP1 first working version: vfind.js (finds videos in shadow roots / un
  6b3d7aba ckpt 2395: independent review of the alpha48 diff read (research/REVIEW_ALPHA48.md, 9 f
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
