# CHECKPOINT 930 — read me first, then TASKS.md

**Written:** 2026-11-12T06:38:43Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `d6a8fee` (this checkpoint is the commit after it)

## Just done
Y2d step 1 DONE: SocketHttp (cleartext-safe socket HTTP client), LanFetcher (private addresses only), CourierClient (hello/proof/skew, signed monotonic requests, create/jobs/action/delete/speedtest/probe, error kinds), RemoteJob; shared test harness CourierHarness.kt (Phone); 100 courier JVM tests green

## Do this next
Y2d step 2: write core/src/main/kotlin/app/kestrel/core/courier/PhoneBackend.kt (tablet-side logical rows: offload(src) -> placeholder id, backend thread polls client.jobs every 1s, maps RemoteJob -> DownloadView with note 'On your phone', COMPLETED -> local.enqueue(Source fetcher=2 LanFetcher url=baseUrl+fileUrl) = 'pull', DONE -> client.delete, fallback to local on unreachable/refused/failed/GONE, pause/resume/cancel pass-through via op queue, ALWAYS mode waits, persistence phone-jobs.json via AtomicFiles, noteFor(engineId)) + PhoneBackendTest with a real tablet Engine (FakeSinks) + Phone harness; then LinkTest (verdict); then step 3 Android glue. Design = research/PHONE_OFFLOAD.md sections 4.3, 7, 8. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M core/src/main/kotlin/app/kestrel/core/courier/CourierClient.kt

## Last ten checkpoints
```
  a042f23 ckpt 923: Y2c DONE (phone-side host logic + staging files + JSON store + SSRF guard; fet
  e084b8b ckpt 892: Y2b DONE: core/courier package (Json, Sign+Pairing+ReplayGuard+AuthThrottle, H
  89585c0 ckpt 874: alpha18 SHIPPED and recorded (kestrel-g-v0.3.0-alpha18, run 43164931160 green,
  1569d02 ckpt 869: gated kestrel-g-v0.3.0-alpha18 (gecko code 117) and pushed it: handoff self-te
  9932748 ckpt 868: pre-ship: alpha18: the updater now always tries to install (it no longer stops
  879b10f ckpt 867: Y1 DONE (updater always tries: no storage gate in download()/install(), floor2
  ceaa63c ckpt 860: owner message 12 written into TASKS (Y1 updater must always try, Y2 phone offl
  eadd5fc ckpt 856: post-alpha17 in the tree (NOT shipped): Make MP4 for finished .ts rows (Stream
  0948a0d ckpt 843: post-dispatch hardening (NOT in alpha17): TsToMp4.peekFormatChange (unmarked s
  376dc8d ckpt 837: alpha17 DISPATCHED: android.yml run 43157262742 (run_number 60) on ccr-a57c163
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
