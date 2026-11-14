# CHECKPOINT 2338 — read me first, then TASKS.md

**Written:** 2026-11-14T20:03:21Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `7db28f82` (this checkpoint is the commit after it)

## Just done
RX3-BK1/BK2 core logic DONE and green: core/session/BackScroll.kt (ScrollMemory LRU 32, BackTarget, BackScrollRules.coverGoes = LastLookRules.hideNow with stopped=false, pictureDelayMs) and BackLab.kt (press classifier kept/cache/network/other, BackStats.line() for Diagnostics, notKept/noStore knowledge); BackScrollTest 11 + BackLabTest 15 pass (checked in the result XML). Baseline :core:test was green on this fresh container (4m26s incl. Gradle setup). Emulator package installed in the scratchpad SDK dir via sdkmanager (no AVD yet)

## Do this next
NEXT for RX3-BK2/BK1, in this order: (1) geckoapp/src/main/assets/kestrel-shields/content.js: in the existing EY-1 scroll block (search 'EY-1: where the page is scrolled') add a pagehide immediate report, a pageshow message {k:'bf',p,ps,nt,ts,y}, and the pull: on load when navigation type is back_forward and pageshow.persisted is false ask runtime.sendMessage({k:'bk',p}) and scrollTo the answer at 0/250/700/1500/3000 ms, stop on touchstart/wheel/keydown; write web-tests/content.test.js cases FIRST (fake sendMessage must answer 'bk'). (2) background.js: relay bk (return the native reply) and bf, note cache-control no-store on main_frame in the existing onHeadersReceived and send {k:'ns',p}. (3) ShieldsBridge.kt: new callbacks backPlace(url):Int, pageShown(...), noStore(url); wire in BrowserActivity (Tab.scrollByUrl = ScrollMemory, tab.backTarget @Volatile, armBackPicture/backCheck/firstPaint/lookScrolled/leavingPage, BackLab instance, Diagnostics line, Health). (4) lab pages tools/sitelab/backlab.py, emulator AVD kestrel29x64 per HANDOFF.md section 3, research/BACK_SCROLL.md

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  13adf44e ckpt 2334: saved research/REVIEW-EXTERNAL-3.md (v2, byte-identical copy of the owner's 
  094aed9b ckpt 2337: alpha40 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha40 (gecko code 141, run
  e2056d0c ckpt 2333: alpha40 (gecko code 141) gated, pushed and the android.yml run DISPATCHED (a
  13e4374a ckpt 2325: gated kestrel-g-v0.3.0-alpha40 (gecko code 141) and pushed it: handoff self-
  917a3327 ckpt 2320: AY2 final: content.js treats only a NEW non-empty selection as text (emulato
  097f61f2 ckpt 2312: AY1, AY3, AY4 ticked with evidence (emulator proof of long press matrix, pic
  ca5c4683 ckpt 2300: AY2+AY4 coded and unit-tested: MiniNav.showAt, PanelLayout long press pick-u
  bad447a5 ckpt 2287: AY1 coded+guarded; AY2 compiled (content.js contextmenu classifier -> backgr
  9a31c5a8 ckpt 2282: AY1 code done (askSlowScript answers CONTINUE with no dialog, Health counter
  1eb7220e ckpt 2270: owner message 39 written into TASKS (AY1 no slow-page prompt, AY2 long press
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
