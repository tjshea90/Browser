# CHECKPOINT 3373 — read me first, then TASKS.md

**Written:** 2026-11-26T00:22:20Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/wonderful-maxwell-p7v1fm` · **builds on:** `40149389` (this checkpoint is the commit after it)

## Just done
alpha109 build DISPATCHED ONCE: run 45728212306 (run number 310), head 40149389, app=gecko full_build=true, started 00:22Z

## Do this next
Wait for run 45728212306: mcp__github__actions_list list_workflow_jobs resource_id 45728212306 (get_job_logs answers 404 until the job ends). All jobs green -> git ls-remote --tags origin shows kestrel-g-v0.3.0-alpha109, get_release_by_tag shows the APK -> bash tools/record-release.sh kestrel-g-v0.3.0-alpha109 with the IDENTICAL note (docs/handoff/ALPHA109_RELEASE_NOTE.txt) -> delete_trigger trig_01S6LL8DicuFQrb8btAAePUx -> tick GL2e in TASKS (claim.sh GL2 --done) -> answer the owner with the Release link on its own line. DO NOT dispatch again. If red: read the failing job (test-jvm / build-gecko = compile error in the Android edits of ckpt 3365: GeckoPrefs.kt, BrowserActivity.kt paintReport, SettingsView.kt), fix, re-dispatch only if no tag was created.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  40149389 ckpt 3372: alpha109 prepared: geckoapp versionCode 210 / 0.3.0-alpha109, extension 1.0.
  a7ea25d4 ckpt 3368: GL2d digest DRAWING section written and run on the real uploads (alpha108 = 
  f9debce0 ckpt 3365: GL2b/c coded (not compiled yet): core DrawGuard + PaintReports, Prefs drawNo
  12d8b35b ckpt 3358: GL2 written into TASKS (owner words) + research/PAINT_GLITCH.md 2026-11-26 s
  d033d956 ckpt 3339: alpha108 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha108, run 455502
  7097bf33 ckpt 3337: alpha108 build dispatched ONCE (run 45550204316, head ad9506c7, app=gecko fu
  ad9506c7 ckpt 3336: gated kestrel-g-v0.3.0-alpha108 (gecko code 209) and pushed it: handoff self
  e096411f ckpt 3335: OPT4: alpha108 built from the evidence: hw2 gone (Settings, Prefs 0..1, Cata
  0aea0586 ckpt 3330: OPT4 written into TASKS in the owner's words (act on the Auto-tune run + all
  803695fb ckpt 3329: reviewed the owner's first real Auto-tune run (run 20261125-034537, DONE on 
```
