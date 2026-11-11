# CHECKPOINT 451 — read me first, then TASKS.md

**Written:** 2026-11-11T19:15:00Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `9c8ee6f` (this checkpoint is the commit after it)

## Just done
alpha9 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha9, gecko code 108, run 43084348983 green, APK 92.6 MB on the Release). U1 closed on the code side: Android's install rule is 10% of the partition + 2 x APK + libs + ~32 MiB (measured); updater, Diagnostics, release notes and docs updated

## Do this next
Reply to the owner (in chat, done). Then wait for their answer: Settings > Storage total/free, whether alpha9 installed in place, Compare 3 feels numbers, neon/white-gap feedback. Next big job when asked: D1 in-extension blocking (research/ADBLOCK_ENGINE.md section 3); U1c Back up/Restore only if in-place updates still fail.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  8ff3900 ckpt 448: alpha9 DISPATCHED: Actions run 43084348983 (gecko code 108, 19:08Z). alpha8 (c
  3b0bd85 ckpt 447: gated kestrel-g-v0.2.0-alpha9 (gecko code 108) and pushed it: handoff self-tes
  30794d6 ckpt 446: pre-ship: alpha9: the update screen's storage numbers now match what Android r
  e822130 ckpt 442: alpha8 build DISPATCHED: Actions run 43080438606 (android.yml, ref ccr-a57c163
  18ccff9 ckpt 441: gated kestrel-g-v0.2.0-alpha8 (gecko code 107) and pushed it: handoff self-tes
  e3285db ckpt 438: U1 cause found + fixed in code: Android needs 10% of the partition free AFTER 
  b39879a ckpt 427: Owner message after alpha7 recorded as TASKS U1: in-place updates never instal
  378cb59 ckpt 423: alpha7 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha7, gecko code 106, run 430722
  d87281f ckpt 419: alpha7 build DISPATCHED: Actions run 43072284547 (android.yml, ref ccr-a57c163
  5f068ba ckpt 418: gated kestrel-g-v0.2.0-alpha7 (gecko code 106) and pushed it: handoff self-tes
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
