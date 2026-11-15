# CHECKPOINT 2391 — read me first, then TASKS.md

**Written:** 2026-11-15T15:54:00Z · **tests:** 1 RED: test_handoff.sh (0 green)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `9e880b85` (this checkpoint is the commit after it)

## Just done
gates for alpha48 are green: web tests 418/418 (npm test, final scripts), JVM suite exit 0 (core + app + geckoapp unit tests, 15:48Z); checked android.yml concurrency: lab runs share group apk-<ref>-lab, a gecko release uses apk-<ref>-gecko + release-gecko, so shipping alpha48 does NOT cancel the queued hour run 43732156386 (but a second LAB dispatch would replace it: do not dispatch lab again unless that is wanted)

## Do this next
wait for lab run 5 (43731411440, in progress since 15:42Z, ends about 16:17Z container clock): actions_list list_workflow_run_artifacts -> actions_get download_workflow_run_artifact (text artifact) -> python3 -I tools/emu/fetch_artifact.py URL scratchpad-dir -> read report.md: Back in full screen (key back-up, fullscreen back-exit, full screen ends), guard_downloads table, apk_guard, ANR/stall, look.readBackSlow; then ship alpha48 (versionCode 149 already bumped): bash ship.sh gecko NOTE, dispatch android.yml app=gecko full_build=true, wait for tag + green jobs, bash tools/record-release.sh TAG with the IDENTICAL note, Release link on its own line; hour run 43732156386 (hotspot, 60 min) is queued behind run 5: read it, write research/SOAK_RUN1.md

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a7ef675c ckpt 2390: hotspot-shaped network option for the lab (--net hotspot = 5 Mbit/s each way
  64223f1f ckpt 2389: run 5 (all, 20 min) = android.yml run 43731411440 in progress; run 6 (all, 6
  a5ac2866 ckpt 2388: lab runs 3+4 read (artifacts via tools/emu/fetch_artifact.py): FS1 WORKS on 
  b486edd4 ckpt 2387: added: Back leaves full screen first (handleBack: exitFullScreen), RX4-X2 re
  e29a9127 ckpt 2386: RX4-M0e done (opt-in 'Measure scrolling on real pages': Settings toggle defa
  4a1e86b2 ckpt 2385: lab smoke run 2 GREEN (run 43684548711: KVM ok, emulator boots in 30 s, Geck
  10247914 ckpt 2384: lab run 1 failed at 'Create the tablet-shaped emulator': avdmanager put the 
  5aaab1ce ckpt 2383: dispatched the first lab run (smoke, 3 min soak): android.yml run 4368415828
  e7847e2b ckpt 2382: SOAK1/RX4-E1 built: .github/workflows/emu-lab.yml (KVM Android 10 x86_64 600
  c471f4a5 ckpt 2381: FS1 code: core/media/FullscreenTurn (pure rule + 20 tests), geckoapp/web/Ful
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
