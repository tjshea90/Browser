# CHECKPOINT 1227 — read me first, then TASKS.md

**Written:** 2026-11-12T22:07:13Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `48d8976` (this checkpoint is the commit after it)

## Just done
AA3-3 D2 DONE and verified: all ~29,204 generic cosmetic rules now apply (was the first 1,200 #id ones): core CosmeticTokens + CosmeticIndex token index (97.8% of the real list keyed, biggest bucket 9, build 52-91 ms, up-front CSS 17.3 KB vs 18.6 KB before), FilterSet.genericCssForTokens/warmCosmetic, ShieldsBridge 'gen' message (cosmetic thread, Health log of the first batches), background.js forwarder, content.js watcher (classes+ids at load + MutationObserver, batches of 300, limits), inject/observer now wait for the root element. Emulator probe d2.html: real-list classes/ids hidden incl. a late-added box, :not(.adsbygoogle) exception respected. Found+fixed 2 older bugs in CosmeticSelectors.toCss (isolated selector fused with the previous one / leading comma dropped a whole rule), mutation-checked. Tests: core +CosmeticTokensTest 20, +CosmeticCssShapeTest 4, +RealCosmeticTokenIndexTest; web-tests 87/87 (+6); app/core suites green. research/ADBLOCK_D2.md written. Not done: procedural cosmetics, scriptlets, $removeparam, redirect=, D3.

## Do this next
1) claim.sh AA3-3 --done is PARTIAL: tick only the D2 part: edit TASKS.md AA3-3 text to say D2 done, remaining = procedural/scriptlets/removeparam/redirect/D3 (add them as a new [ ] line AA3-3b), 2) AA3-7 Back up / Restore (research first: what Settings/bookmarks/history/Shields/tabs data lives where: Prefs, BrowserDb, TabStateStore; design a JSON export to Downloads + import, no accounts/sync), 3) AA3-6 Settings tile rows restyle, 4) when two more items are done: ship alpha24 (versionCode 123 already set, ext manifest 1.0.123): bash ship.sh gecko note, dispatch android.yml, record-release.sh, reply to owner with the Release link; heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  15b71c1 ckpt 1196: AA3-2 typing/keyboard: closed the 'keyboard resizes the page' hypothesis on G
  81f5a25 ckpt 1187: SHIPPED + RECORDED: Kestrel G 0.3.0-alpha23 (code 122), tag kestrel-g-v0.3.0-
  05062dd ckpt 1185: alpha23 CI run 43305054932 failed in test-jvm: BrowserFlowTest.newTabPageWarn
  65bbb1c ckpt 1183: gated kestrel-g-v0.3.0-alpha23 (gecko code 122) and pushed it: handoff self-t
  8e8c9bc ckpt 1176: AB3 code done + unit-tested: NetProfile (core) now has isFastTunnel (10.111.2
  3a65bfe ckpt 1151: AB2 verified on the Android 10 emulator: long press opens the new Save video 
  d9db0b5 ckpt 1139: AB1 done: ONE master switch for the phone helper: PhoneBridge.setMaster/maste
  98cbc10 ckpt 1135: Owner message 15 written into TASKS.md as AB (AB1 one master switch for the p
  b420dda ckpt 1132: alpha22 SHIPPED and recorded (run 43272414589 green incl. the first publish u
  b51c7bc ckpt 1126: alpha22 first run (#69) FAILED in test-jvm: ConnectProxyTest.statsCountTunnel
```

(30 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
