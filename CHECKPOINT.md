# CHECKPOINT 2533 — read me first, then TASKS.md

**Written:** 2026-11-16T17:25:03Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `b7ed26ea` (this checkpoint is the commit after it)

## Just done
ANS57 coded end to end + lab scene written: gp suite (soak.py gp_scene: A = Kestrel's full-screen player on the 5-minute lab clip /clip/long.mp4, B = film inside the page /ho?v=inlinelong; raw_path/hold_drag real finger paths; volume/brightness drags, dead zone, plain sideways drag does nothing, hold + drag right/left, farther = more, an inch up = finer, double tap puts the controls away), kp scene updated (double tap = two toggles and clear film; page full-screen gestures), OwnerRulesTest ownerMessage57 + review guard green, ELEVEN review fixes in

## Do this next
Dispatch lab gp (android.yml app=lab lab_suite=gp lab_minutes=1) on this head; read emulator-lab-text report (gp section + numbers); fix; then lab vp (all video suites: kp np cp ap ho ip gp in ONE boot) as the release-candidate regression; then bump geckoapp/build.gradle.kts versionCode 161 / alpha60 + tools/sync-ext-version.sh, fill TESTED_PLACEHOLDER in docs/handoff/ALPHA60_RELEASE_NOTE.txt (add the gestures paragraph), bash ship.sh gecko with that note, ONE dispatch (app=gecko full_build=true), verify tag + jobs + release APK, record-release, tick ANS55c/d/f5 + ANS57a-f, answer owner 55 + 57 in plain words (Release link on its own line, not on the tablet, open items).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  33203ec9 ckpt 2532: ANS57 glue coded + compiles: VideoLevels (music-stream volume, window bright
  0e7f4377 ckpt 2531: ANS55f: lab suite ip run 2 (43914267812): ALL 21 CHECKS OK on the real engin
  45054263 ckpt 2530: owner message 57 written into TASKS.md (ANS57a-f): double tap on a side must
  736bac20 ckpt 2529: ANS55f lab run 1 (ip, 43911183498): the inline player WORKS on the real engi
  6d700eee ckpt 2528: ANS55f: lab run 1 of suite ip DISPATCHED: run 43911183498 (head 72df6d09, st
  72df6d09 ckpt 2527: ANS55f: inline player coded + unit suites ALL green (core, app, geckoapp; gu
  441aa19c ckpt 2526: ANS55f step 2 coded and COMPILES (:geckoapp:compileDebugKotlin ok): PlayerFa
  16795a42 ckpt 2525: ANS55f step 1 done: gate-main.js geomOf + content.js geo() + background.js g
  50071840 ckpt 2524: alpha59 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha59 (code 160, ru
  2e6fccf5 ckpt 2523: alpha59 RELEASE DISPATCHED (ONE time): android.yml run 43905841979 (run 174,
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
