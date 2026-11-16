# CHECKPOINT 2471 — read me first, then TASKS.md

**Written:** 2026-11-16T03:50:33Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `14d73f8c` (this checkpoint is the commit after it)

## Just done
alpha57 work started while alpha56 builds on GitHub (run 43799695061: test-js failed on an old flaky test RX4-M0b 'a stats message was sent' (passes locally 485/485), failed jobs re-run queued): ANS52i Kestrel's own player CODED and compiling: NativePlayerActivity (Media3 ExoPlayer 1.11.1 on a SurfaceView, hardware decoders first, 15-30 s / 16 MiB buffers, PlayerModel + PlayerControlsView reused, taps/double taps, Back/exit, sound off on pause), NativePlayerStats + Diagnostics line, manifest entry, geckoapp build.gradle media3 deps (needs Guava: fetched online; offline compile of a cold cache fails), Save video sheet gets 'Play in Kestrel's player (test)' (VideoPanel.canPlay/play, VideoFlow.nativeTarget/playNative: HLS master with cap 720 or the row height, plain files; never DASH/audio), lab command nativeplay + nativeurl, soak.py np_scene + suite np in both workflows; guard test kestrelsOwnPlayerIsOfferedOnlyForWhatItCanPlayAndStaysSmall

## Do this next
(1) watch alpha56 run 43799695061 with list_workflow_jobs ONLY (test-js re-run, then build-gecko, then Release), get_release_by_tag kestrel-g-v0.3.0-alpha56, bash tools/record-release.sh kestrel-g-v0.3.0-alpha56 "<note = scratchpad note56.txt, identical to the dispatch>", answer owner messages 51+52 with the Release link on its own line; (2) dispatch the lab suite np (app=lab lab_suite=np) on this commit, read its report (checks: start, decoder, first picture, controls, big button, skip, Back, Diagnostics line) and fix; (3) then ANS52g codec policy script and ANS52h HLS cap; versionCode 158 / 0.3.0-alpha57 only after np is green; delete trigger trig_019ZZh4wE2UHrvMLbwxEFmrV (compat already read); send_later trig_015YaAfScTQXziSPN7zQ5u86 04:20Z; heartbeat trig_01Y8CFHKVb6hmQmEEM78p6gv 04:07Z

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  7a7ad1ae ckpt 2470: gated kestrel-g-v0.3.0-alpha56 (gecko code 157) and pushed it: handoff self-
  4ab1e93b ckpt 2469: alpha56 bundle COMPLETE in code and tests after the worker restart: research
  cd7bcd0e ckpt 2468: independent review of the alpha56 diff (agent): 0 major, 6 minor, all fixed 
  774f1e17 ckpt 2467: ANS52a/a2 CODED + tested: (a) BrowserActivity.holdGeckoView/releaseGeckoHold
  5235b647 ckpt 2466: owner message 52 (rotation still laggy / glitchy, status bar icons stuck in 
  609cad71 ckpt 2465: evidence file corrected (ONE full screen with four control sessions, three w
  82385186 ckpt 2464: ANS51h: no page-look read-back while a page is full screen (the tablet's log
  c7e7d87b ckpt 2463: ANS51b-f ticked in TASKS.md; kp lab scene extended (soak.py kp_scene: after 
  a6585051 ckpt 2462: ANS51b-f CODED + tested: (b) BrowserActivity.filmPicture feeds the film's w/
  dd7a66a8 ckpt 2461: owner message 51 (the real tablet's Diagnostics of alpha55): read in researc
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
