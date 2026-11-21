# CHECKPOINT 2800 — read me first, then TASKS.md

**Written:** 2026-11-21T07:34:51Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `946369a8` (this checkpoint is the commit after it)

## Just done
LD2 written into TASKS.md in the owner's words (LD2a-e): smarter + more complete data, settings Advisor, OFF = fully asleep, digest, alpha83. alpha82 (two scrub fixes, versionCode 183) is gated by ship.sh (log scratchpad ship82.log) but will NOT be dispatched: folded into alpha83 to save Actions minutes.

## Do this next
While ship.sh alpha82 finishes its unit suites (do NOT edit Kotlin until its log shows EXIT: geckoapp compile reads sources): read app Prefs.kt + Health counters to choose the settings snapshot + Advisor rules (docs only). Then LD2a/b/c code in core/livediag (DiagBatch v2, Advisor.kt) + app LiveDiag; ship alpha83 (versionCode 184), do NOT dispatch alpha82

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  946369a8 ckpt 2799: alpha82 prepared: versionCode 183 / 0.3.0-alpha82, extension version synced,
  2d4c1e81 ckpt 2798: alpha81 SHIPPED and RECORDED: run 44878029773 green (check, test-js, test-jv
  dba49a28 ckpt 2797: LD1 privacy fix 2 (for alpha82): DiagScrub removes dotted-quad network addre
  db02a2b5 ckpt 2796: LD1 privacy fix (for alpha82): DiagScrub now drops the path after a BARE hos
  472a364c ckpt 2795: alpha81 DISPATCHED once: android.yml run 44878029773 (run_number 263, head 7
  7f084cff ckpt 2794: gated kestrel-g-v0.3.0-alpha81 (gecko code 182) and pushed it: handoff self-
  68f9d24e ckpt 2793: LD1e: full gate green (:core:test, :app:testDebugUnitTest, :geckoapp:compile
  a6c87bc8 ckpt 2792: LD1d done: tools/diag_digest.py (crashes/kills, slow sites by host, playback
  2a79bf47 ckpt 2791: LD1c tested: LiveDiagTest 13/13 (switch off = nothing kept, switch survives 
  2050ef12 ckpt 2790: LD1c: app side written + geckoapp compiles: diag/LiveDiag.kt (master switch,
```
