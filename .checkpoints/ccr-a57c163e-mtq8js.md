# CHECKPOINT 2083 — read me first, then TASKS.md

**Written:** 2026-11-14T06:02:07Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `0d038c3` (this checkpoint is the commit after it)

## Just done
heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN DISABLED (update_trigger enabled=false; its two wake-ups at 02:17Z and 04:17Z fired mid-session and were stale: alpha36 was already shipped and recorded); owner message 36 answered in plain words with the Release link

## Do this next
WAIT FOR THE OWNER: nothing safe is left to build. When they write: (1) paste of Menu > Diagnostics 'Glide typing' + 'Mini keyboard in page fields' lines from the tablet -> compare 'best word kept %', 'word shown N ms after the lift', 'live preview avg ms' with research/GLIDE_TYPING.md sections 9-10 and decide tuning (keep=300 costs +1.1 ms laptop for +0.3 pt; RETRY_MS 6000; confMaxGap 0.9); (2) any new request -> write it into TASKS.md in their words first. If a later session needs a heartbeat again: re-enable with update_trigger enabled=true or create a new one; delete trig_01LLoVKsjuTUaDn61UGP1pBN after 24 h of owner silence

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5375332 ckpt 2081: alpha36 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha36 (gecko code 137), run
  104a302 ckpt 2075: alpha36 DISPATCHED: android.yml run 43563546964 (run_number 87, app=gecko ful
  edcc148 ckpt 2074: gated kestrel-g-v0.3.0-alpha36 (gecko code 137) and pushed it: handoff self-t
  d44b251 ckpt 2073: version bump 137 / 0.3.0-alpha36 for the release of AV1-AV3 (resize/detach, l
  eac3369 ckpt 2061: AV3 EMULATOR-PROVEN (page-field row, live guess mid-glide her|he|hd, held dig
  c5c3d98 ckpt 2057: AV2b done: repeated-letter words ('awwww') never learned (PersonalModel.stret
  39b545b ckpt 2047: AV2a research REPORT saved to research/GLIDE_RESEARCH.md (Gboard/Samsung/Swif
  b097785 ckpt 2044: AV3b: caps lock (double tap Shift, accent-filled key), page-field bottom row 
  7defb68 ckpt 2038: AV3a live guess while gliding (GlideTyping.preview on its own thread with its
  06a225c ckpt 2033: AV2/AV3 core: PersonalModel now remembers corrections (guessed -> meant), the
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
