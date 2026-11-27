# CHECKPOINT 3420 — read me first, then TASKS.md

**Written:** 2026-11-27T05:43:13Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/eager-hawking-6z6tcf` · **builds on:** `8376c81e` (this checkpoint is the commit after it)

## Just done
ND1: Settings lazy sections (SettingsView.section(title){rows}, Chevron, OPEN set, openAll/openSection for tests) + core LocalStamp (3 tests green) replacing DateFormat.getDateTimeInstance in both BrowserActivity filterStatus; restored selected-tab preview now carries thumbPage; SettingsLazyTest (6 Robolectric: 5 pass, ratio threshold fixed to 4x, rerun needed) ; :geckoapp:compileDebugKotlin green with GeckoView 161 fetched online; research/DIAG_2026-11-27.md written (findings + decisions)

## Do this next
1) rerun ./gradlew --offline -q :app:testDebugUnitTest --tests 'app.kestrel.browser.Settings*' (background, poll log) 2) build KR1 keep-ready: core KeepReady rule+test, geckoapp KeepReadyService + manifest (specialUse) + Prefs.keepReady + Settings row in Appearance + BrowserActivity onStop/onStart + ProcessLife.keepReady (exitIfIdle must return while on) + Health lines 3) tools/diag_digest.py: advice only from newest build family, SELF-CHECKS ('must be 0'), KILLS section 4) whole suite, lint, web-tests, ship alpha110 (versionCode 211) per HANDOFF.md section 2

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M research/DIAG_2026-11-27.md

## Last ten checkpoints
```
  2f0ab8fc ckpt 3407: ND1 (owner: 'Use the newest diagnostics and improve the browser as much as p
  7c753c88 ckpt 3381: VPS2a-c DONE: research/VPS_PLAN.md written (P40 facts + ToS limits, evidence
  af9b1f64 ckpt 3378: VPS2 (owner: NoBull P.40, in depth research for every repo + Claude + LinkMe
  3affc266 ckpt 3377: VPS1 written into TASKS in the owner's words + research/VPS_IDEAS.md (ranked
  100a331b ckpt 3376: alpha109 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha109, run 457282
  93113be6 ckpt 3373: alpha109 build DISPATCHED ONCE: run 45728212306 (run number 310), head 40149
  40149389 ckpt 3372: alpha109 prepared: geckoapp versionCode 210 / 0.3.0-alpha109, extension 1.0.
  a7ea25d4 ckpt 3368: GL2d digest DRAWING section written and run on the real uploads (alpha108 = 
  f9debce0 ckpt 3365: GL2b/c coded (not compiled yet): core DrawGuard + PaintReports, Prefs drawNo
  12d8b35b ckpt 3358: GL2 written into TASKS (owner words) + research/PAINT_GLITCH.md 2026-11-26 s
```

(12 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
