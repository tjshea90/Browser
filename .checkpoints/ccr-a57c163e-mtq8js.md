# CHECKPOINT 756 — read me first, then TASKS.md

**Written:** 2026-11-12T03:30:51Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `3c2f8cc` (this checkpoint is the commit after it)

## Just done
X17 dock download icon VERIFIED on emulator after a real redraw (home+relaunch): brightest px 54,230,242 = accentHi, chamfered neon frame visible; earlier gray screenshots were the emulator's stale frames (dump said 'Download video: 5 found'). X14 content.js contextmenu capture + vmenu plumbing and X16 guard top-host/overlay are written + unit/Playwright tested (64 web tests) but not yet emulator-verified.

## Do this next
On emulator (debug build build-g10 installed, video.html at http://10.0.2.2:8000): 1) tap dock download icon (428-514,862-922) and check the popup rows show name, size, resolution, type; 2) long-press the video -> app 'Download video' menu (X14); 3) spoof.html red fake play link must be blocked with an overlay notice (X16); then tick X14/X16/X17 in TASKS.md with that evidence, then X18/X15 (HLS always mp4: .mp4 name from the start, surface decline reason, Make MP4 for existing .ts, own TS->MP4 writer), then ship alpha17 (versionCode 116) batching X14-X18 + M8-lite. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  51942ec ckpt 744: owner message 11 written into TASKS as X14-X18 in their words (long-press down
  e8ca3f6 ckpt 741: M8-lite: Settings > Downloads (speed limit 2 MB/s..250 KB/s applied at once, d
  06945e0 ckpt 733: Owner told (alpha16 reply: Release link, repeated message 10 answered part by 
  b87f19d ckpt 732: alpha16 SHIPPED + RECORDED (run 43143728373 green; the two earlier runs failed
  dc57dde ckpt 729: stream engine: a dropped job (merged-away sound of a pair, cancelled, cleared)
  9226e37 ckpt 726: TASKS X4/X5/X12 statuses with evidence; HLS auto-resume test; lintRelease clea
  d41caca ckpt 724: M7 content sanity (web page where a file/video was expected -> BAD_CONTENT, HL
  61e1894 ckpt 721: M1b validator-less resume: Sink.readable/pread (Os.pread on both Android sinks
  f17fcb1 ckpt 719: web guard tests fixed (tap reports) + 2 new; CI test-js now gates the Gecko bu
  b1d2872 ckpt 715: Last-Modified splice guard (EngineTest), research/AUDIT-2026-11.md written, do
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
