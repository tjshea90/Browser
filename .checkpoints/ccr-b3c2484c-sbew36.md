# CHECKPOINT 2414 — read me first, then TASKS.md

**Written:** 2026-11-15T19:10:25Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `52e323b9` (this checkpoint is the commit after it)

## Just done
code for owner message 46 + second review: FSB1 (applyImmersive without LAYOUT flags, insets follow real bars, re-hide at focus/turn/700 ms, bars note in Diagnostics, debug nohide), ROT1 (core TurnSettle + TurnSettler: MediaSession pause, black cover, resume rules; GeckoTabData.mediaSession), SND1 safety net (silenceStrays; Home over a playing page pauses it), RW1-RW12 in vfind/videobar/content/background JS with 11 new tests (videobar.test.js 25/25, npm test 443 pass), Kotlin suites core+app+geckoapp green before the last small edits, guard tests green; lab: page /fsbox, scene fullscreen_bars (suite fsy, hidden vs kept bars, strip pixel check, rotation, Kestrel bar in a full-screen web component), snd verdict now uses the page clock (run 43745675957 showed sound DOES stop when the app is left)

## Do this next
NEXT: (1) bump versionCode 150 / versionName alpha49 (sync-ext-version.sh), write the alpha49 release note, bash ship.sh gecko note, dispatch android.yml app=gecko full_build=true; (2) dispatch the lab suite fsy (app=lab lab_suite=fsy lab_minutes=10) - it queues behind compat run 43747605584 (ends about 20:10Z); read its report: strip rows kept vs hidden, turn settled lines, Kestrel bar in full screen; (3) read compat run 43747605584 into research/COMPAT_RUN1.md; (4) answer owner messages 44-46 in plain words (ANS44/ANS45/ANS46)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  7f2c4c92 ckpt 2413: owner message 46 written into TASKS.md (FSB1 bars cover the controls in full
  b8c5d333 ckpt 2412: hour soak 43740398387 read (commit 281c48f4, hotspot-shaped, 70 min, 79 scen
  9edf1ab7 ckpt 2411: LAB2 harness written: lab-only page-health probe (vfind.js probe(), guard an
  4a9a28f4 ckpt 2410: SND1a lab scene written: lab page /video/audible?kind=video|audio (big butto
  44f9516c ckpt 2409: wrote owner message 45 into TASKS.md in the owner's words: AU1 (sound must n
  d5053cd6 ckpt 2408: owner answered in plain words for messages 43 and 44 (alpha48 Release link g
  055248fc ckpt 2407: alpha48 RELEASED: tag kestrel-g-v0.3.0-alpha48 (commit 281c48f4, APK 94.7 MB
  96a8ccce ckpt 2406: TASKS: VC1b/VC1c/LP1b/LP1c ticked with the lab evidence (run 43739450975); r
  fbbd7efc ckpt 2405: lab vc run 3 (43739450975, commit ecbdecc3) all green on the REAL engine inc
  2211e3fd ckpt 2404: gated kestrel-g-v0.3.0-alpha48 (gecko code 149) and pushed it: handoff self-
```

(34 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
