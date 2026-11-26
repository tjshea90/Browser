# CHECKPOINT 3368 — read me first, then TASKS.md

**Written:** 2026-11-26T00:06:43Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/wonderful-maxwell-p7v1fm` · **builds on:** `736c4813` (this checkpoint is the commit after it)

## Just done
GL2d digest DRAWING section written and run on the real uploads (alpha108 = PLAIN graphics chip FAILING MODE, alpha107 = with fixes, alpha106 = processor); DrawGuardTest + OwnerRulesTest updated; core tests running online (core2.log in scratchpad)

## Do this next
Check scratchpad core2.log for EXIT 0 (gradle -q :core:test --tests gfx.* --tests OwnerRulesTest; offline fails in a fresh container, run WITHOUT --offline); then ./gradlew :geckoapp:compileDebugKotlin :app:testDebugUnitTest :geckoapp:testDebugUnitTest; then tick GL2b/c/d in TASKS, bump geckoapp versionCode 210 / alpha109, bash tools/sync-ext-version.sh, bash ship.sh gecko, ONE dispatch, record-release

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f9debce0 ckpt 3365: GL2b/c coded (not compiled yet): core DrawGuard + PaintReports, Prefs drawNo
  12d8b35b ckpt 3358: GL2 written into TASKS (owner words) + research/PAINT_GLITCH.md 2026-11-26 s
  d033d956 ckpt 3339: alpha108 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha108, run 455502
  7097bf33 ckpt 3337: alpha108 build dispatched ONCE (run 45550204316, head ad9506c7, app=gecko fu
  ad9506c7 ckpt 3336: gated kestrel-g-v0.3.0-alpha108 (gecko code 209) and pushed it: handoff self
  e096411f ckpt 3335: OPT4: alpha108 built from the evidence: hw2 gone (Settings, Prefs 0..1, Cata
  0aea0586 ckpt 3330: OPT4 written into TASKS in the owner's words (act on the Auto-tune run + all
  803695fb ckpt 3329: reviewed the owner's first real Auto-tune run (run 20261125-034537, DONE on 
  eeac4ff3 ckpt 3328: alpha107 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha107, run 455370
  788928f7 ckpt 3326: alpha107 build dispatched ONCE (run 45537054629, head d23bd30b, app=gecko fu
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
