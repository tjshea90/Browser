# CHECKPOINT 3432 — read me first, then TASKS.md

**Written:** 2026-11-27T05:48:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/eager-hawking-6z6tcf` · **builds on:** `ef9ed0a1` (this checkpoint is the commit after it)

## Just done
KR1 keep-the-page-ready DONE in code: core KeepReady (4 tests) + geckoapp KeepReadyService (specialUse FGS, IMPORTANCE_MIN, Let go, stopWithTask default) + manifest + Prefs.keepReady (default 1 hour) + BackupManager spec + Flavor.hasKeepReady + ProcessLife.keepReady guard + BrowserActivity onStart stop / onStop keepReadyOnLeave + Diagnostics line + TabDelegates kill-away/kill-front events + Settings row in Appearance; Advisor kills-away rule (+test); digest: advice of newest build only, SELF-CHECKS (caught the WRONG preview), KILLS section (10 of 11 kills were while away); OwnerRulesTest nd1 rules; :geckoapp:compileDebugKotlin green; Settings tests 8/8

## Do this next
1) add Robolectric check of the Settings keep-ready row (Flavor.hasKeepReady=true) 2) run whole suite in background: ./gradlew --offline -q :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest and :app:lintDebug, cd web-tests && npm test, bash tools/test_handoff.sh 3) update TASKS ND1a/b boxes with evidence 4) bump geckoapp versionCode 210->211 / 0.3.0-alpha110 (+ extension version sync per HANDOFF section 2), bash ship.sh gecko note, dispatch android.yml, wait, record-release 5) decide ND1d profiler (research/DIAG_2026-11-27.md section 4)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2bdf5109 ckpt 3420: ND1: Settings lazy sections (SettingsView.section(title){rows}, Chevron, OPE
  2f0ab8fc ckpt 3407: ND1 (owner: 'Use the newest diagnostics and improve the browser as much as p
  7c753c88 ckpt 3381: VPS2a-c DONE: research/VPS_PLAN.md written (P40 facts + ToS limits, evidence
  af9b1f64 ckpt 3378: VPS2 (owner: NoBull P.40, in depth research for every repo + Claude + LinkMe
  3affc266 ckpt 3377: VPS1 written into TASKS in the owner's words + research/VPS_IDEAS.md (ranked
  100a331b ckpt 3376: alpha109 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha109, run 457282
  93113be6 ckpt 3373: alpha109 build DISPATCHED ONCE: run 45728212306 (run number 310), head 40149
  40149389 ckpt 3372: alpha109 prepared: geckoapp versionCode 210 / 0.3.0-alpha109, extension 1.0.
  a7ea25d4 ckpt 3368: GL2d digest DRAWING section written and run on the real uploads (alpha108 = 
  f9debce0 ckpt 3365: GL2b/c coded (not compiled yet): core DrawGuard + PaintReports, Prefs drawNo
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
