# CHECKPOINT 2521 — read me first, then TASKS.md

**Written:** 2026-11-16T16:13:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `42bf0477` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha59: your message 55 (on alpha58). (1) FIXED: the black picture on Yahoo. Yahoo gives the browser a main list of the film and, next to it, separate lists for the picture, the sound and the subtitles, all as long as the film; Kestrel's player was handed the sound or the subtitles list, so it showed the time and nothing else. Now it always takes the main list (the one that offers pictures), and if a film ever plays for 7 seconds with no picture, Kestrel's player hands it back to the page, which plays it, with a short message. (2) FIXED: choosing another video resumed the old one where it had stopped: the newest main list the page asked for is the film now, so each video gets its own stream and starts at the beginning. (3) NOT YET: you asked for the player inside the page, with the page around it, instead of full screen. I am building that now (the video will sit over the page's own video box and follow the page when you scroll, with a full-screen button); it comes in the next release. Until then it still opens full screen; Settings > Web videos play in > The page's own player keeps the page's own player. TESTED: unit tests, 573 web tests in headless browsers and 53 emulator checks of the hand-over on the real Firefox engine, including a copy of Yahoo's list layout (a main list with a long address next to short sound and picture lists), a page that moves on to another video (it gets that video's own stream and starts at the beginning) and a stream with sound but no picture. I also looked at the real Yahoo page from the build machine to find the cause. NOT on your tablet: please try the same Yahoo video again and send the Diagnostics line Web videos in Kestrel's own player (it now names the site of the list it took).

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  04ffa9c9 ckpt 2520: lab ho run dispatched: 43903287513 (head 09476513 = ckpt 2519, started 16:03
  09476513 ckpt 2519: owner message 56 written into TASKS.md (ANS56a-b): 'even when I selected a t
  f7ab7a01 ckpt 2518: ANS55e fix coded (core tests + gecko compile green): Yahoo/Brightcove root c
  5851f383 ckpt 2517: owner message 55 (alpha58 on the tablet: Yahoo video black, resumed the wron
  6f31c2fb ckpt 2516: session handoff saved by the handoff tool (.handoff/handoff.md + json + arch
  3eb91380 ckpt 2515: alpha58 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha58 (code 159, AP
  8dccb965 ckpt 2514: alpha58 RELEASE DISPATCHED AGAIN (the first run 43843245891 made no release:
  596e4e4a ckpt 2513: release run 43843245891 FAILED before any build: GitHub's web-test job faile
  d639fb36 ckpt 2512: alpha58 RELEASE DISPATCHED (ONE time): android.yml run 43843245891 (run 170,
  59ccf63d ckpt 2511: gated kestrel-g-v0.3.0-alpha58 (gecko code 159) and pushed it: handoff self-
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
