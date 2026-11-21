# CHECKPOINT 2775 — read me first, then TASKS.md

**Written:** 2026-11-21T01:08:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `59e1299f` (this checkpoint is the commit after it)

## Just done
ATT1: file-chooser hardening coded + tested (core FilePickRulesTest + OwnerRulesTest 42 of 42, :geckoapp compile EXIT 0): no-picker toast + answer, GET_CONTENT -> OPEN_DOCUMENT fallback, extension types kept in the filter, folder asks answered, manifest queries for Android 11+, Health lines + Diagnostics line 'File chooser'. Real default.kfs probe: Gemini upload / picker addresses are NOT blocked. NOT proven which step stops on the tablet. research/ATT1_FILE_CHOOSER.md written. Bumped code 180 / alpha79, note written. NOT shipped yet.

## Do this next
REL79: bash ship.sh gecko "$(cat docs/handoff/ALPHA79_RELEASE_NOTE.txt)", ONE dispatch (android.yml app=gecko full_build=true ref claude/sweet-gates-gr3bj5, same note), wait tag kestrel-g-v0.3.0-alpha79 + all jobs, get_release_by_tag, bash tools/record-release.sh kestrel-g-v0.3.0-alpha79 with the identical note; then answer the owner (ATT1): link alone on its line, say it is not proven, the Back up and restore check, the Diagnostics line to send

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M geckoapp/build.gradle.kts
     M geckoapp/src/main/assets/kestrel-shields/manifest.json
    ?? docs/handoff/ALPHA79_RELEASE_NOTE.txt
    ?? research/ATT1_FILE_CHOOSER.md

## Last ten checkpoints
```
  299f3c4b ckpt 2774: ATT1 written into TASKS.md in the owner's words (new owner message: attachin
  9a0d1e24 ckpt 2773: alpha78 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha78, gecko code 179, run
  c8ad5f40 ckpt 2772: REL78b: alpha78 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  74fab19a ckpt 2771: gated kestrel-g-v0.3.0-alpha78 (gecko code 179) and pushed it: handoff self-
  afb6d504 ckpt 2770: YTC2: cause NOT proven. Read every tap path again (ControlsMachine, tap matc
  2ffbb683 ckpt 2769: YTC2 written into TASKS.md in the owner's words: alpha77 did not change the 
  36547fc5 ckpt 2768: alpha77 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha77, gecko code 178, run
  3f0787f7 ckpt 2767: REL77b: alpha77 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  2a4e281c ckpt 2766: gated kestrel-g-v0.3.0-alpha77 (gecko code 178) and pushed it: handoff self-
  855b173d ckpt 2765: YTC1: not reproduced (the YouTube film does not start behind its bot check i
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
