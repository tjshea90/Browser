# CHECKPOINT 2475 — read me first, then TASKS.md

**Written:** 2026-11-16T04:24:01Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `462aade2` (this checkpoint is the commit after it)

## Just done
ANS52g designed (research/VIDEO_ACCELERATION.md section 'ANS52g design'), claimed, sub-rows ANS52g.1-5 written in TASKS; the existing Settings row 'Video formats offered to sites' only sets engine prefs and cannot hide VP9 on this tablet (Gecko forces it with a hardware decoder), so the page script enforces it

## Do this next
ANS52g.2: write geckoapp/src/main/assets/kestrel-shields/codec-main.js (MAIN world, no browser API, policy string '<mode><fps30>|<avc>|<hevc>|<vp9>|<av1>' with codec = '-' or '<long>x<short>@<blocks per s>', config event __kst_codec_cfg_v1 in, report event __kst_codec_rep_v1 out, wraps MediaSource.isTypeSupported, canPlayType, mediaCapabilities.decodingInfo) + manifest entry like guard-main.js + web-tests/codec.test.js; heartbeat trig_01Y8CFHKVb6hmQmEEM78p6gv already updated to the alpha56 state

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  ec1665a5 ckpt 2474: alpha56 RELEASED and recorded: https://github.com/tjshea90/Browser/releases/
  e4d999c9 ckpt 2473: alpha56 re-dispatched on ckpt 2472 (the re-run of the first attempt failed w
  5bcc109b ckpt 2472: gated kestrel-g-v0.3.0-alpha56 (gecko code 157) and pushed it: handoff self-
  e074b402 ckpt 2471: alpha57 work started while alpha56 builds on GitHub (run 43799695061: test-j
  7a7ad1ae ckpt 2470: gated kestrel-g-v0.3.0-alpha56 (gecko code 157) and pushed it: handoff self-
  4ab1e93b ckpt 2469: alpha56 bundle COMPLETE in code and tests after the worker restart: research
  cd7bcd0e ckpt 2468: independent review of the alpha56 diff (agent): 0 major, 6 minor, all fixed 
  774f1e17 ckpt 2467: ANS52a/a2 CODED + tested: (a) BrowserActivity.holdGeckoView/releaseGeckoHold
  5235b647 ckpt 2466: owner message 52 (rotation still laggy / glitchy, status bar icons stuck in 
  609cad71 ckpt 2465: evidence file corrected (ONE full screen with four control sessions, three w
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
