# CHECKPOINT 2341 — read me first, then TASKS.md

**Written:** 2026-11-14T20:46:24Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `50505124` (this checkpoint is the commit after it)

## Just done
RX3-BK1/BK2 first emulator proof (static case): page left at y=1800, Back restored it from the engine cache at y=1800 (pageshow persisted=true), the lab classified kind=kept y=1800/1800; bugs found and fixed: page-stop no longer dismisses the back cover (counts as paint, place rule decides), a kept report hides a wrongly-guessed cover, notKept reasons + loc= time in the press line. RX3-B1 DONE (trusted-* only from trusted lists; measured: EasyList/EasyPrivacy/Fanboy hold no trusted line; tests green). RX3-G2 script side DONE: content.js records real taps (trusted click/pointerup, 500 ms / 12 px rule), relay whitelist open/click/overlay/form/redirect, guard-main no longer reports taps; tap.test.js 14 + guard/content/back suites 104 pass

## Do this next
(1) read the other 4 Back lab cases (background run_cases.sh -> scratchpad back-nostore/dynamic/loading/mse.out + cases.done) and record ALL results in research/BACK_SCROLL.md (device, build, date: emulator x86_64 API 29, NOT the tablet; emulator UI stalls seconds so page-stop and pageshow messages are late there); (2) tick RX3-BK1/BK2 with evidence via claim.sh --done only for what is proven, the tablet part stays [~]; (3) RX3-G2 Kotlin: no change needed in the app (tap evidence arrives with the same message); tick G2 with tap.test.js evidence; (4) RX3-G3 (remove the cross-frame postMessage click channel, guard-main.js pressThrough/message listener, tests first in web-tests/guard.test.js), RX3-G1 (static manifest world MAIN registration), then Y1/Y2/Y3/G7

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  8888c78b ckpt 2340: RX3-BK1/BK2 Kotlin wiring DONE and COMPILES (geckoapp + app debug): ShieldsB
  687414dd ckpt 2339: owner message 40 (a screenshot, no words: YouTube Movies and TV page 'Video 
  2d3b6a3a ckpt 2338: RX3-BK1/BK2 core logic DONE and green: core/session/BackScroll.kt (ScrollMem
  13adf44e ckpt 2334: saved research/REVIEW-EXTERNAL-3.md (v2, byte-identical copy of the owner's 
  094aed9b ckpt 2337: alpha40 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha40 (gecko code 141, run
  e2056d0c ckpt 2333: alpha40 (gecko code 141) gated, pushed and the android.yml run DISPATCHED (a
  13e4374a ckpt 2325: gated kestrel-g-v0.3.0-alpha40 (gecko code 141) and pushed it: handoff self-
  917a3327 ckpt 2320: AY2 final: content.js treats only a NEW non-empty selection as text (emulato
  097f61f2 ckpt 2312: AY1, AY3, AY4 ticked with evidence (emulator proof of long press matrix, pic
  ca5c4683 ckpt 2300: AY2+AY4 coded and unit-tested: MiniNav.showAt, PanelLayout long press pick-u
```

(18 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
