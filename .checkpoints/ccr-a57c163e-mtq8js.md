# CHECKPOINT 874 — read me first, then TASKS.md

**Written:** 2026-11-12T06:06:52Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `101ac94` (this checkpoint is the commit after it)

## Just done
alpha18 SHIPPED and recorded (kestrel-g-v0.3.0-alpha18, run 43164931160 green, APK 92779597 bytes, cert verified); owner message 12 answered (Y1 done, Y2 feasible); research/PHONE_OFFLOAD.md written (design, protocol v1, security, stages, tests); Y2a-k added to TASKS

## Do this next
Y2 STAGE 1 build: step Y2b = core/src/main/kotlin/app/kestrel/core/courier/ (Json.kt tiny JSON-lite encode/decode, Sign.kt HMAC-SHA256 request signing + replay window + skew + constant-time compare, Http.kt HTTP/1.1 request parser + response writer with keep-alive and Range 206/416, PeerGuard.kt subnet/loopback check) with JVM tests in core/src/test/kotlin/app/kestrel/core/courier/ (malformed, partial reads, oversized lines, replay, skew); design = research/PHONE_OFFLOAD.md sections 4.1 and 5; then Y2c host logic over Engine/StreamEngine, then Y2d tablet PhoneLink; ckpt after each file. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays (every 4 h). Owner answers to Y2a pending (assume Android 16 phone + OK to install the Courier).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  1569d02 ckpt 869: gated kestrel-g-v0.3.0-alpha18 (gecko code 117) and pushed it: handoff self-te
  9932748 ckpt 868: pre-ship: alpha18: the updater now always tries to install (it no longer stops
  879b10f ckpt 867: Y1 DONE (updater always tries: no storage gate in download()/install(), floor2
  ceaa63c ckpt 860: owner message 12 written into TASKS (Y1 updater must always try, Y2 phone offl
  eadd5fc ckpt 856: post-alpha17 in the tree (NOT shipped): Make MP4 for finished .ts rows (Stream
  0948a0d ckpt 843: post-dispatch hardening (NOT in alpha17): TsToMp4.peekFormatChange (unmarked s
  376dc8d ckpt 837: alpha17 DISPATCHED: android.yml run 43157262742 (run_number 60) on ccr-a57c163
  2726199 ckpt 836: gated kestrel-g-v0.3.0-alpha17 (gecko code 116) and pushed it: handoff self-te
  7f4771d ckpt 835: pre-ship: alpha17: the download button is now an icon in the bottom bar (gray 
  791bba1 ckpt 833: X15 + X18 DONE and emulator-proven (alpha-debug x86_64, Android 10): dock popu
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
