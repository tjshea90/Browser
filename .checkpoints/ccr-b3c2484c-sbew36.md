# CHECKPOINT 2417 — read me first, then TASKS.md

**Written:** 2026-11-15T19:16:53Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `6b3a5337` (this checkpoint is the commit after it)

## Just done
lab scene states fixed (the app turns a wide video sideways by itself at full screen start, then real turns with the virtual sensor), HANDOFF alpha49 map, research/FULLSCREEN_BARS.md

## Do this next
NEXT: wait for review agent a1b60178ab815123f (Kotlin diff of alpha49) and release run 43751318494 (alpha49, tag kestrel-g-v0.3.0-alpha49: a background monitor prints when the tag exists); apply review fixes if real; then CANCEL the pending lab run 43751321208 (it pins commit 0a81c2b8 = the OLD driver) and re-dispatch app=lab lab_suite=fsy lab_minutes=10; the compat run 43747605584 ends about 20:10Z; record-release.sh kestrel-g-v0.3.0-alpha49 with the identical note only after the release run is green; then answer the owner (ANS44/45/46)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  0a81c2b8 ckpt 2416: gated kestrel-g-v0.3.0-alpha49 (gecko code 150) and pushed it: handoff self-
  afdbb7c6 ckpt 2415: pre-ship: alpha49: full-screen video fixes after your three screenshots. (1)
  7c987e9f ckpt 2414: code for owner message 46 + second review: FSB1 (applyImmersive without LAYO
  7f2c4c92 ckpt 2413: owner message 46 written into TASKS.md (FSB1 bars cover the controls in full
  b8c5d333 ckpt 2412: hour soak 43740398387 read (commit 281c48f4, hotspot-shaped, 70 min, 79 scen
  9edf1ab7 ckpt 2411: LAB2 harness written: lab-only page-health probe (vfind.js probe(), guard an
  4a9a28f4 ckpt 2410: SND1a lab scene written: lab page /video/audible?kind=video|audio (big butto
  44f9516c ckpt 2409: wrote owner message 45 into TASKS.md in the owner's words: AU1 (sound must n
  d5053cd6 ckpt 2408: owner answered in plain words for messages 43 and 44 (alpha48 Release link g
  055248fc ckpt 2407: alpha48 RELEASED: tag kestrel-g-v0.3.0-alpha48 (commit 281c48f4, APK 94.7 MB
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
