# CHECKPOINT 3372 — read me first, then TASKS.md

**Written:** 2026-11-26T00:21:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/wonderful-maxwell-p7v1fm` · **builds on:** `507cc8e8` (this checkpoint is the commit after it)

## Just done
alpha109 prepared: geckoapp versionCode 210 / 0.3.0-alpha109, extension 1.0.210, release note docs/handoff/ALPHA109_RELEASE_NOTE.txt, GL2b/c/d ticked, whole core suite green (exit 0). This container has NO Android SDK so ship.sh's local :app/:geckoapp gates cannot run: the workflow's test-jvm and build-gecko jobs are the compile gate.

## Do this next
DISPATCH android.yml ONCE (mcp__github__actions_run_trigger run_workflow ref claude/wonderful-maxwell-p7v1fm inputs app=gecko full_build=true note=<docs/handoff/ALPHA109_RELEASE_NOTE.txt>), wait for all jobs green (actions_list list_workflow_jobs), get_release_by_tag kestrel-g-v0.3.0-alpha109 shows the APK, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha109 with the identical note, delete heartbeat trig_01S6LL8DicuFQrb8btAAePUx, tick GL2e, answer the owner with the Release link. If the run is red: read the failing job, fix, bump to code 211 / alpha110 only if the tag was already created, else re-dispatch.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a7ea25d4 ckpt 3368: GL2d digest DRAWING section written and run on the real uploads (alpha108 = 
  f9debce0 ckpt 3365: GL2b/c coded (not compiled yet): core DrawGuard + PaintReports, Prefs drawNo
  12d8b35b ckpt 3358: GL2 written into TASKS (owner words) + research/PAINT_GLITCH.md 2026-11-26 s
  d033d956 ckpt 3339: alpha108 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha108, run 455502
  7097bf33 ckpt 3337: alpha108 build dispatched ONCE (run 45550204316, head ad9506c7, app=gecko fu
  ad9506c7 ckpt 3336: gated kestrel-g-v0.3.0-alpha108 (gecko code 209) and pushed it: handoff self
  e096411f ckpt 3335: OPT4: alpha108 built from the evidence: hw2 gone (Settings, Prefs 0..1, Cata
  0aea0586 ckpt 3330: OPT4 written into TASKS in the owner's words (act on the Auto-tune run + all
  803695fb ckpt 3329: reviewed the owner's first real Auto-tune run (run 20261125-034537, DONE on 
  eeac4ff3 ckpt 3328: alpha107 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha107, run 455370
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
