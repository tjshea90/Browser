# CHECKPOINT 892 — read me first, then TASKS.md

**Written:** 2026-11-12T06:16:40Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `a5fc4a1` (this checkpoint is the commit after it)

## Just done
Y2b DONE: core/courier package (Json, Sign+Pairing+ReplayGuard+AuthThrottle, Http parser/writer/Range, PeerGuard, CourierServer) with 58 JVM tests green (HttpTest 18, JsonTest 9, PeerGuardTest 6, ServerTest 13 incl. real loopback server, SignTest 12); Y2b ticked in TASKS

## Do this next
Y2c: read Engine public API (core/.../download/Engine.kt), app FileSinks.kt/UrlConnectionFetcher.kt/SqliteStore.kt/StreamStoreFiles.kt and DownloadController.init to decide what is pure-JVM and movable into :core; then write core/.../courier/ JvmFileSinks (SinkFactory), JsonRecStore (Store), host logic CourierHost (routes /v1/hello, /v1/jobs, /v1/jobs/<id>/<action>, /v1/f/<token>/<name>, /v1/speedtest, /v1/probe; HMAC auth; cid idempotency; SSRF guard; janitor) + CourierHostTest over loopback with FakeServer-style origin; design in research/PHONE_OFFLOAD.md sections 4.1, 5, 6.2; ckpt after each file

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  89585c0 ckpt 874: alpha18 SHIPPED and recorded (kestrel-g-v0.3.0-alpha18, run 43164931160 green,
  1569d02 ckpt 869: gated kestrel-g-v0.3.0-alpha18 (gecko code 117) and pushed it: handoff self-te
  9932748 ckpt 868: pre-ship: alpha18: the updater now always tries to install (it no longer stops
  879b10f ckpt 867: Y1 DONE (updater always tries: no storage gate in download()/install(), floor2
  ceaa63c ckpt 860: owner message 12 written into TASKS (Y1 updater must always try, Y2 phone offl
  eadd5fc ckpt 856: post-alpha17 in the tree (NOT shipped): Make MP4 for finished .ts rows (Stream
  0948a0d ckpt 843: post-dispatch hardening (NOT in alpha17): TsToMp4.peekFormatChange (unmarked s
  376dc8d ckpt 837: alpha17 DISPATCHED: android.yml run 43157262742 (run_number 60) on ccr-a57c163
  2726199 ckpt 836: gated kestrel-g-v0.3.0-alpha17 (gecko code 116) and pushed it: handoff self-te
  7f4771d ckpt 835: pre-ship: alpha17: the download button is now an icon in the bottom bar (gray 
```

(17 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
