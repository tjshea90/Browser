# CHECKPOINT 2339 — read me first, then TASKS.md

**Written:** 2026-11-14T20:14:51Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `a235f497` (this checkpoint is the commit after it)

## Just done
owner message 40 (a screenshot, no words: YouTube Movies and TV page 'Video unavailable ... not available on your mobile browser'): written into TASKS as RX4; PROVED it is YouTube's own answer by loading the same address in plain headless Chromium with three user agents (same words, playabilityStatus UNPLAYABLE; an ordinary video is OK); found that the sandbox CAN reach YouTube (review said it could not); saved tools/sitelab/yt_probe.js and research/YOUTUBE_ADS.md. Also done earlier in this step: content.js BK pageshow/pull/pagehide + web-tests/back.test.js (11 pass), background.js bk/bf/ns/pend relays + 5 tests in background.test.js (pass), content.test.js 45/45 still pass

## Do this next
CONTINUE RX3-BK1/BK2 Kotlin wiring (no Kotlin written yet): (1) ShieldsBridge.kt: new ctor params backPlace:(String)->String, backShown:(String,Boolean,String,Long,Int)->Unit, noStore:(String)->Unit, leftPending:(String,Int)->Unit and when-cases bk/bf/ns/pend beside the scroll case (page report p is a URL; bk answers the place as text or empty). (2) app Tab.kt: val scrollByUrl = ScrollMemory(); @Volatile var backPlace: BackTarget?. (3) BrowserActivity.kt: BackLab instance (skip private tabs), armBackPicture arms Tab.backPlace + backLab.press + 0 ms picture via BackScrollRules.pictureDelayMs(backLab.notKept(url)), BackArm gets savedY, backCover state + backPainted/backCheck/backCheckTick, firstPaint hook, lookScrolled(tab,y,page) writes tab.scrollByUrl and ignores late reports of another page, leavingPage calls backLab.left(url, tab.loading, stream), bridge callbacks, Diagnostics line backLab.stats.line(), Health. Add BackLab.pending(url,n) + test first (pend message marks the page not kept). Then compile: ./gradlew :geckoapp:compileDebugKotlin. Then lab pages tools/sitelab/backlab.py + AVD kestrel29x64 (HANDOFF.md section 3) + research/BACK_SCROLL.md. Full web-test run log: scratchpad webtests-full-1.log (check it)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    ?? research/YOUTUBE_ADS.md
    ?? tools/sitelab/yt_probe.js

## Last ten checkpoints
```
  2d3b6a3a ckpt 2338: RX3-BK1/BK2 core logic DONE and green: core/session/BackScroll.kt (ScrollMem
  13adf44e ckpt 2334: saved research/REVIEW-EXTERNAL-3.md (v2, byte-identical copy of the owner's 
  094aed9b ckpt 2337: alpha40 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha40 (gecko code 141, run
  e2056d0c ckpt 2333: alpha40 (gecko code 141) gated, pushed and the android.yml run DISPATCHED (a
  13e4374a ckpt 2325: gated kestrel-g-v0.3.0-alpha40 (gecko code 141) and pushed it: handoff self-
  917a3327 ckpt 2320: AY2 final: content.js treats only a NEW non-empty selection as text (emulato
  097f61f2 ckpt 2312: AY1, AY3, AY4 ticked with evidence (emulator proof of long press matrix, pic
  ca5c4683 ckpt 2300: AY2+AY4 coded and unit-tested: MiniNav.showAt, PanelLayout long press pick-u
  bad447a5 ckpt 2287: AY1 coded+guarded; AY2 compiled (content.js contextmenu classifier -> backgr
  9a31c5a8 ckpt 2282: AY1 code done (askSlowScript answers CONTINUE with no dialog, Health counter
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
