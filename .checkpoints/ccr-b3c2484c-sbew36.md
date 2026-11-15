# CHECKPOINT 2389 — read me first, then TASKS.md

**Written:** 2026-11-15T15:45:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `8c17fde8` (this checkpoint is the commit after it)

## Just done
run 5 (all, 20 min) = android.yml run 43731411440 in progress; run 6 (all, 60 min = the one-hour soak) QUEUED behind it (same concurrency group), dispatched at ~15:47Z on commit 8c17fde8 which also has: full-bleed full screen (insets padding dropped), Android immersive box pre-confirmed in the lab, dump reads engine prefs, README of lab findings in research/EMU_LAB.md

## Do this next
poll run 43731411440 artifacts (list_workflow_run_artifacts), read with tools/emu/fetch_artifact.py (download_workflow_run_artifact -> url); then the one-hour run (id: list_workflow_runs status in_progress/queued) ends ~17:30Z -> write research/SOAK_RUN1.md; meanwhile: tick TASKS rows (claim.sh --done), bump geckoapp versionCode 149 / 0.3.0-alpha48, run the full JVM+web suites, ship.sh gecko when run 5 shows no new bug

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a5ac2866 ckpt 2388: lab runs 3+4 read (artifacts via tools/emu/fetch_artifact.py): FS1 WORKS on 
  b486edd4 ckpt 2387: added: Back leaves full screen first (handleBack: exitFullScreen), RX4-X2 re
  e29a9127 ckpt 2386: RX4-M0e done (opt-in 'Measure scrolling on real pages': Settings toggle defa
  4a1e86b2 ckpt 2385: lab smoke run 2 GREEN (run 43684548711: KVM ok, emulator boots in 30 s, Geck
  10247914 ckpt 2384: lab run 1 failed at 'Create the tablet-shaped emulator': avdmanager put the 
  5aaab1ce ckpt 2383: dispatched the first lab run (smoke, 3 min soak): android.yml run 4368415828
  e7847e2b ckpt 2382: SOAK1/RX4-E1 built: .github/workflows/emu-lab.yml (KVM Android 10 x86_64 600
  c471f4a5 ckpt 2381: FS1 code: core/media/FullscreenTurn (pure rule + 20 tests), geckoapp/web/Ful
  f81e0818 ckpt 2380: saved research/REVIEW-EXTERNAL-4.md (byte-identical copy of the upload) and 
  1f33dcab ckpt 2379: alpha47 RELEASED and recorded (run 43680608062 green, BUILDLOG line written,
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
