# CHECKPOINT 2517 — read me first, then TASKS.md

**Written:** 2026-11-16T15:37:29Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `d16ff8ed` (this checkpoint is the commit after it)

## Just done
owner message 55 (alpha58 on the tablet: Yahoo video black, resumed the wrong/black video, opened full screen, wants the player inside the website) written into TASKS.md as ANS55a-g in his words; his Diagnostics read: native tracks 0 picture(s), decoder none, 0 frames shown, same stream handed over again

## Do this next
reproduce on the real Yahoo page from the sandbox with Playwright (scratchpad pw_yvideo.js is the template: open finance.yahoo.com/quote/TTWO/, record every .m3u8 / Brightcove request with the first lines of its body, press a video, see the master + variant structure and which list WebHandOver.decide picks), then fix: choose only a stream that has a picture + the one of the pressed video, treat no-picture as a failure that gives the film back to the page, add tests (core WebHandOverTest, handover.test.js, lab ho multi-video + audio-only decoy), ship alpha59; then design + build the inline player (ANS55d) in research/ANS55_INLINE_PLAYER.md

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  6f31c2fb ckpt 2516: session handoff saved by the handoff tool (.handoff/handoff.md + json + arch
  3eb91380 ckpt 2515: alpha58 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha58 (code 159, AP
  8dccb965 ckpt 2514: alpha58 RELEASE DISPATCHED AGAIN (the first run 43843245891 made no release:
  596e4e4a ckpt 2513: release run 43843245891 FAILED before any build: GitHub's web-test job faile
  d639fb36 ckpt 2512: alpha58 RELEASE DISPATCHED (ONE time): android.yml run 43843245891 (run 170,
  59ccf63d ckpt 2511: gated kestrel-g-v0.3.0-alpha58 (gecko code 159) and pushed it: handoff self-
  eb003faf ckpt 2510: alpha58 release prepared (NOT shipped yet): geckoapp versionCode 159 / 0.3.0
  a25fa09f ckpt 2509: final vp lab run 43838527885 (head 5567de58 = the independent-review fixes) 
  4430c87e ckpt 2508: final lab vp run dispatched: 43838527885 (head 5567de58 = ckpt 2507, started
  5567de58 ckpt 2507: independent review (9 findings, all real) applied: content.js relays only wh
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
