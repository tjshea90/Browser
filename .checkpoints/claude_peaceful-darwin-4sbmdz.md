# CHECKPOINT 2831 — read me first, then TASKS.md

**Written:** 2026-11-21T19:47:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `c639a3d7` (this checkpoint is the commit after it)

## Just done
PV1: tests (PrivateTabsTest 12 incl. canary leak scans of ring / live journal / crash note / engine crash reports / sends held; source pins), Diagnostics 'Private tabs' line, WebView private process opens the window; core 71 + app diag 83 green before the last line edit

## Do this next
Next: research/PRIVATE_TABS.md (the audit table: ~30 rows: guarded earlier / fixed now / relied on engine / cannot be closed), CLAUDE.md standing rule, TASKS ticks PV1a-c, then full gate (:core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest :app:lintDebug + web-tests), alpha86 (versionCode 187), ship, answer owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M app/src/main/kotlin/app/kestrel/browser/diag/PrivateGuard.kt
     M app/src/test/kotlin/app/kestrel/browser/diag/PrivateTabsTest.kt
     M geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt

## Last ten checkpoints
```
  8abe214c ckpt 2830: PV1 code in place and compiling: core PrivateScrub + PrivateWindow (71 core 
  985f693e ckpt 2829: PV1 written into TASKS.md in the owner's words (private tabs: nothing saved 
  ede36761 ckpt 2828: alpha85 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/
  0f2cd29a ckpt 2827: alpha85 gated (core 1991 incl. FLK1 flake passing alone, app 478, lint 0 err
  b9f6b0b0 ckpt 2826: gated kestrel-g-v0.3.0-alpha85 (gecko code 186) and pushed it: handoff self-
  495ef4db ckpt 2825: pre-ship: alpha85: the live diagnostics now explain WHY a page is slow, whic
  4cf115af ckpt 2824: LD4 gate so far: core 1991 tests (1 failed = FLK1 PhoneBackendTest flake, pa
  a1863e08 ckpt 2823: LD4c done: diag_digest.py LOAD ANALYSIS (per site: loads, load/first paint/f
  0c525795 ckpt 2822: LD4b app side done and compiling: GeckoEngine.networkProfileCode, ShieldsBri
  ca1f0fe2 ckpt 2821: LD4b extension side done: pagetime.js (ES5, top frame, document_start, laten
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
