# CHECKPOINT 1306 — read me first, then TASKS.md

**Written:** 2026-11-12T23:19:37Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `6134384` (this checkpoint is the commit after it)

## Just done
More of the EX ladder coded (compiles; unit tests green: NetProfile 12, BackupManager 10, Engine 41): EX-5 NetProfile.keepHelperUnderVpn + Network lab switch; EX-6/12/16 lab prefs (Prefs lab_*, GeckoEngine.applyLab, Settings rows in the Network lab section); EX-7 WifiBoost (net/WifiBoost.kt, held while the browser is on screen, onResume/onPause/prefsChanged); EX-13 Engine.windowGaps + Diagnostics line (test); EX-14/EX-3 Decode lab + Tap lab pages (assets/labs/*, Diagnostics buttons, checked in headless Chromium); EX-18 flushSessionState in onPause + stateFlushArrived, doc corrections (GECKO_SPEED, GECKO_TUNING_2, PERF_JS J11, setPriorityHint decision); preconnect warms the suggestion's own port. Emulator proof of EX-2: first run printed NO connection for the new path (investigating: private address 10.0.2.2 / LNA / dispatch)

## Do this next
1) Finish the EX-2 emulator proof: wait for ex2.log (scratchpad ex2.sh: host listener tlslog.py on 8443, bookmark https://10.0.2.2:8443/ seeded); if both paths show nothing, sanity-check that a normal navigation to https://10.0.2.2:8443/ reaches the listener, then try Gecko logging (MOZ_LOG) or another target; 2) reinstall the latest debug APK (labs) and prove EX-1 (dumpsys wifi lock while a download moves), EX-3 (Tap lab gap with adb input tap, default off vs switch on), EX-11 (Keyboard events counter after focusing the address field), EX-14 Decode lab numbers; 3) remaining ladder: EX-10 click-to-load embeds, EX-15 warm triggers (after EX-2 works), EX-17 tell owner, then run full suites, bump to code 124/alpha25, ship

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  edaf04d ckpt 1288: EX-2/3/4/8/9/11 code done (compile green, unit tests for EX-9; emulator proof
  8a4f7fd ckpt 1283: EX-1 code done: DownloadService Wi-Fi lock = WIFI_MODE_FULL_HIGH_PERF on ever
  eb2eddd ckpt 1280: Owner message 16 handled per section 0: research/REVIEW-EXTERNAL-1.md saved (
  884b8ab ckpt 1263: alpha24 SHIPPED and recorded (tag kestrel-g-v0.3.0-alpha24, run 43321730068 g
  a6028f3 ckpt 1257: AA3-3b step 1 (after dispatching alpha24 run 43321730068): engine-image chunk
  efdc3a2 ckpt 1248: alpha24 gated and pushed (ckpt 1247), workflow DISPATCHED: run 43321730068 (#
  3f19341 ckpt 1247: gated kestrel-g-v0.3.0-alpha24 (gecko code 123) and pushed it: handoff self-t
  6b43626 ckpt 1246: alpha24 content finished and tested: keyboard-reveal fix (content.js), D2 laz
  2e3e341 ckpt 1238: AA3-7 Back up/Restore, part 1 done and tested: core BackupCodec (strict untru
  b630671 ckpt 1227: AA3-3 D2 DONE and verified: all ~29,204 generic cosmetic rules now apply (was
```

(17 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
