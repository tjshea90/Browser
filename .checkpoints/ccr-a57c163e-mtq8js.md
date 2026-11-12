# CHECKPOINT 1196 — read me first, then TASKS.md

**Written:** 2026-11-12T21:29:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `50ef4d3` (this checkpoint is the commit after it)

## Just done
AA3-2 typing/keyboard: closed the 'keyboard resizes the page' hypothesis on GeckoView 160 (layout viewport constant, only the visual viewport shrinks; engine default dom.interactive_widget_default_resizes_visual=true) -> no A/B switch built. FOUND AND FIXED a real problem: a field tapped in the lower half stayed hidden behind the keyboard until the first letter (Firefox follows the caret, not the focus). Fix in extension content.js (visualViewport resize -> scrollIntoView center when an editable element is outside the visible area, 120 ms debounce, >60 px shrink only). web-tests 81/81 (+5). Emulator: fieldTop 406 (hidden, visible area 335) -> page scrolled 260 px, field at 146 (visible). research/TYPING.md written (facts, rejected ideas, what data would settle per-key latency). Debug-only A/B: setprop debug.kestrel.iwvisual 0|1. Dev version bumped to 0.3.0-alpha24 (code 123, extension manifest 1.0.123) so the extension update reaches installs.

## Do this next
1) tick AA3-2 in TASKS.md (claim.sh AA3-2 --done with the evidence above), 2) run the full gate once more: ./gradlew :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin and cd web-tests && npm test, 3) next item AA3-3 ad-block depth: D2 class/id token index in geckoapp/src/main/assets/kestrel-shields/engine.js so all 29,204 generic cosmetic rules apply (today only the first 1,200), with web-tests; read research/ADBLOCK_ENGINE.md + D1-DESIGN.md first and measure the cost with web-tests/bench; then AA3-7 Back up / Restore, AA3-6 Settings restyle; alpha24 ships when two or three of these are done (ship.sh gecko, dispatch android.yml, record-release.sh, reply to the owner).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  81f5a25 ckpt 1187: SHIPPED + RECORDED: Kestrel G 0.3.0-alpha23 (code 122), tag kestrel-g-v0.3.0-
  05062dd ckpt 1185: alpha23 CI run 43305054932 failed in test-jvm: BrowserFlowTest.newTabPageWarn
  65bbb1c ckpt 1183: gated kestrel-g-v0.3.0-alpha23 (gecko code 122) and pushed it: handoff self-t
  8e8c9bc ckpt 1176: AB3 code done + unit-tested: NetProfile (core) now has isFastTunnel (10.111.2
  3a65bfe ckpt 1151: AB2 verified on the Android 10 emulator: long press opens the new Save video 
  d9db0b5 ckpt 1139: AB1 done: ONE master switch for the phone helper: PhoneBridge.setMaster/maste
  98cbc10 ckpt 1135: Owner message 15 written into TASKS.md as AB (AB1 one master switch for the p
  b420dda ckpt 1132: alpha22 SHIPPED and recorded (run 43272414589 green incl. the first publish u
  b51c7bc ckpt 1126: alpha22 first run (#69) FAILED in test-jvm: ConnectProxyTest.statsCountTunnel
  df466e6 ckpt 1125: gated kestrel-g-v0.3.0-alpha22 (gecko code 121) and pushed it: handoff self-t
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
