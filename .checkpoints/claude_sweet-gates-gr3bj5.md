# CHECKPOINT 2778 — read me first, then TASKS.md

**Written:** 2026-11-21T01:42:21Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `a3a89687` (this checkpoint is the commit after it)

## Just done
alpha79 run 44849629774: test-jvm failed ONCE on PhoneBackendTest.aPhoneThatIsKilledInTheMiddleOfAVideoFinishesItAfterTheRestart (PhoneBackendTest.kt:94, the test's own 40 s await timed out on the shared runner; unrelated to the file-chooser change; passes locally and on alpha76-78). No tag/Release exists. rerun_failed_jobs requested for the SAME run (not a new dispatch). Noted in research/ATT1_FILE_CHOOSER.md.

## Do this next
REL79d: mcp__github__actions_list list_workflow_jobs 44849629774 (filter latest) until test-jvm + build-gecko are success (run_attempt 2), then tag kestrel-g-v0.3.0-alpha79 exists, get_release_by_tag shows the APK, bash tools/record-release.sh kestrel-g-v0.3.0-alpha79 "$(cat docs/handoff/ALPHA79_RELEASE_NOTE.txt)", then answer the owner (ATT1). If test-jvm fails AGAIN on the same test: harden PhoneBackendTest (longer await timeout) in its own commit, then re-dispatch ONLY if still no tag. DO NOT run a second dispatch while this run can still finish

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M research/ATT1_FILE_CHOOSER.md

## Last ten checkpoints
```
  a3a89687 ckpt 2777: REL79b: alpha79 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  e04b99e6 ckpt 2776: gated kestrel-g-v0.3.0-alpha79 (gecko code 180) and pushed it: handoff self-
  79a107c9 ckpt 2775: ATT1: file-chooser hardening coded + tested (core FilePickRulesTest + OwnerR
  299f3c4b ckpt 2774: ATT1 written into TASKS.md in the owner's words (new owner message: attachin
  9a0d1e24 ckpt 2773: alpha78 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha78, gecko code 179, run
  c8ad5f40 ckpt 2772: REL78b: alpha78 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  74fab19a ckpt 2771: gated kestrel-g-v0.3.0-alpha78 (gecko code 179) and pushed it: handoff self-
  afb6d504 ckpt 2770: YTC2: cause NOT proven. Read every tap path again (ControlsMachine, tap matc
  2ffbb683 ckpt 2769: YTC2 written into TASKS.md in the owner's words: alpha77 did not change the 
  36547fc5 ckpt 2768: alpha77 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha77, gecko code 178, run
```
