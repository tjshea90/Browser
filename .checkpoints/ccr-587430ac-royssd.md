# CHECKPOINT 2664 — read me first, then TASKS.md

**Written:** 2026-11-18T04:18:22Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `a156cd8b` (this checkpoint is the commit after it)

## Just done
Lab run 44233990309 (suite new): dm scenes all OK; FOUND+FIXED: background.js rebuilt the vst report from a fixed field list and DROPPED the page player quality list (q/qc/qf) -> QL1 showed no list; now passed through, cut to limits (web test added, 615/615). vs scene msel0 'started' = lab ran with press-to-play rule OFF (Diagnostics said OFF): scene now sets vpress 1 for the mse variants, 0 for the finder variants, and reads states per variant.

## Do this next
dispatch ONE lab lab_suite=new (app=lab lab_profile=standard lab_net=full lab_minutes=5) on ccr-587430ac-royssd; expect ql checks ok and vs: mse+msel playing, msel0 held (with the rule ON); read report.md; if green: bash ship.sh gecko with docs/handoff/ALPHA69_RELEASE_NOTE.txt, ONE dispatch app=gecko full_build=true, verify tag kestrel-g-v0.3.0-alpha69, record-release, tick TASKS

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  1dc61021 ckpt 2663: research/VS1_VP2_NM1_FD1.md written; ALPHA69_RELEASE_NOTE.txt rewritten (FD1
  c8a727cc ckpt 2662: FD1 floating download window done: 8 DownloadPopupFloatTest + OwnerRulesTest
  bb4a5af6 ckpt 2661: Owner message 5 of this session written into TASKS.md BEFORE any code: FD1 (
  78aff752 ckpt 2660: VS1d: vidfast 'stuck on loading' recorded in TASKS (owner words). Lab: vidla
  5375bf9c ckpt 2659: alpha69 CORRECTION: run 44224672021 did NOT build: test-jvm FAILED on GitHub
  de4ea102 ckpt 2658: QL1 + VS1 + DM2 coded and self-tested. QL1: page side (quality-main.js, qual
  96a653ee ckpt 2657: Owner message 3 of this session written into TASKS.md BEFORE any code: VS1 (
  1587c92f ckpt 2656: QL1 page side DONE: quality-main.js (MAIN: notes hls.js / dash.js / Shaka pl
  b73892a9 ckpt 2655: QL1 written into TASKS.md in the owner's words (quality choice in full scree
  8033e054 ckpt 2654: alpha69 DISPATCHED ONCE: android.yml run 44224672021 (run_number 225, app=ge
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
