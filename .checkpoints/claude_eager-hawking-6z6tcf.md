# CHECKPOINT 3479 — read me first, then TASKS.md

**Written:** 2026-11-27T07:30:46Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/eager-hawking-6z6tcf` · **builds on:** `697d4eb7` (this checkpoint is the commit after it)

## Just done
ND2: marker pairing fixed in tools/profile_digest.py (phase 1/2/3), network + extension-plumbing reports; background.js listeners narrowed to frames+xhr (rvStart too); web tests 135/135

## Do this next
finish alpha112: write research/PROFILE_CLAUDE_AI.md + TASKS boxes; run :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin tools/test_handoff.sh; bump geckoapp/build.gradle.kts 212->213 alpha112; ship.sh gecko; dispatch; record-release

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M tools/test_profile_digest.py

## Last ten checkpoints
```
  cf0d3989 ckpt 3465: alpha110 AND alpha111 RELEASED AND RECORDED (BUILDLOG lines 211 and 212). ND
  9a47bf75 ckpt 3461: alpha111 RE-DISPATCHED ONCE after fixes: run 45954070324 (run number 313, he
  240256a0 ckpt 3460: gated kestrel-g-v0.3.0-alpha111 (gecko code 212) and pushed it: handoff self
  3382c4f8 ckpt 3457: alpha111 DISPATCHED ONCE: run 45952674368 (run number 312, head 84d5657e, ap
  84d5657e ckpt 3456: gated kestrel-g-v0.3.0-alpha111 (gecko code 212) and pushed it: handoff self
  561bfd0f ckpt 3454: pre-ship: alpha111: two new instruments, so the next review can see what cla
  1f3ac22f ckpt 3448: ND1d/ND1e CODED (not shipped yet): core ProfileRule (+5 tests), geckoapp Pag
  8433cdc4 ckpt 3440: alpha110 DISPATCHED ONCE: run 45949557236 (run number 311, head 91326acd, re
  91326acd ckpt 3439: gated kestrel-g-v0.3.0-alpha110 (gecko code 211) and pushed it: handoff self
  8d2e39d0 ckpt 3432: KR1 keep-the-page-ready DONE in code: core KeepReady (4 tests) + geckoapp Ke
```

(13 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
