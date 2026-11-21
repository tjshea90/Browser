# CHECKPOINT 2787 — read me first, then TASKS.md

**Written:** 2026-11-21T06:33:33Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `98e6b35c` (this checkpoint is the commit after it)

## Just done
alpha80 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha80, gecko code 181, run 44874632117 all jobs green first time, APK 96.6 MB + sha256 + .dm, BUILDLOG.md pushed). BK10 ticked: Instant Back proven on the real engine in the lab (run 44873006097).

## Do this next
LD1: now the live-diagnostics request. Read app/src/main/kotlin/app/kestrel/browser/diag/Health.kt, LastRun.kt, PageTimes.kt, CrashNote.kt, the Diagnostics builder (BrowserActivity ~2550), BackupManager (token must not be exported), tools/secretscan.sh patterns; write research/LIVE_DIAG.md (design + feasibility in plain words) and build core DiagScrub first, tests first. Remember: the owner has NOT yet been told about alpha80 (answer him with the Release link at the end of the turn, together with the LD1 answer)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  f12802a2 ckpt 2786: LD1 written into TASKS.md in the owner's words (live diagnostics engine with
  5c4edd25 ckpt 2785: REL80b: alpha80 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  0c2b1e08 ckpt 2784: gated kestrel-g-v0.3.0-alpha80 (gecko code 181) and pushed it: handoff self-
  0f713e4a ckpt 2783: BK10 PROVEN on the real engine (lab run 44873006097): no-store page + Instan
  08d2776b ckpt 2782: BK10 lab run 44872447722 (suite back): all three rounds failed before any Ba
  2a29901b ckpt 2781: BK10 coded + tested: (1) extension: blocking main_frame listener turns Cache
  d91a5fd5 ckpt 2780: BK10 written into TASKS.md in the owner's words (Back button: slow load whil
  a70d55af ckpt 2779: alpha79 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha79, gecko code 180, run
  5a64fefc ckpt 2778: alpha79 run 44849629774: test-jvm failed ONCE on PhoneBackendTest.aPhoneThat
  a3a89687 ckpt 2777: REL79b: alpha79 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
```
