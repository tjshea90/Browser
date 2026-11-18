# CHECKPOINT 2670 — read me first, then TASKS.md

**Written:** 2026-11-18T05:25:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `748029c6` (this checkpoint is the commit after it)

## Just done
Owner answered CR1f-c: the app vanished to the home screen = process death, not a freeze. VS2 capture coded (core MediaList captured/orphan/onNewPage + PageIdentity, VideoFlow.clearTab(url), lab variant keep); full JVM suite 7 running.

## Do this next
1) jvm-suite7.log finished rc; 2) CR1g: LastRun memory snapshot (VmRSS of app processes, MemFree, swap, PSI) + 15 s tick from the Health watchdog; 3) lab one at a time: lr (freeze + kill) then new/vs (keep variant); 4) ship alpha70

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  1ac8a776 ckpt 2669: Owner message VS2 written into TASKS (doraby: vidsrc + 111movies find no dow
  676dd260 ckpt 2668: alpha69 RELEASED: tag kestrel-g-v0.3.0-alpha69, run 44239780214 all jobs gre
  cf070608 ckpt 2667: alpha69 DISPATCHED ONCE at 04:47Z: android.yml run 44239780214 (run_number 2
  3f51b70a ckpt 2666: gated kestrel-g-v0.3.0-alpha69 (gecko code 170) and pushed it: handoff self-
  de1a9d82 ckpt 2665: Lab run 44236411726 suite new ALL GREEN (13 scenes, 0 issues): dm 9 checks, 
  1c980267 ckpt 2664: Lab run 44233990309 (suite new): dm scenes all OK; FOUND+FIXED: background.j
  1dc61021 ckpt 2663: research/VS1_VP2_NM1_FD1.md written; ALPHA69_RELEASE_NOTE.txt rewritten (FD1
  c8a727cc ckpt 2662: FD1 floating download window done: 8 DownloadPopupFloatTest + OwnerRulesTest
  bb4a5af6 ckpt 2661: Owner message 5 of this session written into TASKS.md BEFORE any code: FD1 (
  78aff752 ckpt 2660: VS1d: vidfast 'stuck on loading' recorded in TASKS (owner words). Lab: vidla
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
