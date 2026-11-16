# CHECKPOINT 2478 — read me first, then TASKS.md

**Written:** 2026-11-16T04:48:01Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `8a378da2` (this checkpoint is the commit after it)

## Just done
ANS52h CODED + UNIT-TESTED: hlscap.js (pure rewriteMaster, web-tests/hlscap.test.js 8 ok), background.js blocking onHeadersReceived listener (narrow .m3u8 urls, filterResponseData, any doubt = original bytes, counters hx), 6th cfg field (core PictureCap + test, Prefs video_cap, BackupManager spec, Settings row 'Picture size from sites', ShieldsBridge pictureCap supplier, VideoFormats.cap / capLine, Diagnostics line, lab intent vcap), background.test.js 95 ok with a fake StreamFilter, OwnerRulesTest guards for ANS52g and ANS52h; lab: /hlsfetch + /hls/*.m3u8 (plain, gzip, media, small) in soaklab.py and soak.py hls_scene inside suite cp

## Do this next
Verify everything once more (full web-tests run is going in the background: scratchpad webtests-all2.log must end with EXIT 0 and no 'not ok'; then the full gradle suites ./gradlew --offline -q :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest and :app:lintDebug), then dispatch ONE lab run: mcp__github__actions_run_trigger owner tjshea90 repo Browser workflow android.yml ref ccr-b3c2484c-sbew36 inputs app=lab lab_suite=cp lab_minutes=10 (read its result with actions_get / fetch_artifact.py into the scratchpad; research/COMPAT_RUN1.md shows how); fix what the real engine says (first: do the page answers carry cx, does the gzip master list get cut or passed on), tick ANS52g.5 / ANS52h.2-4, then ship.sh gecko alpha57 + ONE fresh dispatch

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d0eb6865 ckpt 2477: ANS52g.5 CODED (not yet run on the emulator): lab page /formats in tools/sit
  85bc93dc ckpt 2476: ANS52g.2-4 CODED + TESTED: codec-main.js (web-tests/codec.test.js 15 ok), re
  f6c00b7a ckpt 2475: ANS52g designed (research/VIDEO_ACCELERATION.md section 'ANS52g design'), cl
  ec1665a5 ckpt 2474: alpha56 RELEASED and recorded: https://github.com/tjshea90/Browser/releases/
  e4d999c9 ckpt 2473: alpha56 re-dispatched on ckpt 2472 (the re-run of the first attempt failed w
  5bcc109b ckpt 2472: gated kestrel-g-v0.3.0-alpha56 (gecko code 157) and pushed it: handoff self-
  e074b402 ckpt 2471: alpha57 work started while alpha56 builds on GitHub (run 43799695061: test-j
  7a7ad1ae ckpt 2470: gated kestrel-g-v0.3.0-alpha56 (gecko code 157) and pushed it: handoff self-
  4ab1e93b ckpt 2469: alpha56 bundle COMPLETE in code and tests after the worker restart: research
  cd7bcd0e ckpt 2468: independent review of the alpha56 diff (agent): 0 major, 6 minor, all fixed 
```

(12 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
