# CHECKPOINT 2532 — read me first, then TASKS.md

**Written:** 2026-11-16T17:20:25Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `79fc54ff` (this checkpoint is the commit after it)

## Just done
ANS57 glue coded + compiles: VideoLevels (music-stream volume, window brightness restored), VideoGestures (engine glue), PlayerControlsView info pill + level bars + sun/speaker icons, NativePlayerActivity + InlinePlayer (holdFirst) + page full screen (BrowserActivity.pageFilmGestureTouch / PlayerController.gestureSeek) wired, Prefs.videoGestures + Settings row + backup spec; independent review of the inline player: 2 major + 5 minor ALL fixed (pageInFront guard, address change stops the film, neon, private logging, ES5 hoist...)

## Do this next
1) OwnerRulesTest guard ownerMessage57 (source reading: engine numbers, hosts wired, setting, restore brightness, tap excludes overlay); 2) full unit suites + web tests; 3) lab: extend ip_scene (hold+drag right = a seek lands later; vertical drag on the right = volume line; left = brightness; double tap closes controls) and kp scene (full-screen player: vertical drags immediate, hold + drag) with tools/emu/glide_emu.py holddrag via a Phone.hold_drag helper; add Health lines gesture-seek / gesture-level (already logged); 4) dispatch lab ip + kp; 5) ship alpha60 (inline + gestures; versionCode 161; docs/handoff/ALPHA60_RELEASE_NOTE.txt has a TESTED_PLACEHOLDER to fill), answer owner 55 + 57.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M research/ANS55_INLINE_PLAYER.md

## Last ten checkpoints
```
  0e7f4377 ckpt 2531: ANS55f: lab suite ip run 2 (43914267812): ALL 21 CHECKS OK on the real engin
  45054263 ckpt 2530: owner message 57 written into TASKS.md (ANS57a-f): double tap on a side must
  736bac20 ckpt 2529: ANS55f lab run 1 (ip, 43911183498): the inline player WORKS on the real engi
  6d700eee ckpt 2528: ANS55f: lab run 1 of suite ip DISPATCHED: run 43911183498 (head 72df6d09, st
  72df6d09 ckpt 2527: ANS55f: inline player coded + unit suites ALL green (core, app, geckoapp; gu
  441aa19c ckpt 2526: ANS55f step 2 coded and COMPILES (:geckoapp:compileDebugKotlin ok): PlayerFa
  16795a42 ckpt 2525: ANS55f step 1 done: gate-main.js geomOf + content.js geo() + background.js g
  50071840 ckpt 2524: alpha59 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha59 (code 160, ru
  2e6fccf5 ckpt 2523: alpha59 RELEASE DISPATCHED (ONE time): android.yml run 43905841979 (run 174,
  d0daa8b8 ckpt 2522: gated kestrel-g-v0.3.0-alpha59 (gecko code 160) and pushed it: handoff self-
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
