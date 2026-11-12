# CHECKPOINT 1283 — read me first, then TASKS.md

**Written:** 2026-11-12T23:02:59Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `081deb7` (this checkpoint is the commit after it)

## Just done
EX-1 code done: DownloadService Wi-Fi lock = WIFI_MODE_FULL_HIGH_PERF on every API level (const WIFI_LOCK_MODE pinned by DownloadServiceWifiLockTest 2/2), stale comment fixed, Diagnostics line 'Wi-Fi lock: HIGH_PERF ... held now=..., taken N time(s)'. Emulator proof (dumpsys wifi while a download moves) still to do in the combined debug build

## Do this next
EX-2: BrowserActivity.preconnect() (~line 1147) + GeckoEngine.executorOrNull().speculativeConnect: dispatch GeckoThread.speculativeConnect on the Gecko thread via XPCOMEventTarget.mainThread() (internal org.mozilla.gecko classes, try/catch), plus calls before PROFILE_READY for the search origin; then EX-3 doubleTapZoomingEnabled(false)+Settings, EX-4 image.jxl.enabled=false; build ONE x86_64 debug APK (-PgeckoAbi=x86_64) for the emulator proofs of EX-1..EX-4

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  eb2eddd ckpt 1280: Owner message 16 handled per section 0: research/REVIEW-EXTERNAL-1.md saved (
  884b8ab ckpt 1263: alpha24 SHIPPED and recorded (tag kestrel-g-v0.3.0-alpha24, run 43321730068 g
  a6028f3 ckpt 1257: AA3-3b step 1 (after dispatching alpha24 run 43321730068): engine-image chunk
  efdc3a2 ckpt 1248: alpha24 gated and pushed (ckpt 1247), workflow DISPATCHED: run 43321730068 (#
  3f19341 ckpt 1247: gated kestrel-g-v0.3.0-alpha24 (gecko code 123) and pushed it: handoff self-t
  6b43626 ckpt 1246: alpha24 content finished and tested: keyboard-reveal fix (content.js), D2 laz
  2e3e341 ckpt 1238: AA3-7 Back up/Restore, part 1 done and tested: core BackupCodec (strict untru
  b630671 ckpt 1227: AA3-3 D2 DONE and verified: all ~29,204 generic cosmetic rules now apply (was
  15b71c1 ckpt 1196: AA3-2 typing/keyboard: closed the 'keyboard resizes the page' hypothesis on G
  81f5a25 ckpt 1187: SHIPPED + RECORDED: Kestrel G 0.3.0-alpha23 (code 122), tag kestrel-g-v0.3.0-
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
