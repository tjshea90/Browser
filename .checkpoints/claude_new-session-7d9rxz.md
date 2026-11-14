# CHECKPOINT 2340 — read me first, then TASKS.md

**Written:** 2026-11-14T20:18:23Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `0de99163` (this checkpoint is the commit after it)

## Just done
RX3-BK1/BK2 Kotlin wiring DONE and COMPILES (geckoapp + app debug): ShieldsBridge bk/bf/ns/pend, Tab.scrollByUrl + Tab.backPlace, BrowserActivity (backLab, BackCover/backCheck hide rule, armBackPicture arms place + 0 ms picture for known-not-kept targets, lookScrolled(tab,y,page) keyed by the report's own address, leavingPage -> backLab.left, firstPaint hook, bridge callbacks, Diagnostics 'Back:' line); core BackLab.pending + MediaList.hasStream with tests (16+2 pass); full web-tests 251/251; AVD kestrel29x64 created (config.ini set per HANDOFF), emulator+android-29 x86_64 image installed

## Do this next
NEXT: (1) write tools/sitelab/backlab.py (ThreadingHTTPServer: /back-static /back-nostore /back-dynamic /back-loading /back-mse /back-next /slow /log; every tall page logs pageshow persisted, nav type, scrollY at +500/+2000 ms to the logfile) and tools/emu/back_emu.py (adb reverse, open URL, swipe, tap link, Back key, read adb logcat -s KestrelHealth lines 'back press kind=' and 'cover-hide'); (2) build ./gradlew :geckoapp:assembleDebug (package app.kestrel.browser.gecko.debug), boot AVD: emulator -avd kestrel29x64 -no-window -no-audio -no-snapshot -no-boot-anim -gpu swiftshader_indirect -no-accel (boot 3-5 min), adb install -r -t; (3) run the 5 cases, record numbers in research/BACK_SCROLL.md (device, build, date); (4) tick RX3-BK1/BK2 with claim.sh --done only for what the emulator proves, the rest stays [~] waiting for the owner's Diagnostics copy; (5) then RX3-B1 (core, pure JVM)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  687414dd ckpt 2339: owner message 40 (a screenshot, no words: YouTube Movies and TV page 'Video 
  2d3b6a3a ckpt 2338: RX3-BK1/BK2 core logic DONE and green: core/session/BackScroll.kt (ScrollMem
  13adf44e ckpt 2334: saved research/REVIEW-EXTERNAL-3.md (v2, byte-identical copy of the owner's 
  094aed9b ckpt 2337: alpha40 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha40 (gecko code 141, run
  e2056d0c ckpt 2333: alpha40 (gecko code 141) gated, pushed and the android.yml run DISPATCHED (a
  13e4374a ckpt 2325: gated kestrel-g-v0.3.0-alpha40 (gecko code 141) and pushed it: handoff self-
  917a3327 ckpt 2320: AY2 final: content.js treats only a NEW non-empty selection as text (emulato
  097f61f2 ckpt 2312: AY1, AY3, AY4 ticked with evidence (emulator proof of long press matrix, pic
  ca5c4683 ckpt 2300: AY2+AY4 coded and unit-tested: MiniNav.showAt, PanelLayout long press pick-u
  bad447a5 ckpt 2287: AY1 coded+guarded; AY2 compiled (content.js contextmenu classifier -> backgr
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
