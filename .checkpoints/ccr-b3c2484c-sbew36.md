# CHECKPOINT 2384 — read me first, then TASKS.md

**Written:** 2026-11-15T06:14:36Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `99766b3a` (this checkpoint is the commit after it)

## Just done
lab run 1 failed at 'Create the tablet-shaped emulator': avdmanager put the AVD elsewhere than ~/.android/avd -> fixed with ANDROID_AVD_HOME set at job level + a clear error; the Gecko x86_64 debug build only takes ~1.5 min with the Gradle cache, KVM present, emulator + Android 10 image install ~1 min. Also coded: RX4-X2 DownloadGuardRules (core + 10 tests, wired in BrowserActivity.download), RX4-X1 ApkIdentity (core + 8 tests) + net/ApkInspect + openDownload refusal/facts

## Do this next
re-dispatch app=lab lab_suite=smoke lab_minutes=3 (ref ccr-b3c2484c-sbew36) and poll with actions_get get_workflow_job; read logs with get_job_logs tail_lines <= 80 (each env block costs tokens); then fix soak.py issues; then suite=all minutes=60

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5aaab1ce ckpt 2383: dispatched the first lab run (smoke, 3 min soak): android.yml run 4368415828
  e7847e2b ckpt 2382: SOAK1/RX4-E1 built: .github/workflows/emu-lab.yml (KVM Android 10 x86_64 600
  c471f4a5 ckpt 2381: FS1 code: core/media/FullscreenTurn (pure rule + 20 tests), geckoapp/web/Ful
  f81e0818 ckpt 2380: saved research/REVIEW-EXTERNAL-4.md (byte-identical copy of the upload) and 
  1f33dcab ckpt 2379: alpha47 RELEASED and recorded (run 43680608062 green, BUILDLOG line written,
  0665c59e ckpt 2378: alpha47 DISPATCHED: workflow android.yml run 43680608062 (run_number 99, ref
  99978930 ckpt 2377: gated kestrel-g-v0.3.0-alpha47 (gecko code 148) and pushed it: handoff self-
  943f5a5e ckpt 2376: YS1: independent review of seekbar.js applied (real YouTube bar markup from 
  021ec124 ckpt 2375: YS1: seekbar.js wired in (manifest, guard flag 16, sbk relay, Settings row '
  b225c11a ckpt 2374: YS1b: seekbar.js (tap/slide on a player's progress bar -> video jumps when t
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
