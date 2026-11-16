# CHECKPOINT 2556 — read me first, then TASKS.md

**Written:** 2026-11-16T20:55:52Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `024ace3c` (this checkpoint is the commit after it)

## Just done
ANS60a+b CODED (lab proof pending): core PlayerGestures has NO long press any more (decide(): the first of steep+5% = level of the side, sideways+1 cm = precise seek wins; the rest 350 ms only shows a hint; middlePasses = the middle strip scrolls the page inside the page; holdFirst = the old rule behind the new Settings switch 'Gestures inside the page need a long press', off by default); GestureHint + GestureMath.hintAt/hintText say 'Slide up or down for brightness' (left) / 'Slide up or down for volume' (right) / 'Slide left or right to seek' (middle); the hint goes the moment the finger moves; VideoGestures.hint(), beginOnce() on seek too, a second finger always ends our touch; InlinePlayer reads the setting per film; Prefs.videoGesturesHold + backup spec + Settings row; PlayerGesturesTest rewritten (24 tests) + OwnerRules guards updated; core tests + geckoapp compile green

## Do this next
1) ANS60c: shared PlayerQuality class (rows / pick / applyRung / capH / chosen / wanted) extracted from NativePlayerActivity (lines 91-99, 230-310) and used by NativePlayerActivity AND InlinePlayer; PlayerControlsView: inline layout puts Quality left of the full-screen button (layoutParts quality/full slots, hitTest and drawing no longer exclude inline); the QualitySheet for the inline player lives in BrowserActivity.content above InlineHost.view (InlineHost owns it, Back closes it first, film end closes it); 2) ANS60d: PlayerFactory tuning (back buffer for instant rewind, bandwidth meter initial estimate for the 5 Mbit/s hotspot, shorter dead-connection timeouts, wake mode network) + new Diagnostics numbers + research/ANS60_PLAYBACK_OPTIMISATION.md; 3) update lab scenes (soak.py gp: plain drags now seek / level; hint text; ip: quality), JVM + web tests, lab gp + ip + np, ship alpha61; heartbeat trig_01WwqLTpgB4vKso9Rn5q9WV1

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6d684f43 ckpt 2555: heartbeat trig_01WwqLTpgB4vKso9Rn5q9WV1 created (cron every 2 h at :45, boun
  00db0efd ckpt 2554: owner message 60 written into TASKS.md (ANS60a-d) in his words: gestures wit
  ce6ca1f7 ckpt 2553: alpha60 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha60 (code 161, ru
  a1b11269 ckpt 2552: RELEASE kestrel-g-v0.3.0-alpha60 (gecko code 161) DISPATCHED ONCE: android.y
  47d2920c ckpt 2551: gated kestrel-g-v0.3.0-alpha60 (gecko code 161) and pushed it: handoff self-
  4f54303f ckpt 2550: pre-ship: alpha60: your messages 55, 57, 58 and 59 (the player inside the we
  2ef63cb0 ckpt 2549: lab vp (all video suites kp np cp ap ho ip gp pv, one boot) dispatched: run 
  cd7443ee ckpt 2548: lab run 43928839921 (gp, head af4a8675): 21/21 checks, 0 issues = ANS57 prov
  11d927ab ckpt 2547: lab run 43927135769 (gp, head 42e933e5): Kestrel's OWN player passes every g
  d6fe07e1 ckpt 2546: heartbeat created: trig_01LyMjhKLgSaFigb7F61XAfe (every 2 h, bound to this s
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
