# CHECKPOINT 2392 — read me first, then TASKS.md

**Written:** 2026-11-15T15:59:04Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `08a9b444` (this checkpoint is the commit after it)

## Just done
lab server follows the hotspot too: soaklab.py LAB_RATE_KBPS / LAB_DELAY_MS (tested locally: 1 MB at 581 KB/s, 80 ms per page), emu-lab.yml sets them when net=hotspot (600 KB/s, 80 ms), report header says what is shaped; HANDOFF.md alpha48 map line written; independent code review of the alpha48 diff running in the background (agent a481a178ed92c9f22, read-only)

## Do this next
re-dispatch the hour run so it carries this (android.yml app=lab lab_suite=all lab_minutes=60 lab_net=hotspot; it replaces the pending run 43732156386, run 5 43731411440 keeps running ~16:17Z); read run 5 artifacts (Back in full screen? guard table? ANR?); act on the reviewer's findings; then ship alpha48: bash ship.sh gecko NOTE (note draft in scratchpad note48.txt), dispatch android.yml app=gecko full_build=true, record-release.sh after green

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  9f97e3b4 ckpt 2391: gates for alpha48 are green: web tests 418/418 (npm test, final scripts), JV
  a7ef675c ckpt 2390: hotspot-shaped network option for the lab (--net hotspot = 5 Mbit/s each way
  64223f1f ckpt 2389: run 5 (all, 20 min) = android.yml run 43731411440 in progress; run 6 (all, 6
  a5ac2866 ckpt 2388: lab runs 3+4 read (artifacts via tools/emu/fetch_artifact.py): FS1 WORKS on 
  b486edd4 ckpt 2387: added: Back leaves full screen first (handleBack: exitFullScreen), RX4-X2 re
  e29a9127 ckpt 2386: RX4-M0e done (opt-in 'Measure scrolling on real pages': Settings toggle defa
  4a1e86b2 ckpt 2385: lab smoke run 2 GREEN (run 43684548711: KVM ok, emulator boots in 30 s, Geck
  10247914 ckpt 2384: lab run 1 failed at 'Create the tablet-shaped emulator': avdmanager put the 
  5aaab1ce ckpt 2383: dispatched the first lab run (smoke, 3 min soak): android.yml run 4368415828
  e7847e2b ckpt 2382: SOAK1/RX4-E1 built: .github/workflows/emu-lab.yml (KVM Android 10 x86_64 600
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
