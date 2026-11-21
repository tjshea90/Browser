# CHECKPOINT 2785 — read me first, then TASKS.md

**Written:** 2026-11-21T06:15:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `0c2b1e08` (this checkpoint is the commit after it)

## Just done
REL80b: alpha80 DISPATCHED ONCE (android.yml app=gecko full_build=true ref claude/sweet-gates-gr3bj5, note = docs/handoff/ALPHA80_RELEASE_NOTE.txt, gecko code 181) at about 06:15Z. DO NOT DISPATCH AGAIN.

## Do this next
REL80c: find the run (list_workflow_runs android.yml branch claude/sweet-gates-gr3bj5, the newest gecko one), wait until test-jvm + build-gecko are success (list_workflow_jobs; if test-jvm fails ONLY on PhoneBackendTest, FLK1, use rerun_failed_jobs on the SAME run, never a second dispatch), tag kestrel-g-v0.3.0-alpha80 exists (git ls-remote --tags origin), get_release_by_tag shows the APK, THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha80 "$(cat docs/handoff/ALPHA80_RELEASE_NOTE.txt)"; answer the owner (BK10): Release link alone on its line, what Instant Back does and the privacy trade-off, the Settings switch, what is still slow, NOT run on the tablet

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  0c2b1e08 ckpt 2784: gated kestrel-g-v0.3.0-alpha80 (gecko code 181) and pushed it: handoff self-
  0f713e4a ckpt 2783: BK10 PROVEN on the real engine (lab run 44873006097): no-store page + Instan
  08d2776b ckpt 2782: BK10 lab run 44872447722 (suite back): all three rounds failed before any Ba
  2a29901b ckpt 2781: BK10 coded + tested: (1) extension: blocking main_frame listener turns Cache
  d91a5fd5 ckpt 2780: BK10 written into TASKS.md in the owner's words (Back button: slow load whil
  a70d55af ckpt 2779: alpha79 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha79, gecko code 180, run
  5a64fefc ckpt 2778: alpha79 run 44849629774: test-jvm failed ONCE on PhoneBackendTest.aPhoneThat
  a3a89687 ckpt 2777: REL79b: alpha79 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  e04b99e6 ckpt 2776: gated kestrel-g-v0.3.0-alpha79 (gecko code 180) and pushed it: handoff self-
  79a107c9 ckpt 2775: ATT1: file-chooser hardening coded + tested (core FilePickRulesTest + OwnerR
```
