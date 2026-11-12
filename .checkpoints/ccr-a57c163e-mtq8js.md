# CHECKPOINT 1280 — read me first, then TASKS.md

**Written:** 2026-11-12T23:01:28Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `0797bf6` (this checkpoint is the commit after it)

## Just done
Owner message 16 handled per section 0: research/REVIEW-EXTERNAL-1.md saved (byte-for-byte copy), section 13 copied into TASKS.md (AC1 + EX-1..EX-18, all [ ]). Also in the tree for alpha25 (unshipped, all green: core 645 tests, app suite, web-tests): $removeparam for top-level navigations (QueryRules, KFS format 3, NavPolicy, privacy-removeparam.txt source, snapshot rebuilt: 2,441 of 2,990 real rules), kestrel-image worker thread (BridgeLane), Settings shows 'address-cleaning rules'. ConnectProxyTest.aBigTransferArrivesByteForByte failed once under parallel load, passed alone and in the re-run (timing test).

## Do this next
Work the EX ladder of research/REVIEW-EXTERNAL-1.md section 1 TOP-DOWN, one row per step, ckpt after each: EX-1 DownloadService.keepAwake -> WIFI_MODE_FULL_HIGH_PERF on every API level (+comment fix + Diagnostics line 'Wi-Fi lock: HIGH_PERF held=<bool>'); then EX-2 (BrowserActivity.preconnect line ~1147: dispatch GeckoThread.speculativeConnect on the Gecko thread via XPCOMEventTarget.mainThread(), plus queue origins before the profile is ready; prove with a local TLS ClientHello logger on the emulator), EX-3 doubleTapZoomingEnabled(false)+Settings switch, EX-4 image.jxl.enabled=false (+tools/check-gecko-prefs.sh), then EX-6.., see TASKS.md. Verify each review claim against the repo first (the repo's own measurement wins). Then ship alpha25 (code 124 / 0.3.0-alpha25, ext 1.0.124 via tools/sync-ext-version.sh) with these fixes + removeparam; AA3-3b rest (:style() rules, procedural, redirect=) stays open. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  884b8ab ckpt 1263: alpha24 SHIPPED and recorded (tag kestrel-g-v0.3.0-alpha24, run 43321730068 g
  a6028f3 ckpt 1257: AA3-3b step 1 (after dispatching alpha24 run 43321730068): engine-image chunk
  efdc3a2 ckpt 1248: alpha24 gated and pushed (ckpt 1247), workflow DISPATCHED: run 43321730068 (#
  3f19341 ckpt 1247: gated kestrel-g-v0.3.0-alpha24 (gecko code 123) and pushed it: handoff self-t
  6b43626 ckpt 1246: alpha24 content finished and tested: keyboard-reveal fix (content.js), D2 laz
  2e3e341 ckpt 1238: AA3-7 Back up/Restore, part 1 done and tested: core BackupCodec (strict untru
  b630671 ckpt 1227: AA3-3 D2 DONE and verified: all ~29,204 generic cosmetic rules now apply (was
  15b71c1 ckpt 1196: AA3-2 typing/keyboard: closed the 'keyboard resizes the page' hypothesis on G
  81f5a25 ckpt 1187: SHIPPED + RECORDED: Kestrel G 0.3.0-alpha23 (code 122), tag kestrel-g-v0.3.0-
  05062dd ckpt 1185: alpha23 CI run 43305054932 failed in test-jvm: BrowserFlowTest.newTabPageWarn
```

(16 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
