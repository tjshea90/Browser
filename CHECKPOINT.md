# CHECKPOINT 2393 — read me first, then TASKS.md

**Written:** 2026-11-15T16:00:21Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `13ac03b9` (this checkpoint is the commit after it)

## Just done
re-dispatched the one-hour hotspot run WITH the lab-server shaping: android.yml run 43732896160 (commit 13ac03b9, lab_suite=all lab_minutes=60 lab_net=hotspot), it starts when run 5 (43731411440) ends; the older pending run 43732156386 was cancelled by the concurrency rule; heartbeat trigger trig_01Y8CFHKVb6hmQmEEM78p6gv (enabled, cron 7 */2, bound to this session) exists: delete it when alpha48 is shipped and the soak is written up

## Do this next
1) when run 43731411440 ends (about 16:15Z container clock): actions_list list_workflow_run_artifacts -> actions_get download_workflow_run_artifact -> python3 -I tools/emu/fetch_artifact.py URL <scratchpad dir> -> read report.md (Back in full screen: key back-up / fullscreen back-exit / full screen ends; guard_downloads table; apk_guard; ANR/stall; look.readBackSlow); 2) read the reviewer's findings (agent a481a178ed92c9f22 notification) and fix real ones; 3) ship alpha48: bash ship.sh gecko with the note in scratchpad note48.txt, dispatch android.yml app=gecko full_build=true note=same, wait tag + green, bash tools/record-release.sh TAG with the IDENTICAL note, Release link on its own line; 4) read the hour run 43732896160 (about 75 min after run 5 ends), write research/SOAK_RUN1.md, TASKS rows per finding (SOAK1c/SOAK1d), tick RX4-E1/X2/FS1d with claim.sh --done; 5) delete trig_01Y8CFHKVb6hmQmEEM78p6gv

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  13ac03b9 ckpt 2392: lab server follows the hotspot too: soaklab.py LAB_RATE_KBPS / LAB_DELAY_MS 
  9f97e3b4 ckpt 2391: gates for alpha48 are green: web tests 418/418 (npm test, final scripts), JV
  a7ef675c ckpt 2390: hotspot-shaped network option for the lab (--net hotspot = 5 Mbit/s each way
  64223f1f ckpt 2389: run 5 (all, 20 min) = android.yml run 43731411440 in progress; run 6 (all, 6
  a5ac2866 ckpt 2388: lab runs 3+4 read (artifacts via tools/emu/fetch_artifact.py): FS1 WORKS on 
  b486edd4 ckpt 2387: added: Back leaves full screen first (handleBack: exitFullScreen), RX4-X2 re
  e29a9127 ckpt 2386: RX4-M0e done (opt-in 'Measure scrolling on real pages': Settings toggle defa
  4a1e86b2 ckpt 2385: lab smoke run 2 GREEN (run 43684548711: KVM ok, emulator boots in 30 s, Geck
  10247914 ckpt 2384: lab run 1 failed at 'Create the tablet-shaped emulator': avdmanager put the 
  5aaab1ce ckpt 2383: dispatched the first lab run (smoke, 3 min soak): android.yml run 4368415828
```
