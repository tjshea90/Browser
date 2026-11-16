# CHECKPOINT 2531 — read me first, then TASKS.md

**Written:** 2026-11-16T17:14:04Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `44c13a69` (this checkpoint is the commit after it)

## Just done
ANS55f: lab suite ip run 2 (43914267812): ALL 21 CHECKS OK on the real engine (overlay box == page box at rest and after a 103 px scroll; moving picture; drag-scroll; controls; long press sheet; full-screen button; close x; Back; scrolled out; setting off; Diagnostics). ANS57a core done (a double-tap seek hides the controls: ControlsMachine.seekGesture, PlayerModel tap host) + core PlayerGestures/GestureMath (VLC numbers) with 20 unit tests green; research/ANS57_VLC_GESTURES.md written from the real VLC source

## Do this next
ANS57 glue next: (1) geckoapp/web: VideoGestures (Host impl for PlayerGestures: AudioManager STREAM_MUSIC volume, window brightness + restore, haptic, info drawing via PlayerControlsView.showInfo/showLevel with fade), Prefs.videoGestures (default on) + Settings row + BackupManager spec; (2) NativePlayerActivity.onStageTouch -> gestures; (3) InlinePlayer.onTouchEvent -> gestures (holdForVertical=true; keep startPass/long press via longHold); (4) page full screen: BrowserActivity.dispatchTouchEvent when fullscreenOn && player.active; (5) OwnerRulesTest guard, lab: kp scene + ip scene gestures via tools/emu/glide_emu.py holddrag/slide; (6) review agent abc4fba2246ef5679 report still pending; (7) alpha60 = inline + gestures, then ship, answer owner 55 + 57.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  45054263 ckpt 2530: owner message 57 written into TASKS.md (ANS57a-f): double tap on a side must
  736bac20 ckpt 2529: ANS55f lab run 1 (ip, 43911183498): the inline player WORKS on the real engi
  6d700eee ckpt 2528: ANS55f: lab run 1 of suite ip DISPATCHED: run 43911183498 (head 72df6d09, st
  72df6d09 ckpt 2527: ANS55f: inline player coded + unit suites ALL green (core, app, geckoapp; gu
  441aa19c ckpt 2526: ANS55f step 2 coded and COMPILES (:geckoapp:compileDebugKotlin ok): PlayerFa
  16795a42 ckpt 2525: ANS55f step 1 done: gate-main.js geomOf + content.js geo() + background.js g
  50071840 ckpt 2524: alpha59 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha59 (code 160, ru
  2e6fccf5 ckpt 2523: alpha59 RELEASE DISPATCHED (ONE time): android.yml run 43905841979 (run 174,
  d0daa8b8 ckpt 2522: gated kestrel-g-v0.3.0-alpha59 (gecko code 160) and pushed it: handoff self-
  c4c4f1df ckpt 2521: pre-ship: alpha59: your message 55 (on alpha58). (1) FIXED: the black pictur
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
