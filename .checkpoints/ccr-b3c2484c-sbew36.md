# CHECKPOINT 2558 — read me first, then TASKS.md

**Written:** 2026-11-16T21:07:08Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `007527b2` (this checkpoint is the commit after it)

## Just done
ANS60d CODED (the emulator cannot show speed, only that nothing broke): PlayerFactory = 10 s back buffer from the keyframe (instant rewind), ONE shared DefaultBandwidthMeter with a 4 Mbit/s first guess for the 5 Mbit/s hotspot, connect/read timeouts 10 s, WAKE_MODE_NETWORK, PlayerFactory.warm() (Android codec list asked once in the idle time, 20 s after the browser is on screen); new PlaybackMeter (both players): one 'stats' Health line per film (playable after, stalls + waits, link estimate, picture bitrate, size changes, dropped, buffered ahead) and the sums in NativePlayerStats / the Kestrel own player Diagnostics line; research/ANS60_PLAYBACK_OPTIMISATION.md (hardware facts, what changed, what was considered and rejected, what cannot be measured here, sources); compile + guards green

## Do this next
1) lab scenes in tools/emu/soak.py + tools/sitelab/soaklab.py: soaklab variant 'inlinehls' (video src = /hls/film/master.m3u8, two renditions); ip scene: Q block = tap the Quality button (quality at x,y in the inline 'shown' line), 'inline quality-open', rows at x,y ('inline quality-rows'), tap row 2, 'inline quality <row>' pick line, the sheet is gone and the film still plays, Back with the sheet open closes only the sheet; check the 'stats' line exists after the film ends (playable after N ms); gp scene: plain drags now seek / change levels without hold_drag (input swipe is enough), hint text lines ('gesture-hint' with 'Slide up or down for brightness' on the left, volume on the right, 'Slide left or right to seek' in the middle) using a still finger (hold_drag with 0 distance = rest), inside the page: the middle strip scrolls the page, the side drags change levels, and with the setting 'video_gestures_hold' on the old rule (debug command to flip it); np scene unchanged (quality list refactor: must stay green); 2) run JVM suites + web tests; lab gp + ip + np (or vp); 3) release note docs/handoff/ALPHA61_RELEASE_NOTE.txt, versionCode 162 / 0.3.0-alpha61, ship, answer owner 60; heartbeat trig_01WwqLTpgB4vKso9Rn5q9WV1

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  c8fca4df ckpt 2557: ANS60c CODED (lab proof pending): quality list in the player inside the page
  756afc69 ckpt 2556: ANS60a+b CODED (lab proof pending): core PlayerGestures has NO long press an
  6d684f43 ckpt 2555: heartbeat trig_01WwqLTpgB4vKso9Rn5q9WV1 created (cron every 2 h at :45, boun
  00db0efd ckpt 2554: owner message 60 written into TASKS.md (ANS60a-d) in his words: gestures wit
  ce6ca1f7 ckpt 2553: alpha60 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha60 (code 161, ru
  a1b11269 ckpt 2552: RELEASE kestrel-g-v0.3.0-alpha60 (gecko code 161) DISPATCHED ONCE: android.y
  47d2920c ckpt 2551: gated kestrel-g-v0.3.0-alpha60 (gecko code 161) and pushed it: handoff self-
  4f54303f ckpt 2550: pre-ship: alpha60: your messages 55, 57, 58 and 59 (the player inside the we
  2ef63cb0 ckpt 2549: lab vp (all video suites kp np cp ap ho ip gp pv, one boot) dispatched: run 
  cd7443ee ckpt 2548: lab run 43928839921 (gp, head af4a8675): 21/21 checks, 0 issues = ANS57 prov
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
