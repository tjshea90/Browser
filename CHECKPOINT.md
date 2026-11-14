# CHECKPOINT 2074 — read me first, then TASKS.md

**Written:** 2026-11-14T05:51:33Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `d44b251` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha36 (gecko code 137) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha36 (includes alpha35): 1) The mini keyboard can be resized and moved. Drag one of the small corner marks to make it bigger or smaller (the keys and letters grow with it and glide typing follows the new key positions). Drag the thin bar on top of the panel to move the whole panel anywhere on the screen; let go near the top right corner, or double tap that bar, and it docks back. The size and place are remembered, and Settings has a button Reset the mini keyboard's size and position. Holding the invisible top right corner still shows and hides the panel wherever it is. 2) It learns more from you: when it guessed one word and you meant another it remembers that and prefers your word the next time the two look alike; it learns the last two words you wrote, words you used a moment ago, and the words of the pages you visit and bookmark; a word you delete and then glide again no longer comes up first again, it goes to second place; typed noise such as awwww is never learned. Nothing is learned in private tabs or password fields, and Settings > Privacy and security > Forget what the keyboard learned wipes everything. English only. 3) Polish: while you glide, the strip already shows the word it thinks you mean; the key under your finger is shown large above it; hold a top row letter for its number; double tap Shift for caps lock; in a web page's text field the bottom row has comma, apostrophe and question mark; a full stop, comma, slash or .com right after a glided word joins it without a gap; a small vibration on each key (Settings > Key vibration on the mini keyboard, on by default). Menu > Diagnostics > Glide typing now says how fast the word appeared after your finger lifted, how many live guesses were made, and how many corrections and phrases were remembered: please paste it after a few days of use. Tested on the emulator with real touches and a test page; NOT yet tried on your tablet (real thumb, real speed and the vibration could not be tested there)."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha36), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha36 "alpha36 (includes alpha35): 1) The mini keyboard can be resized and moved. Drag one of the small corner marks to make it bigger or smaller (the keys and letters grow with it and glide typing follows the new key positions). Drag the thin bar on top of the panel to move the whole panel anywhere on the screen; let go near the top right corner, or double tap that bar, and it docks back. The size and place are remembered, and Settings has a button Reset the mini keyboard's size and position. Holding the invisible top right corner still shows and hides the panel wherever it is. 2) It learns more from you: when it guessed one word and you meant another it remembers that and prefers your word the next time the two look alike; it learns the last two words you wrote, words you used a moment ago, and the words of the pages you visit and bookmark; a word you delete and then glide again no longer comes up first again, it goes to second place; typed noise such as awwww is never learned. Nothing is learned in private tabs or password fields, and Settings > Privacy and security > Forget what the keyboard learned wipes everything. English only. 3) Polish: while you glide, the strip already shows the word it thinks you mean; the key under your finger is shown large above it; hold a top row letter for its number; double tap Shift for caps lock; in a web page's text field the bottom row has comma, apostrophe and question mark; a full stop, comma, slash or .com right after a glided word joins it without a gap; a small vibration on each key (Settings > Key vibration on the mini keyboard, on by default). Menu > Diagnostics > Glide typing now says how fast the word appeared after your finger lifted, how many live guesses were made, and how many corrections and phrases were remembered: please paste it after a few days of use. Tested on the emulator with real touches and a test page; NOT yet tried on your tablet (real thumb, real speed and the vibration could not be tested there)."
NEVER ship kestrel-g-v0.3.0-alpha36 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d44b251 ckpt 2073: version bump 137 / 0.3.0-alpha36 for the release of AV1-AV3 (resize/detach, l
  eac3369 ckpt 2061: AV3 EMULATOR-PROVEN (page-field row, live guess mid-glide her|he|hd, held dig
  c5c3d98 ckpt 2057: AV2b done: repeated-letter words ('awwww') never learned (PersonalModel.stret
  39b545b ckpt 2047: AV2a research REPORT saved to research/GLIDE_RESEARCH.md (Gboard/Samsung/Swif
  b097785 ckpt 2044: AV3b: caps lock (double tap Shift, accent-filled key), page-field bottom row 
  7defb68 ckpt 2038: AV3a live guess while gliding (GlideTyping.preview on its own thread with its
  06a225c ckpt 2033: AV2/AV3 core: PersonalModel now remembers corrections (guessed -> meant), the
  411b5fb ckpt 2020: AV1 code: MiniNav panel geometry (grip drag detaches/moves with dock snap + d
  2287b4e ckpt 2013: owner message 36 written into TASKS (AV1 resize+detach the mini keyboard, AV2
  d4f48f8 ckpt 2010: alpha35 (kestrel-g-v0.3.0-alpha35, code 136) released and RECORDED: run 43556
```
