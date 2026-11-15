# CHECKPOINT 2448 — read me first, then TASKS.md

**Written:** 2026-11-15T23:10:04Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `17392233` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha54 (gecko code 155) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-b3c2484c-sbew36, inputs {"app":"gecko","full_build":"true","note":"alpha54: fixes from an independent check of alpha53 (the new video player work for your message 49 comes in the next release). (1) On pages that change their address without loading again (Yahoo Finance, YouTube) the page's size report was lost, so every turn of the tablet under a full-screen video kept the black cover up for the whole 7 seconds; fixed, and a page that says nothing at all is now let go after 3 seconds. (2) The first manual turn of the tablet after a full screen began used to turn the screen TWICE (a flash of the wrong orientation first, then the right one: two re-layouts for one turn); the hand-back to the tablet's sensor now waits until the tablet has stayed in its position for a moment. (3) A second turn while the first is still going gets its own wait; a touch that ends the black wait no longer counts as a page that gave up, and the cover takes that touch so it cannot hit the page behind it; a turn and back inside one window no longer makes the next keyboard resize look like a turn; no pointless one-pixel size changes on pages whose size is off on both axes; a private tab's page keeps no counters. Everything else is as alpha53. Tested in headless browsers, unit tests and the emulator, NOT on your tablet."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha54), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha54 "alpha54: fixes from an independent check of alpha53 (the new video player work for your message 49 comes in the next release). (1) On pages that change their address without loading again (Yahoo Finance, YouTube) the page's size report was lost, so every turn of the tablet under a full-screen video kept the black cover up for the whole 7 seconds; fixed, and a page that says nothing at all is now let go after 3 seconds. (2) The first manual turn of the tablet after a full screen began used to turn the screen TWICE (a flash of the wrong orientation first, then the right one: two re-layouts for one turn); the hand-back to the tablet's sensor now waits until the tablet has stayed in its position for a moment. (3) A second turn while the first is still going gets its own wait; a touch that ends the black wait no longer counts as a page that gave up, and the cover takes that touch so it cannot hit the page behind it; a turn and back inside one window no longer makes the next keyboard resize look like a turn; no pointless one-pixel size changes on pages whose size is off on both axes; a private tab's page keeps no counters. Everything else is as alpha53. Tested in headless browsers, unit tests and the emulator, NOT on your tablet."
NEVER ship kestrel-g-v0.3.0-alpha54 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  17392233 ckpt 2447: pre-ship: alpha54: fixes from an independent check of alpha53 (the new video
  aa9dfa02 ckpt 2446: owner message 49 (video player audit + overhaul request, with observations o
  4e91027c ckpt 2445: ROT3 coded (not shipped): FullscreenTurner waits 650 ms of the tablet stayin
  f1e253aa ckpt 2444: alpha53 RELEASED and recorded (run 43770059374 green); owner message 48 ANSW
  f4fbfe56 ckpt 2443: alpha53 (ROT2: the cover and pause of a turn wait until the page has the new
  848ef753 ckpt 2442: gated kestrel-g-v0.3.0-alpha53 (gecko code 154) and pushed it: handoff self-
  3b961794 ckpt 2441: pre-ship: alpha53: turning the tablet under a full-screen video, after your 
  9d4609ad ckpt 2440: lab run 43766649065 (fsy with the slow_turn scene) read: ROT2 works on the r
  9cadf845 ckpt 2439: ROT2 complete in the tree and green: all Kotlin suites EXIT 0, npm test 453 
  359fe757 ckpt 2438: ROT2 coded (owner message 48): TurnSettle PAGE_CAP/NUDGE/HINT + settled(expe
```
