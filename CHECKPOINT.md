# CHECKPOINT 2783 — read me first, then TASKS.md

**Written:** 2026-11-21T06:02:37Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `cabd24e2` (this checkpoint is the commit after it)

## Just done
BK10 PROVEN on the real engine (lab run 44873006097): no-store page + Instant Back = KEPT on Back, control under /login = loaded again from the network. Added the stale-history-copy guard (BackScrollRules.sameAddress, tested) for the owner's glitch. Bumped code 181 / alpha80, note written. NOT shipped yet.

## Do this next
REL80: bash ship.sh gecko "$(cat docs/handoff/ALPHA80_RELEASE_NOTE.txt)" (WAIT for it to finish, then confirm the commit is pushed BEFORE dispatching), ONE dispatch (android.yml app=gecko full_build=true ref claude/sweet-gates-gr3bj5, same note), wait tag kestrel-g-v0.3.0-alpha80 + all jobs (a flaky PhoneBackendTest may need rerun_failed_jobs, see FLK1), get_release_by_tag, bash tools/record-release.sh kestrel-g-v0.3.0-alpha80 with the identical note; answer the owner (BK10)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M geckoapp/build.gradle.kts
     M geckoapp/src/main/assets/kestrel-shields/manifest.json
     M research/BACK_SCROLL.md
    ?? docs/handoff/ALPHA80_RELEASE_NOTE.txt

## Last ten checkpoints
```
  08d2776b ckpt 2782: BK10 lab run 44872447722 (suite back): all three rounds failed before any Ba
  2a29901b ckpt 2781: BK10 coded + tested: (1) extension: blocking main_frame listener turns Cache
  d91a5fd5 ckpt 2780: BK10 written into TASKS.md in the owner's words (Back button: slow load whil
  a70d55af ckpt 2779: alpha79 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha79, gecko code 180, run
  5a64fefc ckpt 2778: alpha79 run 44849629774: test-jvm failed ONCE on PhoneBackendTest.aPhoneThat
  a3a89687 ckpt 2777: REL79b: alpha79 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  e04b99e6 ckpt 2776: gated kestrel-g-v0.3.0-alpha79 (gecko code 180) and pushed it: handoff self-
  79a107c9 ckpt 2775: ATT1: file-chooser hardening coded + tested (core FilePickRulesTest + OwnerR
  299f3c4b ckpt 2774: ATT1 written into TASKS.md in the owner's words (new owner message: attachin
  9a0d1e24 ckpt 2773: alpha78 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha78, gecko code 179, run
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
