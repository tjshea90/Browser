# CHECKPOINT 2843 — read me first, then TASKS.md

**Written:** 2026-11-21T22:33:13Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `7c87ac96` (this checkpoint is the commit after it)

## Just done
IMG1 picked-file facts written (core FilePickFacts + FilePickStats.lastPick + tests, BrowserActivity.pickedFacts on Io never private, PrivateTabsTest pin), ALPHA87_RELEASE_NOTE.txt drafted; the VD1 gate (g2.log) still in lint, NOT yet seen green; new tests (core FilePickFactsTest/FilePickRulesTest, PrivateTabsTest pin) not yet run

## Do this next
Next: wait g2.log GRADLE_EXIT=0 (lint), then ONE more gradle run: :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin for the IMG1 additions; web-tests npm test; test_handoff.sh; versionCode 188 / 0.3.0-alpha87 + sync-ext-version.sh; ship.sh gecko with the ALPHA87 note; ONE dispatch; verify; record-release; tick VD1/IMG1 boxes; answer owner (IMG1, VD1, VD2, GL1)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  39f5dc40 ckpt 2842: Research notes written for the 3 new owner requests (IMG1_UPLOAD.md, VD2 sec
  a0c4325f ckpt 2841: Owner sent 3 more requests mid-gate (IMG1 screenshots 'can't process images'
  6ed63137 ckpt 2840: VD1c/d part 2 written (gate running): core VideoSites + test (4 pass), Shiel
  418ba1cd ckpt 2839: VD1c/d part 1 green: per-site flag ShieldFlag.AUTOSTART (core + Shields pane
  4b9a2d4a ckpt 2838: VD1a/b done: research/VIDEO_NOLOAD.md written. Cause 1 (strongest for Reels 
  ddf1d945 ckpt 2837: VD1 written into TASKS.md in the owner's words (diagnose why Instagram reels
  cb695e57 ckpt 2836: alpha86 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/
  2f0f8b2b ckpt 2835: alpha86 gated (core + app full suites via ship.sh, lint 0 errors, test_hando
  0df22017 ckpt 2834: gated kestrel-g-v0.3.0-alpha86 (gecko code 187) and pushed it: handoff self-
  620bf412 ckpt 2833: pre-ship: alpha86: private tabs checked end to end, and every hole found is 
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
