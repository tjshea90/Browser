# CHECKPOINT 2477 — read me first, then TASKS.md

**Written:** 2026-11-16T04:42:07Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `cad7563b` (this checkpoint is the commit after it)

## Just done
ANS52g.5 CODED (not yet run on the emulator): lab page /formats in tools/sitelab/soaklab.py, debug intent vfmt (mode,fps30,tablet|none; DeviceVideo.labLimits override, debug builds only), soak.py suite cp (cp_scene: five cases compared with the engine's own answers, Diagnostics line check, report section), cp added to android.yml + emu-lab.yml lab_suite options; full unit suites core+app+geckoapp green; ANS52h designed (research/VIDEO_ACCELERATION.md), claimed, sub-rows ANS52h.1-4 in TASKS; hlscap.js (pure rewriteMaster) written but not yet tested or wired

## Do this next
ANS52h.3: write web-tests/hlscap.test.js (rewriteMaster: rungs above cap dropped, smallest kept when all above, no RESOLUTION kept, I-frame lines, CRLF kept, media playlist = null, portrait by long side); add hlscap.js to the manifest background scripts BEFORE background.js and to the SRC list in web-tests/background.test.js; in background.js: hlsCap from the 6th cfg field (parts[5]), blocking onHeadersReceived listener (urls *://*/*.m3u8*, types xmlhttprequest/media/other, status 200, no content-encoding, filterResponseData buffer <= 512 KB, TextDecoder fatal, any doubt = original bytes), counters hx in the st message; fix the harness (it keeps only the LAST onHeadersReceived listener: store the blocking one as listeners.hlsHeaders) and add a fake StreamFilter. THEN ANS52h.2 app side (core PictureCap, Prefs video_cap, Settings row, ShieldsBridge 6th field, Diagnostics), THEN lab scene hls in suite cp + ONE lab dispatch (android.yml app=lab lab_suite=cp) for both ANS52g and ANS52h

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  85bc93dc ckpt 2476: ANS52g.2-4 CODED + TESTED: codec-main.js (web-tests/codec.test.js 15 ok), re
  f6c00b7a ckpt 2475: ANS52g designed (research/VIDEO_ACCELERATION.md section 'ANS52g design'), cl
  ec1665a5 ckpt 2474: alpha56 RELEASED and recorded: https://github.com/tjshea90/Browser/releases/
  e4d999c9 ckpt 2473: alpha56 re-dispatched on ckpt 2472 (the re-run of the first attempt failed w
  5bcc109b ckpt 2472: gated kestrel-g-v0.3.0-alpha56 (gecko code 157) and pushed it: handoff self-
  e074b402 ckpt 2471: alpha57 work started while alpha56 builds on GitHub (run 43799695061: test-j
  7a7ad1ae ckpt 2470: gated kestrel-g-v0.3.0-alpha56 (gecko code 157) and pushed it: handoff self-
  4ab1e93b ckpt 2469: alpha56 bundle COMPLETE in code and tests after the worker restart: research
  cd7bcd0e ckpt 2468: independent review of the alpha56 diff (agent): 0 major, 6 minor, all fixed 
  774f1e17 ckpt 2467: ANS52a/a2 CODED + tested: (a) BrowserActivity.holdGeckoView/releaseGeckoHold
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
