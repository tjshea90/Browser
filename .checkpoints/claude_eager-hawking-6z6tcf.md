# CHECKPOINT 3448 — read me first, then TASKS.md

**Written:** 2026-11-27T06:09:22Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/eager-hawking-6z6tcf` · **builds on:** `0e98a59d` (this checkpoint is the commit after it)

## Just done
ND1d/ND1e CODED (not shipped yet): core ProfileRule (+5 tests), geckoapp PageProfiler (ProfilerController start/reload/stop/upload), LiveDiag.sendProfile (bytes, streamed base64), Settings buttons in the live diagnostics section, TabDelegates hooks, diag/ProcMem (per-process PSS), CpuMeter sys load, digest lines (memory by process, run queue), tools/profile_digest.py + tools/test_profile_digest.py + test_handoff 98 green, OwnerRulesTest nd1d/nd1e, CLAUDE.md + research/DIAG_2026-11-27.md section 4; :geckoapp:compileDebugKotlin green; alpha110 CI run 45949557236 still running

## Do this next
1) when run 45949557236 is green: get_release_by_tag, record-release alpha110 (note in the ckpt 3439 commit message), tick ND1c, answer the owner. 2) full :app:testDebugUnitTest result of the background run (scratchpad t_app2.log: EXIT line) must be green before alpha111 3) alpha111: bump geckoapp versionCode 211->212 / 0.3.0-alpha111, write the note (profile recorder + per-process memory: say plainly NOT run on the tablet), bash ship.sh gecko, dispatch, record

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/DIAG_2026-11-27.md

## Last ten checkpoints
```
  8433cdc4 ckpt 3440: alpha110 DISPATCHED ONCE: run 45949557236 (run number 311, head 91326acd, re
  91326acd ckpt 3439: gated kestrel-g-v0.3.0-alpha110 (gecko code 211) and pushed it: handoff self
  8d2e39d0 ckpt 3432: KR1 keep-the-page-ready DONE in code: core KeepReady (4 tests) + geckoapp Ke
  2bdf5109 ckpt 3420: ND1: Settings lazy sections (SettingsView.section(title){rows}, Chevron, OPE
  2f0ab8fc ckpt 3407: ND1 (owner: 'Use the newest diagnostics and improve the browser as much as p
  7c753c88 ckpt 3381: VPS2a-c DONE: research/VPS_PLAN.md written (P40 facts + ToS limits, evidence
  af9b1f64 ckpt 3378: VPS2 (owner: NoBull P.40, in depth research for every repo + Claude + LinkMe
  3affc266 ckpt 3377: VPS1 written into TASKS in the owner's words + research/VPS_IDEAS.md (ranked
  100a331b ckpt 3376: alpha109 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha109, run 457282
  93113be6 ckpt 3373: alpha109 build DISPATCHED ONCE: run 45728212306 (run number 310), head 40149
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
