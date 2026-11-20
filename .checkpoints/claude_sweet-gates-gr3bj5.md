# CHECKPOINT 2760 — read me first, then TASKS.md

**Written:** 2026-11-20T19:55:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `7810b2fc` (this checkpoint is the commit after it)

## Just done
RVW5: coded + tested in core (media tests 524 of 524 green): NA1 title cleaning drops the site tail for short host labels / 'Video Dailymotion' / 'Wikimedia Commons' / ' on Vimeo' / 'File:' + extension (EpisodeNames, real titles from tools/sitelab/realnames.js); FD1 MediaSniff.isGoogleAdVideo (ted.com dclk_video_ads); FD2 MediaList.markParts + SerialName (abcnews live: numbered .mp4 parts); PB11 brightness window write only when the 1/255 step changes (GestureMath.sameBrightnessStep + VideoLevels, test written, run pending); research/REVIEW_2026-11-23_RVW5.md written; new tools realnames.js reqbench.js. NOT yet run: :app/:geckoapp unit tests, lint, web npm test, PlayerGesturesTest.

## Do this next
RVW5e/f: run the whole suite in the background (./gradlew --offline -q :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest :app:lintDebug; cd web-tests && npm test), then tick RVW5a-d in TASKS.md with evidence, bump geckoapp versionCode 177 / 0.3.0-alpha76, bash tools/sync-ext-version.sh, write docs/handoff/ALPHA76_RELEASE_NOTE.txt, bash ship.sh gecko, ONE dispatch (android.yml app=gecko full_build=true ref claude/sweet-gates-gr3bj5), record-release, answer the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  58b7c173 ckpt 2759: RVW5 written into TASKS.md in the owner's words (new levers: speed, playback
  80ae116d ckpt 2758: RVW4 DONE AND SHIPPED: alpha75 RELEASED AND RECORDED = kestrel-g-v0.3.0-alph
  a0ad95ec ckpt 2757: REL75b: alpha75 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  eebefd5f ckpt 2756: gated kestrel-g-v0.3.0-alpha75 (gecko code 176) and pushed it: handoff self-
  9e99464e ckpt 2755: RVW4: whole JVM suite GREEN (:core:test 1907 tests 0 failures 12 skipped as 
  b1ec2fd4 ckpt 2754: NL1 PROVEN on the real engine: lab run 44787922316 (daily, 62 scenes, 12 min
  ae4e6a11 ckpt 2753: RVW4: NL2 done: realfinder.js (real pages through the real background.js + c
  d637614b ckpt 2752: NL1: licence row (docs/FILTER-LISTS.md) + HANDOFF engine step written; emula
  85af50a4 ckpt 2751: RVW4-NL1 code done and compiling: Gradle prepareWebcompat (AAR from Mozilla 
  934b8dad ckpt 2750: RVW4a: researched. Engine prefs / downloads / finder are saturated (all chea
```

(14 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
