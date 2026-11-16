# CHECKPOINT 2525 — read me first, then TASKS.md

**Written:** 2026-11-16T16:30:15Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `75320db5` (this checkpoint is the commit after it)

## Just done
ANS55f step 1 done: gate-main.js geomOf + content.js geo() + background.js g/tp relay (11 numbers: x,y,w,h,vvScale,vvOffX,vvOffY,vvW,pageX,pageY,fx); web-tests/handover.test.js 25/25 incl. 3 new geometry tests; core InlineBox.Track tests green

## Do this next
ANS55f step 2: Kotlin. (a) VideoFlow.handOver/start: parse m.optJSONArray("g") + m.optInt("tp") into InlineBox.Page (follows = tp==1 && fx==0); (b) PlayerFactory.create(ctx,url,headerLines) extracted from NativePlayerActivity.buildPlayer (activity uses it); (c) PlayerControlsView inline flag (close x + full-screen button + size scale, Listener.onFullscreen); (d) InlinePlayer view + InlineVideo host in BrowserActivity (content.addView before home, CompositorScrollDelegate, touch forwarding, stop hooks); (e) VideoFlow.start inline branch; Prefs web_inline + Settings row; then core+Robolectric tests, OwnerRulesTest guards, lab suite ip, ship alpha60. Do NOT redispatch alpha59.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  50071840 ckpt 2524: alpha59 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha59 (code 160, ru
  2e6fccf5 ckpt 2523: alpha59 RELEASE DISPATCHED (ONE time): android.yml run 43905841979 (run 174,
  d0daa8b8 ckpt 2522: gated kestrel-g-v0.3.0-alpha59 (gecko code 160) and pushed it: handoff self-
  c4c4f1df ckpt 2521: pre-ship: alpha59: your message 55 (on alpha58). (1) FIXED: the black pictur
  04ffa9c9 ckpt 2520: lab ho run dispatched: 43903287513 (head 09476513 = ckpt 2519, started 16:03
  09476513 ckpt 2519: owner message 56 written into TASKS.md (ANS56a-b): 'even when I selected a t
  f7ab7a01 ckpt 2518: ANS55e fix coded (core tests + gecko compile green): Yahoo/Brightcove root c
  5851f383 ckpt 2517: owner message 55 (alpha58 on the tablet: Yahoo video black, resumed the wron
  6f31c2fb ckpt 2516: session handoff saved by the handoff tool (.handoff/handoff.md + json + arch
  3eb91380 ckpt 2515: alpha58 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha58 (code 159, AP
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
