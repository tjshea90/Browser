# CHECKPOINT 2779 — read me first, then TASKS.md

**Written:** 2026-11-21T02:00:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `11c69d86` (this checkpoint is the commit after it)

## Just done
alpha79 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha79, gecko code 180, run 44849629774 attempt 2 all jobs green; attempt 1 failed only on the flaky PhoneBackendTest, FLK1 in TASKS; APK 96.6 MB + sha256 + .dm, BUILDLOG.md line pushed). ATT1: file-chooser hardening shipped, NOT proven on the tablet.

## Do this next
NOTHING is running. Waiting for the owner: (1) does attaching a file on Gemini work on alpha79? If not, send the Diagnostics line 'File chooser (attach a file on a page): ...' (asked / opened / no picker app / nothing chosen / files given / last) and say whether Settings > Back up and restore > Restore shows a file chooser. (2) the YouTube controls report (YTC1/YTC2, alpha78): which Settings switch makes them stay. Read research/ATT1_FILE_CHOOSER.md and research/YTC1_YOUTUBE_CONTROLS.md. Open: FLK1 (harden PhoneBackendTest in its own commit), INBOX/OUTBOX capture OFF (private clone missing, add_repo denied twice), a lab scene with a file input (tools/emu/soak.py) would prove the chooser on the real engine

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  5a64fefc ckpt 2778: alpha79 run 44849629774: test-jvm failed ONCE on PhoneBackendTest.aPhoneThat
  a3a89687 ckpt 2777: REL79b: alpha79 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  e04b99e6 ckpt 2776: gated kestrel-g-v0.3.0-alpha79 (gecko code 180) and pushed it: handoff self-
  79a107c9 ckpt 2775: ATT1: file-chooser hardening coded + tested (core FilePickRulesTest + OwnerR
  299f3c4b ckpt 2774: ATT1 written into TASKS.md in the owner's words (new owner message: attachin
  9a0d1e24 ckpt 2773: alpha78 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha78, gecko code 179, run
  c8ad5f40 ckpt 2772: REL78b: alpha78 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  74fab19a ckpt 2771: gated kestrel-g-v0.3.0-alpha78 (gecko code 179) and pushed it: handoff self-
  afb6d504 ckpt 2770: YTC2: cause NOT proven. Read every tap path again (ControlsMachine, tap matc
  2ffbb683 ckpt 2769: YTC2 written into TASKS.md in the owner's words: alpha77 did not change the 
```
