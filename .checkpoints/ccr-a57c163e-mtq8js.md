# CHECKPOINT 2075 — read me first, then TASKS.md

**Written:** 2026-11-14T05:52:20Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `edcc148` (this checkpoint is the commit after it)

## Just done
alpha36 DISPATCHED: android.yml run 43563546964 (run_number 87, app=gecko full_build=true, ref ccr-a57c163e-mtq8js, queued 05:51Z); ship.sh gate passed (EXIT 0)

## Do this next
wait for run 43563546964 to be green AND tag kestrel-g-v0.3.0-alpha36 (git ls-remote --tags origin), then mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha36, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha36 with the note in scratchpad alpha36_note.txt (same text as the dispatch note in ckpt 2074 commit message), then claim.sh AV2 AV3 AV3d --done, then answer the owner message 36 in plain words and re-list open items; NEVER ship alpha36 twice

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  edcc148 ckpt 2074: gated kestrel-g-v0.3.0-alpha36 (gecko code 137) and pushed it: handoff self-t
  d44b251 ckpt 2073: version bump 137 / 0.3.0-alpha36 for the release of AV1-AV3 (resize/detach, l
  eac3369 ckpt 2061: AV3 EMULATOR-PROVEN (page-field row, live guess mid-glide her|he|hd, held dig
  c5c3d98 ckpt 2057: AV2b done: repeated-letter words ('awwww') never learned (PersonalModel.stret
  39b545b ckpt 2047: AV2a research REPORT saved to research/GLIDE_RESEARCH.md (Gboard/Samsung/Swif
  b097785 ckpt 2044: AV3b: caps lock (double tap Shift, accent-filled key), page-field bottom row 
  7defb68 ckpt 2038: AV3a live guess while gliding (GlideTyping.preview on its own thread with its
  06a225c ckpt 2033: AV2/AV3 core: PersonalModel now remembers corrections (guessed -> meant), the
  411b5fb ckpt 2020: AV1 code: MiniNav panel geometry (grip drag detaches/moves with dock snap + d
  2287b4e ckpt 2013: owner message 36 written into TASKS (AV1 resize+detach the mini keyboard, AV2
```
