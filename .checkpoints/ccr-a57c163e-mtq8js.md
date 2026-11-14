# CHECKPOINT 2038 — read me first, then TASKS.md

**Written:** 2026-11-14T05:04:49Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `65054b3` (this checkpoint is the commit after it)

## Just done
AV3a live guess while gliding (GlideTyping.preview on its own thread with its own decoder, strip shows it, Diagnostics 'word shown N ms after the lift' + 'live preview'); AV1 proven on the emulator (grip drag detaches, corner drag resizes, glide works on the resized keyboard, drag near the corner docks, size kept); lift position applied at the end of a drag

## Do this next
AV3b: shift lock (double tap Shift), field-mode bottom row with comma/apostrophe/question mark, long-press digits on the top row, key preview bubble; then emulator proof, docs (research/MINI_NAV.md AV1 section, GLIDE_TYPING.md), research agent report -> research/GLIDE_RESEARCH.md, ship alpha36

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  06a225c ckpt 2033: AV2/AV3 core: PersonalModel now remembers corrections (guessed -> meant), the
  411b5fb ckpt 2020: AV1 code: MiniNav panel geometry (grip drag detaches/moves with dock snap + d
  2287b4e ckpt 2013: owner message 36 written into TASKS (AV1 resize+detach the mini keyboard, AV2
  d4f48f8 ckpt 2010: alpha35 (kestrel-g-v0.3.0-alpha35, code 136) released and RECORDED: run 43556
  b59f884 ckpt 2005: alpha35 dispatched: android.yml queued for ref ccr-a57c163e-mtq8js (app=gecko
  072c85d ckpt 2004: gated kestrel-g-v0.3.0-alpha35 (gecko code 136) and pushed it: handoff self-t
  90d1fd1 ckpt 2002: version bump 136 / 0.3.0-alpha35 for the release of AU1-AU3
  21e5616 ckpt 1998: AU1+AU2+AU3 verified on the emulator (corner long press, page-field typing: l
  95be9ed ckpt 1994: AU3 verified on the JVM side: core 876/877 (the one failure is a download tim
  85e6e7a ckpt 1990: AU3 code: TextEdit (core, 7 tests), PageField interface + MiniNav field mode 
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
