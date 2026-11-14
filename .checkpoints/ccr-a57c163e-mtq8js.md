# CHECKPOINT 2057 — read me first, then TASKS.md

**Written:** 2026-11-14T05:20:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `58a4c53` (this checkpoint is the commit after it)

## Just done
AV2b done: repeated-letter words ('awwww') never learned (PersonalModel.stretched, UserWords.glideable), correction lift capped at 1.6 nats and only when the path fits within 0.9/letter (GlideParams.confCap/confMaxGap, measured: right word second/third has median extra cost -0.12/letter normal), retry-aware demotion in MiniNav (deleted word first again within 6 s -> second place, stats line counts it), GlideEval gaps=1 prints recall/width/gap; PersonalModelTest 18 and MiniNavTest 64 pass; candidate recall measured 99.1/98.6/97.7/93.2 percent (calm/normal/sloppy/bad), keep=300 would give +0.3 top1 for +1.1 ms so kept 200

## Do this next
docs: research/GLIDE_TYPING.md sections 9-10 (AV2/AV3: learning additions, ablation numbers, live preview, polish, candidate recall table), TASKS.md ticks with evidence for AV1a-c AV2a-c AV3a-d, then emulator proof of AV3 (install4.log, live preview strip via glide_emu.py --shot, bubble, held digit, field row), then version 137 / alpha36 and ship

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  39b545b ckpt 2047: AV2a research REPORT saved to research/GLIDE_RESEARCH.md (Gboard/Samsung/Swif
  b097785 ckpt 2044: AV3b: caps lock (double tap Shift, accent-filled key), page-field bottom row 
  7defb68 ckpt 2038: AV3a live guess while gliding (GlideTyping.preview on its own thread with its
  06a225c ckpt 2033: AV2/AV3 core: PersonalModel now remembers corrections (guessed -> meant), the
  411b5fb ckpt 2020: AV1 code: MiniNav panel geometry (grip drag detaches/moves with dock snap + d
  2287b4e ckpt 2013: owner message 36 written into TASKS (AV1 resize+detach the mini keyboard, AV2
  d4f48f8 ckpt 2010: alpha35 (kestrel-g-v0.3.0-alpha35, code 136) released and RECORDED: run 43556
  b59f884 ckpt 2005: alpha35 dispatched: android.yml queued for ref ccr-a57c163e-mtq8js (app=gecko
  072c85d ckpt 2004: gated kestrel-g-v0.3.0-alpha35 (gecko code 136) and pushed it: handoff self-t
  90d1fd1 ckpt 2002: version bump 136 / 0.3.0-alpha35 for the release of AU1-AU3
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
