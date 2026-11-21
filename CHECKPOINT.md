# CHECKPOINT 2795 — read me first, then TASKS.md

**Written:** 2026-11-21T06:59:09Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `7f084cff` (this checkpoint is the commit after it)

## Just done
alpha81 DISPATCHED once: android.yml run 44878029773 (run_number 263, head 7f084cff, app=gecko full_build=true) in progress at 2026-11-21T06:58Z. Do NOT dispatch again.

## Do this next
WAIT for run 44878029773 (list_workflow_jobs resource_id 44878029773; get_job_logs answers 404 until a job ends; a full build takes ~15-25 min). When green: git ls-remote origin refs/tags/kestrel-g-v0.3.0-alpha81, mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha81 shows the APK, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha81 with the IDENTICAL note from docs/handoff/ALPHA81_RELEASE_NOTE.txt; then answer the owner (alpha80 Back + LD1 answer + token how-to + NOT run on tablet + open items). If a job fails only on PhoneBackendTest (FLK1 timing flake): rerun_failed_jobs on the SAME run, no second dispatch

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  7f084cff ckpt 2794: gated kestrel-g-v0.3.0-alpha81 (gecko code 182) and pushed it: handoff self-
  68f9d24e ckpt 2793: LD1e: full gate green (:core:test, :app:testDebugUnitTest, :geckoapp:compile
  a6c87bc8 ckpt 2792: LD1d done: tools/diag_digest.py (crashes/kills, slow sites by host, playback
  2a79bf47 ckpt 2791: LD1c tested: LiveDiagTest 13/13 (switch off = nothing kept, switch survives 
  2050ef12 ckpt 2790: LD1c: app side written + geckoapp compiles: diag/LiveDiag.kt (master switch,
  5d077b35 ckpt 2789: LD1b: core livediag package written + tested (DiagScrub: links -> host, sens
  808aa4f6 ckpt 2787: alpha80 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha80, gecko code 181, run
  f12802a2 ckpt 2786: LD1 written into TASKS.md in the owner's words (live diagnostics engine with
  5c4edd25 ckpt 2785: REL80b: alpha80 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  0c2b1e08 ckpt 2784: gated kestrel-g-v0.3.0-alpha80 (gecko code 181) and pushed it: handoff self-
```
