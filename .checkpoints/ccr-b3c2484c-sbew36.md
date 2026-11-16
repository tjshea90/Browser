# CHECKPOINT 2481 — read me first, then TASKS.md

**Written:** 2026-11-16T05:03:20Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `f9a2c24b` (this checkpoint is the commit after it)

## Just done
owner message 53 (04:59Z, on alpha56: 'The video player is much better now'; Kestrel's own player 'very smooth, noticeably smoother than inside the website') written into TASKS as ANS53a-h in the owner's words BEFORE any code: a never autoplay, b no download or buffering before play, c controls instant (zero delay) on tap and on the next tap, d quality setting in the player controls (resolution, bitrate, fps) for as many videos as possible, e download badge lit on Home for a page left (bug), f Kestrel's own player in portrait: stretched, then forced landscape, no rotate (bug), g feedback; the alpha57 lab run 43806840560 (suite cp) and the review agent are still running

## Do this next
1) read lab run 43806840560 and the review agent's findings (cp = codec policy + HLS cap proof); 2) investigate in this order and write each finding to research/ANS53_DESIGN.md as it is found: ANS53e (why the badge stays lit on Home: tab.media / MediaList / the badge code in BrowserActivity or VideoFlow), ANS53f (NativePlayerActivity orientation + aspect: FullscreenTurn.onEnter forces landscape, SurfaceView stretches), ANS53c (core TapMachine waits the double tap window 300 ms: make a tap toggle at once), ANS53a/b (Gecko prefs media.autoplay.default=5 + blocking_policy + preload; a page-world gate that defers MediaSource blob src until the first real press), ANS53d (quality sheet: Media3 track selection in the native player + a Quality button in the page controls that hands over to the native player at that rung); 3) claim each row with bash tools/claim.sh ANS53x before coding; 4) everything ships in alpha57 with the ANS52g/h work

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a75ad8f5 ckpt 2480: docs: HANDOFF alpha57 IN PROGRESS map (ANS52g + ANS52h), TASKS ANS52i.1; lab
  230b3bc4 ckpt 2479: lab run 43806840560 dispatched (android.yml app=lab lab_suite=cp, ref ccr-b3
  1d05d91d ckpt 2478: ANS52h CODED + UNIT-TESTED: hlscap.js (pure rewriteMaster, web-tests/hlscap.
  d0eb6865 ckpt 2477: ANS52g.5 CODED (not yet run on the emulator): lab page /formats in tools/sit
  85bc93dc ckpt 2476: ANS52g.2-4 CODED + TESTED: codec-main.js (web-tests/codec.test.js 15 ok), re
  f6c00b7a ckpt 2475: ANS52g designed (research/VIDEO_ACCELERATION.md section 'ANS52g design'), cl
  ec1665a5 ckpt 2474: alpha56 RELEASED and recorded: https://github.com/tjshea90/Browser/releases/
  e4d999c9 ckpt 2473: alpha56 re-dispatched on ckpt 2472 (the re-run of the first attempt failed w
  5bcc109b ckpt 2472: gated kestrel-g-v0.3.0-alpha56 (gecko code 157) and pushed it: handoff self-
  e074b402 ckpt 2471: alpha57 work started while alpha56 builds on GitHub (run 43799695061: test-j
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
