# CHECKPOINT 2387 — read me first, then TASKS.md

**Written:** 2026-11-15T06:36:09Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `8298a1c3` (this checkpoint is the commit after it)

## Just done
added: Back leaves full screen first (handleBack: exitFullScreen), RX4-X2 refinement (a tap that only opened the page does not cover what the page starts: page-start compared with the touch; core 15 tests), lab scenes for X2 guard_downloads (4 pages auto/tap x link/nav) + X1 apk_guard (debug intent apkcheck, run-as copies of the installed APK as fake-update.apk / holiday.mp4 / random plain.mp4), research/SECURITY.md section 10 (round 4 status)

## Do this next
lab run 3 (43685125862, commit 4a1e86b2, without the guard/apk scenes) ends ~06:42 container clock: list_workflow_run_artifacts -> download -> read report.md; then dispatch lab_suite=all lab_minutes=60 (queues behind nothing)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e29a9127 ckpt 2386: RX4-M0e done (opt-in 'Measure scrolling on real pages': Settings toggle defa
  4a1e86b2 ckpt 2385: lab smoke run 2 GREEN (run 43684548711: KVM ok, emulator boots in 30 s, Geck
  10247914 ckpt 2384: lab run 1 failed at 'Create the tablet-shaped emulator': avdmanager put the 
  5aaab1ce ckpt 2383: dispatched the first lab run (smoke, 3 min soak): android.yml run 4368415828
  e7847e2b ckpt 2382: SOAK1/RX4-E1 built: .github/workflows/emu-lab.yml (KVM Android 10 x86_64 600
  c471f4a5 ckpt 2381: FS1 code: core/media/FullscreenTurn (pure rule + 20 tests), geckoapp/web/Ful
  f81e0818 ckpt 2380: saved research/REVIEW-EXTERNAL-4.md (byte-identical copy of the upload) and 
  1f33dcab ckpt 2379: alpha47 RELEASED and recorded (run 43680608062 green, BUILDLOG line written,
  0665c59e ckpt 2378: alpha47 DISPATCHED: workflow android.yml run 43680608062 (run_number 99, ref
  99978930 ckpt 2377: gated kestrel-g-v0.3.0-alpha47 (gecko code 148) and pushed it: handoff self-
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
