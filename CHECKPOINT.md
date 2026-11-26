# CHECKPOINT 3365 — read me first, then TASKS.md

**Written:** 2026-11-26T00:05:04Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/wonderful-maxwell-p7v1fm` · **builds on:** `9316a71a` (this checkpoint is the commit after it)

## Just done
GL2b/c coded (not compiled yet): core DrawGuard + PaintReports, Prefs drawNote/paintReports, GeckoPrefs always-on shortcut lines + leavePlain + trialSwitchedOff, BrowserActivity menu tile + paintReport, Settings one choice

## Do this next
1) write core DrawGuardTest + update OwnerRulesTest (perf7 line ~719-726, perf10 ~806-811, perf25 ~940-948) for the removed Safe drawing toggle; 2) compile: ./gradlew --offline -q :core:test :geckoapp:compileDebugKotlin (background, poll log); 3) GL2d digest DRAWING section in tools/diag_digest.py; 4) heartbeat trigger; 5) bump versionCode 210 / alpha109, ship.sh gecko, dispatch

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt

## Last ten checkpoints
```
  12d8b35b ckpt 3358: GL2 written into TASKS (owner words) + research/PAINT_GLITCH.md 2026-11-26 s
  d033d956 ckpt 3339: alpha108 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha108, run 455502
  7097bf33 ckpt 3337: alpha108 build dispatched ONCE (run 45550204316, head ad9506c7, app=gecko fu
  ad9506c7 ckpt 3336: gated kestrel-g-v0.3.0-alpha108 (gecko code 209) and pushed it: handoff self
  e096411f ckpt 3335: OPT4: alpha108 built from the evidence: hw2 gone (Settings, Prefs 0..1, Cata
  0aea0586 ckpt 3330: OPT4 written into TASKS in the owner's words (act on the Auto-tune run + all
  803695fb ckpt 3329: reviewed the owner's first real Auto-tune run (run 20261125-034537, DONE on 
  eeac4ff3 ckpt 3328: alpha107 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha107, run 455370
  788928f7 ckpt 3326: alpha107 build dispatched ONCE (run 45537054629, head d23bd30b, app=gecko fu
  d23bd30b ckpt 3325: gated kestrel-g-v0.3.0-alpha107 (gecko code 208) and pushed it: handoff self
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
