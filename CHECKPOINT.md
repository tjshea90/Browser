# CHECKPOINT 1316 — read me first, then TASKS.md

**Written:** 2026-11-12T23:30:09Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `e8a33e8` (this checkpoint is the commit after it)

## Just done
EX ladder: EX-10 embeds-wait-for-a-tap coded (Shields flag EMBEDS=128 + Prefs.shieldEmbeds + Shields/Settings rows + cfg flag bit 2 + extension embed.html/js + gate + allow-once handshake; 94 web-tests green; core test; FIXED a latent bug: SiteShieldStore.decode capped masks at 127 and would have dropped a site's Embeds choice), EX-15 trigger 1 (warm front origin on resume), EX-9/EX-11/EX-4 proven or done, TASKS.md rows annotated EX-1..EX-18. Emulator findings so far: Diagnostics shows Wi-Fi lock HIGH_PERF line, Keyboard probe events 6 after a typing session (EX-11 proven), Engine prefs read-back jxl.enabled=false, allow_double_tap_zooming=false; Warm-ups counter 1 after typing a suggested host, but NO connection reached the host listener (even a normal https://10.0.2.2:8443/ navigation: blank page) - EX-2 proof still open

## Do this next
1) EX-2: ex2c.sh with EX2C_URL=http://10.0.2.2:8443/ is running (scratchpad ex2c.log): if plain http connects, HTTPS to a private IP is the problem (try a TLS server with a real handshake, or MOZ_LOG via debug config env); 2) emulator proofs: EX-1 dumpsys wifi while a download moves (slow server port 8001 rate 100000: http://10.0.2.2:8001/big.bin?dl=1), EX-3 Tap lab gap off vs on (set tap_zoom in shared_prefs/kestrel.xml with run-as while the app is stopped), EX-10 (embedtest page with an iframe to http://kestrel-embed-test.invalid/embed/x), 3) run full suites (./gradlew :core:test :app:testDebugUnitTest + web-tests), bump to code 124 / 0.3.0-alpha25 (tools/sync-ext-version.sh), ship.sh gecko, dispatch, record, reply to the owner (EX-17: charger overnight once a week; the privacy trade-offs of the lab switches)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  352efcb ckpt 1306: More of the EX ladder coded (compiles; unit tests green: NetProfile 12, Backu
  edaf04d ckpt 1288: EX-2/3/4/8/9/11 code done (compile green, unit tests for EX-9; emulator proof
  8a4f7fd ckpt 1283: EX-1 code done: DownloadService Wi-Fi lock = WIFI_MODE_FULL_HIGH_PERF on ever
  eb2eddd ckpt 1280: Owner message 16 handled per section 0: research/REVIEW-EXTERNAL-1.md saved (
  884b8ab ckpt 1263: alpha24 SHIPPED and recorded (tag kestrel-g-v0.3.0-alpha24, run 43321730068 g
  a6028f3 ckpt 1257: AA3-3b step 1 (after dispatching alpha24 run 43321730068): engine-image chunk
  efdc3a2 ckpt 1248: alpha24 gated and pushed (ckpt 1247), workflow DISPATCHED: run 43321730068 (#
  3f19341 ckpt 1247: gated kestrel-g-v0.3.0-alpha24 (gecko code 123) and pushed it: handoff self-t
  6b43626 ckpt 1246: alpha24 content finished and tested: keyboard-reveal fix (content.js), D2 laz
  2e3e341 ckpt 1238: AA3-7 Back up/Restore, part 1 done and tested: core BackupCodec (strict untru
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
