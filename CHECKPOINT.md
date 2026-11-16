# CHECKPOINT 2557 — read me first, then TASKS.md

**Written:** 2026-11-16T21:02:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `af45a06c` (this checkpoint is the commit after it)

## Just done
ANS60c CODED (lab proof pending): quality list in the player inside the page: new web/PlayerQuality (rows / pick / onTracks / begin: the rules moved out of NativePlayerActivity, which now uses it; guards re-pointed), InlinePlayer creates one per film (remembered pick = Prefs.inlineMaxH 'inline_max_h', backup spec, kept apart from the full-screen pick), PlayerControlsView draws + hit-tests the Quality button inside the page left of the full-screen button, InlineHost owns the QualitySheet (added to BrowserActivity.content above the film; Back closes it first; film end closes it; a tap on it is no page tap), health lines 'inline quality-open N row(s)' and 'inline quality-rows ... (screen pixels)' + the shown line now says 'quality at x,y'; core tests (1405) + compile green

## Do this next
1) ANS60d: research/ANS60_PLAYBACK_OPTIMISATION.md + PlayerFactory tuning (setBackBuffer 8-10 s for instant rewind, DefaultBandwidthMeter initial estimate ~3.5 Mbit/s for the 5 Mbit/s hotspot, connect/read timeouts shorter for dead connections, setWakeMode(C.WAKE_MODE_NETWORK), re-check bufferForPlaybackAfterRebufferMs) and new Diagnostics numbers (rebuffer count + stalled ms, start-up ms prepare->ready, selected video bitrate, buffered ahead, bandwidth estimate, dropped frames per played minute) in NativePlayerStats + both players + Diagnostics line; 2) soaklab.py variant 'inlinehls' (video src = /hls/film/master.m3u8) + soak.py ip scene: tap the quality button (quality at x,y of the inline 'shown' line), 'inline quality-open', tap a row (rows at x,y), 'inline quality' pick line, film keeps playing, Back closes the sheet first; gp scene: plain drags now seek / change levels (no hold_drag needed for them), hint text lines (gesture-hint ... 'Slide up or down for brightness'), middle strip scrolls the page inside the page; 3) JVM suites + web tests, lab gp + ip + np, ship alpha61; heartbeat trig_01WwqLTpgB4vKso9Rn5q9WV1

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  756afc69 ckpt 2556: ANS60a+b CODED (lab proof pending): core PlayerGestures has NO long press an
  6d684f43 ckpt 2555: heartbeat trig_01WwqLTpgB4vKso9Rn5q9WV1 created (cron every 2 h at :45, boun
  00db0efd ckpt 2554: owner message 60 written into TASKS.md (ANS60a-d) in his words: gestures wit
  ce6ca1f7 ckpt 2553: alpha60 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha60 (code 161, ru
  a1b11269 ckpt 2552: RELEASE kestrel-g-v0.3.0-alpha60 (gecko code 161) DISPATCHED ONCE: android.y
  47d2920c ckpt 2551: gated kestrel-g-v0.3.0-alpha60 (gecko code 161) and pushed it: handoff self-
  4f54303f ckpt 2550: pre-ship: alpha60: your messages 55, 57, 58 and 59 (the player inside the we
  2ef63cb0 ckpt 2549: lab vp (all video suites kp np cp ap ho ip gp pv, one boot) dispatched: run 
  cd7443ee ckpt 2548: lab run 43928839921 (gp, head af4a8675): 21/21 checks, 0 issues = ANS57 prov
  11d927ab ckpt 2547: lab run 43927135769 (gp, head 42e933e5): Kestrel's OWN player passes every g
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
