# CHECKPOINT 2782 — read me first, then TASKS.md

**Written:** 2026-11-21T05:54:00Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `280b8dc7` (this checkpoint is the commit after it)

## Just done
BK10 lab run 44872447722 (suite back): all three rounds failed before any Back press with FileNotFoundError: tools/emu/back_emu.py had the sandbox's adb path hard-coded. Fixed: soak.py back_scene now sets back_emu.ADB = the lab's own Adb.bin. The code under test is unchanged. Local gates for the BK10 change: web npm test 653 of 653, :core + :app tests green, :app:lintDebug 0 errors, handoff self-test 69 of 69.

## Do this next
BK10d: re-dispatch the lab ONCE (android.yml app=lab lab_suite=back lab_profile=standard lab_net=full lab_minutes=5), wait for the artifact emulator-lab-text (list_workflow_run_artifacts), fetch it with python3 -B -I tools/emu/fetch_artifact.py <archive_download_url> <empty dir>, read report.md + events.jsonl rows kind=back (static persisted? nostore-instant-back new_get? sign-in control new_get?) and write section 6 result in research/BACK_SCROLL.md; then ship alpha80 (versionCode 181)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2a29901b ckpt 2781: BK10 coded + tested: (1) extension: blocking main_frame listener turns Cache
  d91a5fd5 ckpt 2780: BK10 written into TASKS.md in the owner's words (Back button: slow load whil
  a70d55af ckpt 2779: alpha79 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha79, gecko code 180, run
  5a64fefc ckpt 2778: alpha79 run 44849629774: test-jvm failed ONCE on PhoneBackendTest.aPhoneThat
  a3a89687 ckpt 2777: REL79b: alpha79 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  e04b99e6 ckpt 2776: gated kestrel-g-v0.3.0-alpha79 (gecko code 180) and pushed it: handoff self-
  79a107c9 ckpt 2775: ATT1: file-chooser hardening coded + tested (core FilePickRulesTest + OwnerR
  299f3c4b ckpt 2774: ATT1 written into TASKS.md in the owner's words (new owner message: attachin
  9a0d1e24 ckpt 2773: alpha78 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha78, gecko code 179, run
  c8ad5f40 ckpt 2772: REL78b: alpha78 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
