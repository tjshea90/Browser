# CHECKPOINT 1288 — read me first, then TASKS.md

**Written:** 2026-11-12T23:06:46Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `e61dae8` (this checkpoint is the commit after it)

## Just done
EX-2/3/4/8/9/11 code done (compile green, unit tests for EX-9; emulator proofs pending): GeckoEngine.speculativeConnect dispatches GeckoThread.speculativeConnect on the Gecko thread (XPCOMEventTarget.mainThread) + Diagnostics 'Warm-ups'; Prefs.doubleTapZoom (default off, tap_zoom) + inputAutoZoom (default on, input_zoom) as builder settings, live in applyPrefs and Settings > Appearance (Gecko only), in the backup allow-list; gecko-prefs.txt image.jxl.enabled=false + config version 6 + read-back names; DownloadController nice 9 while the UI is visible (DownloadThreadPriorityTest); KeyboardProbe.onChange from a global-layout listener + typingTick safety-net cadence once the probe delivered an event + Diagnostics 'Keyboard: N layout event(s)'

## Do this next
Build ONE x86_64 debug APK (./gradlew :geckoapp:assembleDebug -PgeckoAbi=x86_64, background + poll) and prove on the emulator: EX-1 dumpsys wifi shows a HIGH_PERF lock while a download moves; EX-2 a host-side TLS ClientHello logger on port 8443 sees one connection when the top suggestion (seed a bookmark https://10.0.2.2:8443/) appears; EX-3 Tap lab page (viewport width=1200) touchend->click gap before/after; EX-4 Diagnostics read-back image.jxl.enabled=false; EX-11 Diagnostics 'Keyboard: N layout events' > 0 after focusing the address field. Then EX-5/6/7/12/16 as switches (Speed lab), EX-10 click-to-load, EX-13/14/15/18. Scratch scripts: /tmp/claude-0/-home-user-Browser/30a5aa00-41c9-5bd3-87d1-b10052a52d84/scratchpad (emu.sh, kb*.sh show the install/drive pattern)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  8a4f7fd ckpt 1283: EX-1 code done: DownloadService Wi-Fi lock = WIFI_MODE_FULL_HIGH_PERF on ever
  eb2eddd ckpt 1280: Owner message 16 handled per section 0: research/REVIEW-EXTERNAL-1.md saved (
  884b8ab ckpt 1263: alpha24 SHIPPED and recorded (tag kestrel-g-v0.3.0-alpha24, run 43321730068 g
  a6028f3 ckpt 1257: AA3-3b step 1 (after dispatching alpha24 run 43321730068): engine-image chunk
  efdc3a2 ckpt 1248: alpha24 gated and pushed (ckpt 1247), workflow DISPATCHED: run 43321730068 (#
  3f19341 ckpt 1247: gated kestrel-g-v0.3.0-alpha24 (gecko code 123) and pushed it: handoff self-t
  6b43626 ckpt 1246: alpha24 content finished and tested: keyboard-reveal fix (content.js), D2 laz
  2e3e341 ckpt 1238: AA3-7 Back up/Restore, part 1 done and tested: core BackupCodec (strict untru
  b630671 ckpt 1227: AA3-3 D2 DONE and verified: all ~29,204 generic cosmetic rules now apply (was
  15b71c1 ckpt 1196: AA3-2 typing/keyboard: closed the 'keyboard resizes the page' hypothesis on G
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
