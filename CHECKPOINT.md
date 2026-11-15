# CHECKPOINT 2434 — read me first, then TASKS.md

**Written:** 2026-11-15T21:27:35Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `ab5859df` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha52 (gecko code 153) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-b3c2484c-sbew36, inputs {"app":"gecko","full_build":"true","note":"alpha52: full-screen video, third step. Found with the emulator lab (real Firefox engine): when a page goes full screen the engine sizes it to the whole DISPLAY (1024 px high) instead of the space between the system bars (947 px) and keeps that size until the next real size change, so a player's bottom controls were cut off even with the bars kept, and a turn could leave the old height behind (your third screenshot). Now the page tells the app the size it really got, and when that is not the size of the screen area the app gives the engine a real size change (the view one pixel shorter for a moment, at most three times per full screen). Diagnostics has a new line Full-screen page size (how many checks, how many were wrong, how many corrections). Everything else is as alpha51: bars kept by default (Settings, System bars in full-screen video), every video paused during a turn and played again by itself, an extra resize after the turn, Kestrel's video bar in full screen. Tested in headless browsers (453 tests) and unit tests; a clean emulator run of this fix is queued; NOT tried on your tablet yet: please try the Yahoo Finance video in full screen, turn the tablet both ways, and send me the Diagnostics lines Full-screen system bars, Turning the page's own pause, and Full-screen page size."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha52), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha52 "alpha52: full-screen video, third step. Found with the emulator lab (real Firefox engine): when a page goes full screen the engine sizes it to the whole DISPLAY (1024 px high) instead of the space between the system bars (947 px) and keeps that size until the next real size change, so a player's bottom controls were cut off even with the bars kept, and a turn could leave the old height behind (your third screenshot). Now the page tells the app the size it really got, and when that is not the size of the screen area the app gives the engine a real size change (the view one pixel shorter for a moment, at most three times per full screen). Diagnostics has a new line Full-screen page size (how many checks, how many were wrong, how many corrections). Everything else is as alpha51: bars kept by default (Settings, System bars in full-screen video), every video paused during a turn and played again by itself, an extra resize after the turn, Kestrel's video bar in full screen. Tested in headless browsers (453 tests) and unit tests; a clean emulator run of this fix is queued; NOT tried on your tablet yet: please try the Yahoo Finance video in full screen, turn the tablet both ways, and send me the Diagnostics lines Full-screen system bars, Turning the page's own pause, and Full-screen page size."
NEVER ship kestrel-g-v0.3.0-alpha52 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  ab5859df ckpt 2433: pre-ship: alpha52: full-screen video, third step. Found with the emulator la
  80198e75 ckpt 2432: FB4 coded + all green: turn.js vpt report, background relay, ShieldsBridge v
  9780732e ckpt 2431: lab run 43760546128 (fsy, first clean run, alpha51 code) read: Keep mode put
  e9464a7e ckpt 2430: alpha51 gated and queued (FB1 bars kept by default + Settings row, FB2/FB3 t
  38a628d2 ckpt 2429: gated kestrel-g-v0.3.0-alpha51 (gecko code 152) and pushed it: handoff self-
  7c27ad91 ckpt 2428: pre-ship: alpha51: full-screen video, second attempt after alpha50 changed n
  88fb79ef ckpt 2427: FB1 + FB2 + FB3 coded: system bars kept by default (Prefs.fullscreenBars, Se
  691a247e ckpt 2426: owner message 47 written into TASKS.md (alpha50 changed nothing on the table
  d941af5c ckpt 2425: alpha50 shipped and recorded (Release kestrel-g-v0.3.0-alpha50, run 43758859
  4415fbaf ckpt 2424: lab run 43753283814 (fsy) read: it says NOTHING about the full-screen code: 
```
