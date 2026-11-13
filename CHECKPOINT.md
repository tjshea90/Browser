# CHECKPOINT 1729 — read me first, then TASKS.md

**Written:** 2026-11-13T21:57:58Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `f3217c5` (this checkpoint is the commit after it)

## Just done
alpha32 SHIPPED and recorded (run 43514151508 green, APK 92,539,679 B vs alpha31 93,443,198 B); owner answered in OUTBOX text; EY-22a (canContinue + G18) done; EY-1 in progress: core LastLookRules + LastLookMeta (+7 tests), app LastLook store, RestoreOverlay look mode written, BrowserActivity wiring NOT yet done

## Do this next
EY-1 wiring in geckoapp BrowserActivity: Prefs.lastLook (key last_look default true) + Settings toggle under Clear browsing data + SettingsView.Listener.lastLookOff(); Tab.scrollY + a GeckoSession.ScrollDelegate set in createSession; captureLook(tab, why) on onPause (first line), 700 ms after the last scroll (>= 4 s apart) and 1.5 s after page stop (regular http tabs only, not loading, session attached), saved via LastLook.save on Io.exec then LastLook.retain(LastLookRules.keep); earlyCover(): load meta+bitmap synchronously on the UI thread, LastLookRules.usable(meta, url, Flavor.engineVersion, LastLook.SKIN, prefs.webDark != 0?, tab.desktop) then restoreOverlay.showLook(bitmap, 'Opening host', onDrawn = StartTimeline.mark('look')); hide rule via LastLookRules.hideNow in firstPaint/scroll/page-stop/1500 ms timer, NOTE chip after 4 s (setLookNote), cap 12 s; wipes: coldStartClear, clearBrowsingData, doCloseTab, showNewTabPage, switch off; drop the look when the page URL differs at page start; then tests (core done), emulator proof: open a long page, scroll, swipe away, turn Wi-Fi off, reopen: picture is in the first frame; update research/COLD_START.md

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  8999097 ckpt 1719: gated kestrel-g-v0.3.0-alpha32 (gecko code 131) and pushed it: handoff self-t
  e9da0f9 ckpt 1718: EY-2 (start timeline, emulator marks + Diagnostics line, research/COLD_START.
  caaf9c8 ckpt 1700: AL1-AL6 + AM1 ticked with evidence (AL5 final emulator chain: S1->S2 dropped=
  42dbcff ckpt 1683: AL5 root cause found with a debug logcat mirror of the Health events (new: He
  37e9055 ckpt 1678: AL5: found why a script-named address in a player frame outlived a server swi
  a3bd814 ckpt 1673: report saved as research/REVIEW-EXTERNAL-2.md and its TASKS block (EY-1..EY-3
  d19f54f ckpt 1672: owner message 28 written into TASKS as AN1 (review and complete all fixes and
  d14b0ab ckpt 1665: AL5 emulator lab found a real bug and it is fixed in code: after two server s
  e03327f ckpt 1661: AM1 CODE DONE: removed Tap lab, Decode lab (assets/labs 394 KB), Scroll test 
  bab3a84 ckpt 1650: owner message 27 written into TASKS as AM1 (remove finished diagnosis tools);
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
