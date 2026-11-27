# CHECKPOINT 3440 — read me first, then TASKS.md

**Written:** 2026-11-27T06:05:26Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/eager-hawking-6z6tcf` · **builds on:** `91326acd` (this checkpoint is the commit after it)

## Just done
alpha110 DISPATCHED ONCE: run 45949557236 (run number 311, head 91326acd, ref claude/eager-hawking-6z6tcf, app=gecko full_build=true). Gates before it: handoff self-test, :core:test, :app:testDebugUnitTest (515), :geckoapp:compileDebugKotlin, :app:lintRelease 0 errors. A background loop writes the tag appearance to the scratchpad tagwait110.log (cap 30 min)

## Do this next
WAIT for run 45949557236 (mcp__github__actions_list list_workflow_jobs resource_id 45949557236; never dispatch alpha110 again). When all jobs are green: mcp__github__get_release_by_tag tjshea90 Browser kestrel-g-v0.3.0-alpha110 shows the APK, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha110 "$(cat <scratchpad>/note110.txt)" with the IDENTICAL note (the text is in the run's head_commit message / ship.sh output; also copy at /tmp/claude-0/-home-user/5d71be5f-e0a0-5db4-9e01-d1e6d144170f/scratchpad/note110.txt if the container lives), tick ND1c in TASKS.md, then answer the owner with the Release link on its own line. MEANWHILE build ND1d for alpha111 (drafts in the scratchpad nd1d/: ProfileRule.kt+test, PageProfiler.kt, patch_livediag.py; profile_digest.py + synth_profile.py in the scratchpad): copy into core/app/geckoapp, Settings buttons in liveDiagSection, SettingsListener.recordProfile, TabDelegates pageStarted/pageStopped hooks, tools/profile_digest.py + test_handoff check, CLAUDE.md note; plus ProcMem (per-process PSS via ActivityManager.getProcessMemoryInfo: the page process is invisible to /proc on Android 10) and /proc/loadavg in CpuMeter busy lines

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  91326acd ckpt 3439: gated kestrel-g-v0.3.0-alpha110 (gecko code 211) and pushed it: handoff self
  8d2e39d0 ckpt 3432: KR1 keep-the-page-ready DONE in code: core KeepReady (4 tests) + geckoapp Ke
  2bdf5109 ckpt 3420: ND1: Settings lazy sections (SettingsView.section(title){rows}, Chevron, OPE
  2f0ab8fc ckpt 3407: ND1 (owner: 'Use the newest diagnostics and improve the browser as much as p
  7c753c88 ckpt 3381: VPS2a-c DONE: research/VPS_PLAN.md written (P40 facts + ToS limits, evidence
  af9b1f64 ckpt 3378: VPS2 (owner: NoBull P.40, in depth research for every repo + Claude + LinkMe
  3affc266 ckpt 3377: VPS1 written into TASKS in the owner's words + research/VPS_IDEAS.md (ranked
  100a331b ckpt 3376: alpha109 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha109, run 457282
  93113be6 ckpt 3373: alpha109 build DISPATCHED ONCE: run 45728212306 (run number 310), head 40149
  40149389 ckpt 3372: alpha109 prepared: geckoapp versionCode 210 / 0.3.0-alpha109, extension 1.0.
```
