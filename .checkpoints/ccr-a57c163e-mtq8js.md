# CHECKPOINT 1135 — read me first, then TASKS.md

**Written:** 2026-11-12T19:35:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `0a3f2f0` (this checkpoint is the commit after it)

## Just done
Owner message 15 written into TASKS.md as AB (AB1 one master switch for the phone helper, AB2 video long-press filtering + download sheet with rename, AB3 Fast Wi-Fi optimisation incl. LinkMeter); its first two paragraphs are a re-send of AA1-AA4 (already answered after alpha22); AA3-11 ticked

## Do this next
Order: AB1 (read app/.../download/PhoneBridge.kt toggleHelper, geckoapp BrowserActivity M.PHONE tile, SettingsView Phone helper section; make ONE master state, tests in PageProxyTest/PhoneBridgeTest), then AB2 (read geckoapp/.../web/VideoFlow.kt, core/.../media/MediaList.kt + MediaSniff, content.js vmenu path; scoring in core with tests; sheet UI), then AB3 (add_repo tjshea90/LinkMeter read, docs/FAST_WIFI.md + relay code; NetProfile additions), then ship alpha23 with swipe-to-close/early cover/NetworkMonitor already in the tree. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  b420dda ckpt 1132: alpha22 SHIPPED and recorded (run 43272414589 green incl. the first publish u
  b51c7bc ckpt 1126: alpha22 first run (#69) FAILED in test-jvm: ConnectProxyTest.statsCountTunnel
  df466e6 ckpt 1125: gated kestrel-g-v0.3.0-alpha22 (gecko code 121) and pushed it: handoff self-t
  e283585 ckpt 1123: AA3-8 engine check (Mozilla Maven <latest> = ours 160.0.20261104181552), AA3-
  b39b8fd ckpt 1118: alpha22 shipped to GitHub: ship.sh gates green, pushed; workflow run 43270755
  4f54c04 ckpt 1117: gated kestrel-g-v0.3.0-alpha22 (gecko code 121) and pushed it: handoff self-t
  5c226ea ckpt 1103: AA4: K1 K2 K5 K6 verified on the emulator (evidence in research/SPEED3.md 6.1
  084b5ba ckpt 1101: AA4 speed batch code step: K1 LiveBudget (core, 9 tests) + RAM-adaptive live 
  6fc25a2 ckpt 1077: Reviews saved: research/PERF_KOTLIN.md, research/PERF_JS.md, research/PROMPT_
  7ceb366 ckpt 1075: AA3 audit done by an independent read-only agent: research/PROMPT_AUDIT.md (2
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
