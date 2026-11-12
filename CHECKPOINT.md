# CHECKPOINT 1187 — read me first, then TASKS.md

**Written:** 2026-11-12T21:10:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `5c7bc24` (this checkpoint is the commit after it)

## Just done
SHIPPED + RECORDED: Kestrel G 0.3.0-alpha23 (code 122), tag kestrel-g-v0.3.0-alpha23 on 05062dd, run 43306563436 green (run 43305054932 had failed on a flaky BrowserFlowTest, fixed in the test), APK 92871049 bytes sha256 daaa3f8a3a04952c73fe426e38a1741b3ca38ce85c12a53d77c0a025fa57c959, BUILDLOG.md recorded. Contains AB1 (one Phone helper switch), AB2 (Save video sheet), AB3 (Fast Wi-Fi tunnel profile: no IPv6, 1000 KB/s page yield), swipe-to-close, early cover, NetworkMonitor off the UI thread, long-press-with-two-same-tabs fix. Owner message 15 answered in the chat reply (Release link: https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha23). Emulator scratch for the typing probe: scratchpad/site/kb.html + kb2.sh (page logs innerHeight vs visualViewport.height).

## Do this next
AA3-2 typing lag: 1) emulator probe of how the keyboard resizes the page: open http://10.0.2.2:8000/kb.html (scratchpad/site/kb.html, served by mediaserver.py site 8000), make sure NO other adb script runs at once (they clash), tap the field at (200,679), read the log lines from the UI tree (innerH vs vvH); 2) add dom.interactive_widget_default_resizes_visual to GeckoEngine's pref read-back list and test = true via gecko-prefs.txt (config version 6) to see innerH stay constant while vvH shrinks; 3) if it works: ship it as the default plus Settings A/B switch 'Keyboard: resize page / keep page size' (adjustResize vs adjustNothing is the other lever, see BrowserActivity.setUrlTyping ~line 1026); 4) a Diagnostics 'Typing lab' (key-to-paint latency on a local page); then AA3-3 ad-block depth (D2 class/id token index), AA3-7 Back up/Restore, AA3-6 Settings restyle. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN still fires every 2 h.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  05062dd ckpt 1185: alpha23 CI run 43305054932 failed in test-jvm: BrowserFlowTest.newTabPageWarn
  65bbb1c ckpt 1183: gated kestrel-g-v0.3.0-alpha23 (gecko code 122) and pushed it: handoff self-t
  8e8c9bc ckpt 1176: AB3 code done + unit-tested: NetProfile (core) now has isFastTunnel (10.111.2
  3a65bfe ckpt 1151: AB2 verified on the Android 10 emulator: long press opens the new Save video 
  d9db0b5 ckpt 1139: AB1 done: ONE master switch for the phone helper: PhoneBridge.setMaster/maste
  98cbc10 ckpt 1135: Owner message 15 written into TASKS.md as AB (AB1 one master switch for the p
  b420dda ckpt 1132: alpha22 SHIPPED and recorded (run 43272414589 green incl. the first publish u
  b51c7bc ckpt 1126: alpha22 first run (#69) FAILED in test-jvm: ConnectProxyTest.statsCountTunnel
  df466e6 ckpt 1125: gated kestrel-g-v0.3.0-alpha22 (gecko code 121) and pushed it: handoff self-t
  e283585 ckpt 1123: AA3-8 engine check (Mozilla Maven <latest> = ours 160.0.20261104181552), AA3-
```
