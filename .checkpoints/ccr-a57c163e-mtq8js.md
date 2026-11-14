# CHECKPOINT 2073 — read me first, then TASKS.md

**Written:** 2026-11-14T05:51:03Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `98310d5` (this checkpoint is the commit after it)

## Just done
version bump 137 / 0.3.0-alpha36 for the release of AV1-AV3 (resize/detach, learning round two, polish); full suite green (core + app + lint + gecko compile, EXIT 0); TASKS AV1a-c AV2a-c AV3a-c ticked with evidence; HANDOFF feature map extended

## Do this next
bash ship.sh gecko "$(cat scratchpad alpha36_note.txt)" (runs the gates + unit suites + pushes), then dispatch android.yml app=gecko full_build=true ref ccr-a57c163e-mtq8js with the same note, wait for tag + green run (git ls-remote tag kestrel-g-v0.3.0-alpha36), get_release_by_tag, bash tools/record-release.sh kestrel-g-v0.3.0-alpha36, claim.sh AV2/AV3/AV3d --done, answer the owner (message 36) in plain words and re-list open items

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  eac3369 ckpt 2061: AV3 EMULATOR-PROVEN (page-field row, live guess mid-glide her|he|hd, held dig
  c5c3d98 ckpt 2057: AV2b done: repeated-letter words ('awwww') never learned (PersonalModel.stret
  39b545b ckpt 2047: AV2a research REPORT saved to research/GLIDE_RESEARCH.md (Gboard/Samsung/Swif
  b097785 ckpt 2044: AV3b: caps lock (double tap Shift, accent-filled key), page-field bottom row 
  7defb68 ckpt 2038: AV3a live guess while gliding (GlideTyping.preview on its own thread with its
  06a225c ckpt 2033: AV2/AV3 core: PersonalModel now remembers corrections (guessed -> meant), the
  411b5fb ckpt 2020: AV1 code: MiniNav panel geometry (grip drag detaches/moves with dock snap + d
  2287b4e ckpt 2013: owner message 36 written into TASKS (AV1 resize+detach the mini keyboard, AV2
  d4f48f8 ckpt 2010: alpha35 (kestrel-g-v0.3.0-alpha35, code 136) released and RECORDED: run 43556
  b59f884 ckpt 2005: alpha35 dispatched: android.yml queued for ref ccr-a57c163e-mtq8js (app=gecko
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
