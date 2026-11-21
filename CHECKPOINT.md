# CHECKPOINT 2844 — read me first, then TASKS.md

**Written:** 2026-11-21T22:37:40Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `202d02da` (this checkpoint is the commit after it)

## Just done
Digest FILE PICKS section (+2 checks, handoff 88/88); OwnerRulesTest pins updated for the VD1 per-site exception (the first full gate failed only there: 2001/2002 core tests); rerun of :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin running (g3.log), lint not yet run on this change

## Do this next
Next: g3.log GRADLE_EXIT=0 then ONE lint run (:app:lintDebug), web-tests npm test, bump geckoapp/build.gradle.kts 188 / 0.3.0-alpha87, sync-ext-version.sh, ship.sh gecko with docs/handoff/ALPHA87_RELEASE_NOTE.txt, ONE dispatch, verify, record-release, tick VD1/IMG1 boxes, answer the owner (VD1, IMG1, VD2, GL1: GL1 needs screenshots + steps)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  0a054723 ckpt 2843: IMG1 picked-file facts written (core FilePickFacts + FilePickStats.lastPick 
  39f5dc40 ckpt 2842: Research notes written for the 3 new owner requests (IMG1_UPLOAD.md, VD2 sec
  a0c4325f ckpt 2841: Owner sent 3 more requests mid-gate (IMG1 screenshots 'can't process images'
  6ed63137 ckpt 2840: VD1c/d part 2 written (gate running): core VideoSites + test (4 pass), Shiel
  418ba1cd ckpt 2839: VD1c/d part 1 green: per-site flag ShieldFlag.AUTOSTART (core + Shields pane
  4b9a2d4a ckpt 2838: VD1a/b done: research/VIDEO_NOLOAD.md written. Cause 1 (strongest for Reels 
  ddf1d945 ckpt 2837: VD1 written into TASKS.md in the owner's words (diagnose why Instagram reels
  cb695e57 ckpt 2836: alpha86 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/
  2f0f8b2b ckpt 2835: alpha86 gated (core + app full suites via ship.sh, lint 0 errors, test_hando
  0df22017 ckpt 2834: gated kestrel-g-v0.3.0-alpha86 (gecko code 187) and pushed it: handoff self-
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
