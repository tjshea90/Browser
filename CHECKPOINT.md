# CHECKPOINT 2668 — read me first, then TASKS.md

**Written:** 2026-11-18T05:04:29Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `78d706af` (this checkpoint is the commit after it)

## Just done
alpha69 RELEASED: tag kestrel-g-v0.3.0-alpha69, run 44239780214 all jobs green (jvm, js, build-gecko incl. certificate check + Publish), APK 96 MB on the Release, recorded in BUILDLOG.md (44c320d2). TASKS ticked (DM1/DM2/QL1/VS1/VS1d/FD1/NM1/VP2 + answer boxes), HANDOFF lessons added.

## Do this next
NOTHING running. Wait for the owner: install alpha69 from https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha69 and test on the tablet (YouTube quality button in full screen, doraby servers vidsrc/vidfast, naming Ozark S01E02, the floating download window, no popup at start); on a crash: Diagnostics line Last run. CR1d stays open until a crash is captured with Last run. No heartbeat trigger exists for this job.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M HANDOFF.md

## Last ten checkpoints
```
  cf070608 ckpt 2667: alpha69 DISPATCHED ONCE at 04:47Z: android.yml run 44239780214 (run_number 2
  3f51b70a ckpt 2666: gated kestrel-g-v0.3.0-alpha69 (gecko code 170) and pushed it: handoff self-
  de1a9d82 ckpt 2665: Lab run 44236411726 suite new ALL GREEN (13 scenes, 0 issues): dm 9 checks, 
  1c980267 ckpt 2664: Lab run 44233990309 (suite new): dm scenes all OK; FOUND+FIXED: background.j
  1dc61021 ckpt 2663: research/VS1_VP2_NM1_FD1.md written; ALPHA69_RELEASE_NOTE.txt rewritten (FD1
  c8a727cc ckpt 2662: FD1 floating download window done: 8 DownloadPopupFloatTest + OwnerRulesTest
  bb4a5af6 ckpt 2661: Owner message 5 of this session written into TASKS.md BEFORE any code: FD1 (
  78aff752 ckpt 2660: VS1d: vidfast 'stuck on loading' recorded in TASKS (owner words). Lab: vidla
  5375bf9c ckpt 2659: alpha69 CORRECTION: run 44224672021 did NOT build: test-jvm FAILED on GitHub
  de4ea102 ckpt 2658: QL1 + VS1 + DM2 coded and self-tested. QL1: page side (quality-main.js, qual
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
