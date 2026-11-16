# CHECKPOINT 2511 — read me first, then TASKS.md

**Written:** 2026-11-16T09:41:43Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `515741df` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha58 (gecko code 159) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-b3c2484c-sbew36, inputs {"app":"gecko","full_build":"true","note":"alpha58: your message 54 (on alpha57). (1) WEB VIDEOS PLAY IN KESTREL'S OWN PLAYER, the smooth one behind the download button: when you press play on a video in a web page, Kestrel's own player takes the film over (hardware decoder, upright, its own controls and Quality list) and Back returns you to the page; press play on the page again and it continues where you stopped. It works for a plain video file, for a stream the page asked for (players that load a playlist, as tested on the emulator; I have not tried your sites) and for a film that goes full screen. Players that hide their stream (YouTube style) and DRM films stay in the page, as before. If Kestrel's own player cannot play a film it goes back to the page, which plays it itself for two minutes. Settings > Web videos play in: Kestrel's player whenever it can (default) / only in full screen / the page's own player. Only your own tap can start it: a page cannot open the player by itself. (2) FIXED: full screen no longer turns the tablet sideways by itself. Settings > Turn the screen for full-screen video now starts as Follow the tablet (portrait stays portrait); Automatic is still there. (3) FIXED: the Quality list in Kestrel's own player has an x button. The box you saw freeze was the site's own quality box (the site's code); films that move to Kestrel's player use Kestrel's list instead. TESTED: unit tests, 573 web tests in headless browsers, an independent code review (9 problems found and fixed, one of them a page that could have opened the player without your tap) and about 270 emulator checks on the real Firefox engine (a plain video with the engine's own play button, a play button over the picture, a script player with a stream, a player with nothing to take over, a broken address, pressing again, and the setting). NOT on your tablet: please send the Diagnostics line Web videos in Kestrel's own player, and tell me which video sites still play in the page."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha58), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha58 "alpha58: your message 54 (on alpha57). (1) WEB VIDEOS PLAY IN KESTREL'S OWN PLAYER, the smooth one behind the download button: when you press play on a video in a web page, Kestrel's own player takes the film over (hardware decoder, upright, its own controls and Quality list) and Back returns you to the page; press play on the page again and it continues where you stopped. It works for a plain video file, for a stream the page asked for (players that load a playlist, as tested on the emulator; I have not tried your sites) and for a film that goes full screen. Players that hide their stream (YouTube style) and DRM films stay in the page, as before. If Kestrel's own player cannot play a film it goes back to the page, which plays it itself for two minutes. Settings > Web videos play in: Kestrel's player whenever it can (default) / only in full screen / the page's own player. Only your own tap can start it: a page cannot open the player by itself. (2) FIXED: full screen no longer turns the tablet sideways by itself. Settings > Turn the screen for full-screen video now starts as Follow the tablet (portrait stays portrait); Automatic is still there. (3) FIXED: the Quality list in Kestrel's own player has an x button. The box you saw freeze was the site's own quality box (the site's code); films that move to Kestrel's player use Kestrel's list instead. TESTED: unit tests, 573 web tests in headless browsers, an independent code review (9 problems found and fixed, one of them a page that could have opened the player without your tap) and about 270 emulator checks on the real Firefox engine (a plain video with the engine's own play button, a play button over the picture, a script player with a stream, a player with nothing to take over, a broken address, pressing again, and the setting). NOT on your tablet: please send the Diagnostics line Web videos in Kestrel's own player, and tell me which video sites still play in the page."
NEVER ship kestrel-g-v0.3.0-alpha58 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  eb003faf ckpt 2510: alpha58 release prepared (NOT shipped yet): geckoapp versionCode 159 / 0.3.0
  a25fa09f ckpt 2509: final vp lab run 43838527885 (head 5567de58 = the independent-review fixes) 
  4430c87e ckpt 2508: final lab vp run dispatched: 43838527885 (head 5567de58 = ckpt 2507, started
  5567de58 ckpt 2507: independent review (9 findings, all real) applied: content.js relays only wh
  9ba75b5b ckpt 2506: tap window widened for a slow tablet (core WebHandOver.pressWindowMs 15 s, 3
  51380853 ckpt 2505: ANS54e.9 SECURITY: a page script can fake the gate's question, so every hand
  4ad3a730 ckpt 2504: lab vp run dispatched: 43832408756 (lab job 133180783158, head 6b32808a = ck
  6b32808a ckpt 2503: lab run 43831112200 (ho run 2) read: file now hands over on the real engine 
  57239dd2 ckpt 2502: lab run 43831112200 (suite ho, head bbfc9c3f = ckpt 2501) dispatched; gradle
  bbfc9c3f ckpt 2501: ANS54e: lab run 43826954106 (suite ho) read: overlay/hls/unknown/never ok, f
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
