# CHECKPOINT 3376 — read me first, then TASKS.md

**Written:** 2026-11-26T00:38:32Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/wonderful-maxwell-p7v1fm` · **builds on:** `8285449c` (this checkpoint is the commit after it)

## Just done
alpha109 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha109, run 45728212306 green; GL2 ticked). Heartbeat and wake triggers deleted.

## Do this next
Wait for the owner: (1) he installs alpha109 (check Settings > Network lab > How pages are drawn shows the graphics chip with fixes, Diagnostics 'Engine prefs' scissored-cache-clears=false) and says whether cut-off pictures or text still happen; (2) after his next Send now run python3 -B -I tools/diag_digest.py and read the new DRAWING section first (mode per upload, paint report taps per mode); (3) open items: OPT3e (ask the hw1 glitch question in Auto-tune even when hw1 is already on), PERF25a (which device ran Brave), PERF17c/d, PERF19, PERF23, PERF14e, PERF9, Back missed place. This container had NO Android SDK (/opt/android-sdk missing: the environment setup script did not install it), so local :app/:geckoapp gates cannot run: GitHub CI is the compile gate.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  93113be6 ckpt 3373: alpha109 build DISPATCHED ONCE: run 45728212306 (run number 310), head 40149
  40149389 ckpt 3372: alpha109 prepared: geckoapp versionCode 210 / 0.3.0-alpha109, extension 1.0.
  a7ea25d4 ckpt 3368: GL2d digest DRAWING section written and run on the real uploads (alpha108 = 
  f9debce0 ckpt 3365: GL2b/c coded (not compiled yet): core DrawGuard + PaintReports, Prefs drawNo
  12d8b35b ckpt 3358: GL2 written into TASKS (owner words) + research/PAINT_GLITCH.md 2026-11-26 s
  d033d956 ckpt 3339: alpha108 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha108, run 455502
  7097bf33 ckpt 3337: alpha108 build dispatched ONCE (run 45550204316, head ad9506c7, app=gecko fu
  ad9506c7 ckpt 3336: gated kestrel-g-v0.3.0-alpha108 (gecko code 209) and pushed it: handoff self
  e096411f ckpt 3335: OPT4: alpha108 built from the evidence: hw2 gone (Settings, Prefs 0..1, Cata
  0aea0586 ckpt 3330: OPT4 written into TASKS in the owner's words (act on the Auto-tune run + all
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
