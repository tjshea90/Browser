# CHECKPOINT 2482 — read me first, then TASKS.md

**Written:** 2026-11-16T05:17:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `5add6e93` (this checkpoint is the commit after it)

## Just done
ANS53 first batch CODED (unit tests green: core player + QualityLabel; geckoapp compiles): ANS53e badge/sheet/menu ignore a page under the Home screen (BrowserActivity.onHomeScreen, refreshDownloadButton, onDownload, VideoFlow.available); ANS53c a tap toggles the controls at once with no fade and no double-tap wait (core TapMachine/ControlsMachine/PlayerModel, tests), a tap is answered on the glass itself (BrowserActivity.dispatchTouchEvent -> PlayerController.appTap, the page's copy matched by the touch time ts and dropped, a tap on a page button put back via vtap pg:1, Diagnostics counts); ANS53f Kestrel's own player follows the tablet (FullscreenTurner MODE_FOLLOW), rotate button in the controls, surface a 1x1 point until the picture shape is known (no stretching), no forced landscape; ANS53d quality list in Kestrel's own player (core QualityLabel, QualitySheet, Media3 track override, remembered Prefs.nativeMaxH)

## Do this next
ANS53a/b: write research/ANS53_DESIGN.md (Gecko facts from the source read: media.autoplay.default 5 = block all; blocking_policy 0 sticky / 1 transient / 2 input depth; the embedder DENY does not stop a document with sticky activation, so set blocking_policy 1; PERMISSION_AUTOPLAY_* now always DENY; preload prefs), then code: GeckoEngine.applyVideo sets media.autoplay.default=5, media.autoplay.blocking_policy=1, media.preload.default=1, media.preload.auto=1; TabDelegates onContentPermissionRequest denies INAUDIBLE too (remove the Settings row videoMutedAutoplay or make it inert); page-world script gate-main.js (world MAIN): preload none + strip autoplay on media elements while closed, defer MediaSource blob src assignment until a trusted press / play() with navigator.userActivation.isActive, content scripts (videobar.js, player.js, assist) dispatch the open event before their own play(); lab pages /autoplay (video autoplay muted + preload variants + an MSE page + a poster) counting the clip bytes the lab server serves before and after a press; then ANS53d page quality button (hand over to the native player at a rung), then Settings texts, OwnerRulesTest guards, lab scenes, ship alpha57

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6052291b ckpt 2481: owner message 53 (04:59Z, on alpha56: 'The video player is much better now';
  a75ad8f5 ckpt 2480: docs: HANDOFF alpha57 IN PROGRESS map (ANS52g + ANS52h), TASKS ANS52i.1; lab
  230b3bc4 ckpt 2479: lab run 43806840560 dispatched (android.yml app=lab lab_suite=cp, ref ccr-b3
  1d05d91d ckpt 2478: ANS52h CODED + UNIT-TESTED: hlscap.js (pure rewriteMaster, web-tests/hlscap.
  d0eb6865 ckpt 2477: ANS52g.5 CODED (not yet run on the emulator): lab page /formats in tools/sit
  85bc93dc ckpt 2476: ANS52g.2-4 CODED + TESTED: codec-main.js (web-tests/codec.test.js 15 ok), re
  f6c00b7a ckpt 2475: ANS52g designed (research/VIDEO_ACCELERATION.md section 'ANS52g design'), cl
  ec1665a5 ckpt 2474: alpha56 RELEASED and recorded: https://github.com/tjshea90/Browser/releases/
  e4d999c9 ckpt 2473: alpha56 re-dispatched on ckpt 2472 (the re-run of the first attempt failed w
  5bcc109b ckpt 2472: gated kestrel-g-v0.3.0-alpha56 (gecko code 157) and pushed it: handoff self-
```

(18 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
