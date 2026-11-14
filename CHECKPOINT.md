# CHECKPOINT 2047 — read me first, then TASKS.md

**Written:** 2026-11-14T05:13:10Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `54d257e` (this checkpoint is the commit after it)

## Just done
AV2a research REPORT saved to research/GLIDE_RESEARCH.md (Gboard/Samsung/SwiftKey/AOSP/SHARK2/Fowler/FUTO, 15 ranked ideas with our decision table); AV1 + AV3 code proven; full suite green (EXIT 0)

## Do this next
implement the report's follow-ups: (11) retry-aware demotion in MiniNav.glideDone (deleted word comes up first again -> second place), (2) PersonalModel.add rejects letters repeated 3+ times, (12) confusion bonus capped + only when shape gap small (GlideParams.confMaxGap/confCap), (4) candidate recall measure in GlideEval; then docs GLIDE_TYPING.md sections 9-10, emulator proof of AV3 (live preview strip, bubble, held digit, field row), version 137 / alpha36, ship

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    A  research/GLIDE_RESEARCH.md

## Last ten checkpoints
```
  b097785 ckpt 2044: AV3b: caps lock (double tap Shift, accent-filled key), page-field bottom row 
  7defb68 ckpt 2038: AV3a live guess while gliding (GlideTyping.preview on its own thread with its
  06a225c ckpt 2033: AV2/AV3 core: PersonalModel now remembers corrections (guessed -> meant), the
  411b5fb ckpt 2020: AV1 code: MiniNav panel geometry (grip drag detaches/moves with dock snap + d
  2287b4e ckpt 2013: owner message 36 written into TASKS (AV1 resize+detach the mini keyboard, AV2
  d4f48f8 ckpt 2010: alpha35 (kestrel-g-v0.3.0-alpha35, code 136) released and RECORDED: run 43556
  b59f884 ckpt 2005: alpha35 dispatched: android.yml queued for ref ccr-a57c163e-mtq8js (app=gecko
  072c85d ckpt 2004: gated kestrel-g-v0.3.0-alpha35 (gecko code 136) and pushed it: handoff self-t
  90d1fd1 ckpt 2002: version bump 136 / 0.3.0-alpha35 for the release of AU1-AU3
  21e5616 ckpt 1998: AU1+AU2+AU3 verified on the emulator (corner long press, page-field typing: l
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
