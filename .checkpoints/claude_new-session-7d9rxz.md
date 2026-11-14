# CHECKPOINT 2342 — read me first, then TASKS.md

**Written:** 2026-11-14T20:56:19Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `c142ffbc` (this checkpoint is the commit after it)

## Just done
RX3-G3 DONE (script side): the cross-frame postMessage assist channel is gone; the parent guard only dispatches a parameterless '__kst_assist_v1' event, content.js decides from its own trusted press (fresh 2 s, max 5, frame and place worked out from the press), background relays to the ONE child frame via tabs.sendMessage (frameDoc map + frameParent), the child's content.js plays its video or presses through (never on a/button/input/form...); assist.test.js 9 + guard + background 72 + all related web suites pass. Back lab: 5 emulator cases measured (static kept exact; nostore from network, engine restored 1800; dynamic NOT restored by the engine (0) = the pull case; loading page (hanging img) STILL KEPT by Gecko; mse reloaded from cache). Found app bug: armBackPicture saw a stale session state (index -1 of 1) so nothing was armed -> pull answered empty; fixed by answering from the tab's own thread-safe ScrollMemory (no arming, BackTarget removed). Rebuilt x86_64 APK installed, dynamic round re-running

## Do this next
(1) read scratchpad back-dynamic2.out: expect +500/+2000 ms = 1800 and Health back.pull counted; if the place is still 0 debug content.js pull() on the emulator (the 'bk' message path: content -> background -> ShieldsBridge 'bk'); (2) write research/BACK_SCROLL.md with ALL numbers (table of 5 cases, device emulator x86_64 API 29 NOT the tablet, build alpha41-dev debug, date 2026-11-14, honest limits: emulator stalls make show/pagehide ms meaningless; the page-stop 24 s lag) and the open owner question BK5; (3) claim.sh --done RX3-BK1 and RX3-BK2 only for what is proven, keep tablet part [~]; (4) tick RX3-G2 and RX3-G3 with their web-test evidence; (5) RX3-G1 next (guard on first page: manifest content_scripts world MAIN for guard-main.js and scriptlets-main.js, then test on emulator /fakeplay as first page after force-stop); then Y1/Y2/Y3/G7

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  279f3b45 ckpt 2341: RX3-BK1/BK2 first emulator proof (static case): page left at y=1800, Back re
  8888c78b ckpt 2340: RX3-BK1/BK2 Kotlin wiring DONE and COMPILES (geckoapp + app debug): ShieldsB
  687414dd ckpt 2339: owner message 40 (a screenshot, no words: YouTube Movies and TV page 'Video 
  2d3b6a3a ckpt 2338: RX3-BK1/BK2 core logic DONE and green: core/session/BackScroll.kt (ScrollMem
  13adf44e ckpt 2334: saved research/REVIEW-EXTERNAL-3.md (v2, byte-identical copy of the owner's 
  094aed9b ckpt 2337: alpha40 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha40 (gecko code 141, run
  e2056d0c ckpt 2333: alpha40 (gecko code 141) gated, pushed and the android.yml run DISPATCHED (a
  13e4374a ckpt 2325: gated kestrel-g-v0.3.0-alpha40 (gecko code 141) and pushed it: handoff self-
  917a3327 ckpt 2320: AY2 final: content.js treats only a NEW non-empty selection as text (emulato
  097f61f2 ckpt 2312: AY1, AY3, AY4 ticked with evidence (emulator proof of long press matrix, pic
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
