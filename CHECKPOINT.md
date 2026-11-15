# CHECKPOINT 2442 — read me first, then TASKS.md

**Written:** 2026-11-15T22:34:28Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `3b961794` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha53 (gecko code 154) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-b3c2484c-sbew36, inputs {"app":"gecko","full_build":"true","note":"alpha53: turning the tablet under a full-screen video, after your two screenshots. They showed the page still laid out for the OLD orientation for a few seconds after the turn: the engine of your tablet hands the new size to the page late, and the black cover of alpha51 and alpha52 went away as soon as Kestrel's own window was still, which is before the page had caught up. Now the black cover and the video pause stay until the page itself says it has the new size (at most 7 seconds), a small progress ring shows after about a second, a touch on the screen ends the wait, and if the page has not caught up after 2.5 seconds the engine is given one real size change. A page open in two tabs is no longer mixed up with the other tab. Diagnostics: the line Turning under a video now says how long the page took to have the new size after the last turn (the worst case, how many gave up), and Full-screen page size counts the pages that report their size. Everything else is as alpha52. Tested in headless browsers (453 tests), unit tests and the emulator (its engine is much faster than your tablet's, so it cannot show the long delay), NOT on your tablet: please try the Yahoo Finance video in full screen, turn the tablet both ways, tell me how long the black screen lasts, and send me the Diagnostics lines Turning under a video, Full-screen page size and Full-screen system bars."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha53), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha53 "alpha53: turning the tablet under a full-screen video, after your two screenshots. They showed the page still laid out for the OLD orientation for a few seconds after the turn: the engine of your tablet hands the new size to the page late, and the black cover of alpha51 and alpha52 went away as soon as Kestrel's own window was still, which is before the page had caught up. Now the black cover and the video pause stay until the page itself says it has the new size (at most 7 seconds), a small progress ring shows after about a second, a touch on the screen ends the wait, and if the page has not caught up after 2.5 seconds the engine is given one real size change. A page open in two tabs is no longer mixed up with the other tab. Diagnostics: the line Turning under a video now says how long the page took to have the new size after the last turn (the worst case, how many gave up), and Full-screen page size counts the pages that report their size. Everything else is as alpha52. Tested in headless browsers (453 tests), unit tests and the emulator (its engine is much faster than your tablet's, so it cannot show the long delay), NOT on your tablet: please try the Yahoo Finance video in full screen, turn the tablet both ways, tell me how long the black screen lasts, and send me the Diagnostics lines Turning under a video, Full-screen page size and Full-screen system bars."
NEVER ship kestrel-g-v0.3.0-alpha53 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  3b961794 ckpt 2441: pre-ship: alpha53: turning the tablet under a full-screen video, after your 
  9d4609ad ckpt 2440: lab run 43766649065 (fsy with the slow_turn scene) read: ROT2 works on the r
  9cadf845 ckpt 2439: ROT2 complete in the tree and green: all Kotlin suites EXIT 0, npm test 453 
  359fe757 ckpt 2438: ROT2 coded (owner message 48): TurnSettle PAGE_CAP/NUDGE/HINT + settled(expe
  bd7e42fd ckpt 2437: owner message 48 (two screenshots of a turn: the page keeps the previous ori
  ac2107ef ckpt 2436: alpha52 RELEASED and recorded (run 43763812046 green, APK in the Release, BU
  1e089300 ckpt 2435: alpha52 shipped to GitHub (gates green, pushed): release run 43763812046 (an
  49db2736 ckpt 2434: gated kestrel-g-v0.3.0-alpha52 (gecko code 153) and pushed it: handoff self-
  ab5859df ckpt 2433: pre-ship: alpha52: full-screen video, third step. Found with the emulator la
  80198e75 ckpt 2432: FB4 coded + all green: turn.js vpt report, background relay, ShieldsBridge v
```
