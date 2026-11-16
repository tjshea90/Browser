# CHECKPOINT 2524 — read me first, then TASKS.md

**Written:** 2026-11-16T16:27:43Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `17f81789` (this checkpoint is the commit after it)

## Just done
alpha59 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha59 (code 160, run 43905841979 green, APK on the release, BUILDLOG recorded); TASKS ANS55a/b/e + ANS56a ticked; HANDOFF alpha59 bullet; core InlineBox.Track (scroll follow in CSS px x zoom per GeckoView javadoc) + tests green

## Do this next
Owner answer for messages 55+56 is posted in chat (ANS55g/ANS56b: tick after). Building the INLINE player (ANS55f, claimed): (1) gate-main.js factsOf add g=[x,y,w,h,vvScale,vvOffX,vvOffY,vvW,pageX,pageY,fx] (fx=1 for fixed/sticky ancestor or inner scroller), content.js relay (numbers clamped), background.js relay (top = sender.frameId===0), ShieldsBridge->VideoFlow.handOver parse into InlineBox.Page; (2) PlayerFactory extracted from NativePlayerActivity.buildPlayer; (3) PlayerControlsView inline flag (close x + full-screen button, scale for small boxes, Listener.onFullscreen); (4) InlinePlayer view + InlineVideo host (content.addView before home; CompositorScrollDelegate on the tab session; touch drag forwarding to geckoView; stop on Back/pause/tab switch/page start/config/zoom); (5) VideoFlow.start inline branch + repeat guard + replace on a new film; Prefs web_inline + Settings row; (6) tests + lab suite ip; ship alpha60. Do NOT redispatch alpha59.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2e6fccf5 ckpt 2523: alpha59 RELEASE DISPATCHED (ONE time): android.yml run 43905841979 (run 174,
  d0daa8b8 ckpt 2522: gated kestrel-g-v0.3.0-alpha59 (gecko code 160) and pushed it: handoff self-
  c4c4f1df ckpt 2521: pre-ship: alpha59: your message 55 (on alpha58). (1) FIXED: the black pictur
  04ffa9c9 ckpt 2520: lab ho run dispatched: 43903287513 (head 09476513 = ckpt 2519, started 16:03
  09476513 ckpt 2519: owner message 56 written into TASKS.md (ANS56a-b): 'even when I selected a t
  f7ab7a01 ckpt 2518: ANS55e fix coded (core tests + gecko compile green): Yahoo/Brightcove root c
  5851f383 ckpt 2517: owner message 55 (alpha58 on the tablet: Yahoo video black, resumed the wron
  6f31c2fb ckpt 2516: session handoff saved by the handoff tool (.handoff/handoff.md + json + arch
  3eb91380 ckpt 2515: alpha58 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha58 (code 159, AP
  8dccb965 ckpt 2514: alpha58 RELEASE DISPATCHED AGAIN (the first run 43843245891 made no release:
```

(10 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
