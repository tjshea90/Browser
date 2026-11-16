# CHECKPOINT 2476 — read me first, then TASKS.md

**Written:** 2026-11-16T04:34:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `616a86e9` (this checkpoint is the commit after it)

## Just done
ANS52g.2-4 CODED + TESTED: codec-main.js (web-tests/codec.test.js 15 ok), relay in content.js / background.js (codec-relay.test.js 6 ok, background.test.js 88 ok; RX4-M0b de-flaked), app side: core VideoLimits (+test), DeviceVideo.limits (hardware decoders only, three reference sizes), ShieldsBridge cfg 5th field + videoPolicy supplier, VideoFormats object + Diagnostics line 'Video formats offered to sites', Prefs vid_fps30 + BackupManager spec + Settings rows, OwnerRulesTest guard; full web-tests 495 ok

## Do this next
ANS52g.5: the lab proof. Add a lab page to tools/sitelab/soaklab.py (/video/formats: asks isTypeSupported / canPlayType / mediaCapabilities.decodingInfo for a table of types after 2 s and sends each answer to /report?kind=fmt), a soak.py scene (suite 'cp') that sets the Settings (adb debug intent or prefs) to Automatic / H.264 only / 30 fps and reads report.jsonl + the Diagnostics line; add 'cp' to android.yml + emu-lab.yml lab_suite options; then dispatch lab run, read result, tick ANS52g.5; THEN ANS52h

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  f6c00b7a ckpt 2475: ANS52g designed (research/VIDEO_ACCELERATION.md section 'ANS52g design'), cl
  ec1665a5 ckpt 2474: alpha56 RELEASED and recorded: https://github.com/tjshea90/Browser/releases/
  e4d999c9 ckpt 2473: alpha56 re-dispatched on ckpt 2472 (the re-run of the first attempt failed w
  5bcc109b ckpt 2472: gated kestrel-g-v0.3.0-alpha56 (gecko code 157) and pushed it: handoff self-
  e074b402 ckpt 2471: alpha57 work started while alpha56 builds on GitHub (run 43799695061: test-j
  7a7ad1ae ckpt 2470: gated kestrel-g-v0.3.0-alpha56 (gecko code 157) and pushed it: handoff self-
  4ab1e93b ckpt 2469: alpha56 bundle COMPLETE in code and tests after the worker restart: research
  cd7bcd0e ckpt 2468: independent review of the alpha56 diff (agent): 0 major, 6 minor, all fixed 
  774f1e17 ckpt 2467: ANS52a/a2 CODED + tested: (a) BrowserActivity.holdGeckoView/releaseGeckoHold
  5235b647 ckpt 2466: owner message 52 (rotation still laggy / glitchy, status bar icons stuck in 
```

(32 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
