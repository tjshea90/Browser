# CHECKPOINT 2388 — read me first, then TASKS.md

**Written:** 2026-11-15T15:42:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `68bec1cb` (this checkpoint is the commit after it)

## Just done
lab runs 3+4 read (artifacts via tools/emu/fetch_artifact.py): FS1 WORKS on the real engine (turn sideways on a wide video, follows the sensor with Android's rotation lock ON, one-time turn let go, rotate button); RX4-X1 APK guard PROVEN (own copy + disguised mp4 refused, plain mp4 ignored); RX4-X3 Safe Browsing PROVEN (all 7 Mozilla/Google test pages blocked); X2 asks 'by itself' (auto-asked seen) but the lab could not read the dialog; FOUND: Back did not leave full screen (fixed? not proven: key logging added), capturePixels() blocks the UI thread (2 s stall, ANR after 20 s) -> read-back back-off added, emulator system_server watchdog death under 4 guest cores -> 2 cores + recovery, driver number bug

## Do this next
dispatch lab_suite=all lab_minutes=20 (run 5) on this commit; then read it with tools/emu/fetch_artifact.py; if green dispatch lab_minutes=60; then ship alpha48 (bump geckoapp versionCode 149 / alpha48, bash ship.sh gecko ...)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M tools/emu/soak.py

## Last ten checkpoints
```
  b486edd4 ckpt 2387: added: Back leaves full screen first (handleBack: exitFullScreen), RX4-X2 re
  e29a9127 ckpt 2386: RX4-M0e done (opt-in 'Measure scrolling on real pages': Settings toggle defa
  4a1e86b2 ckpt 2385: lab smoke run 2 GREEN (run 43684548711: KVM ok, emulator boots in 30 s, Geck
  10247914 ckpt 2384: lab run 1 failed at 'Create the tablet-shaped emulator': avdmanager put the 
  5aaab1ce ckpt 2383: dispatched the first lab run (smoke, 3 min soak): android.yml run 4368415828
  e7847e2b ckpt 2382: SOAK1/RX4-E1 built: .github/workflows/emu-lab.yml (KVM Android 10 x86_64 600
  c471f4a5 ckpt 2381: FS1 code: core/media/FullscreenTurn (pure rule + 20 tests), geckoapp/web/Ful
  f81e0818 ckpt 2380: saved research/REVIEW-EXTERNAL-4.md (byte-identical copy of the upload) and 
  1f33dcab ckpt 2379: alpha47 RELEASED and recorded (run 43680608062 green, BUILDLOG line written,
  0665c59e ckpt 2378: alpha47 DISPATCHED: workflow android.yml run 43680608062 (run_number 99, ref
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
