# CHECKPOINT 976 — read me first, then TASKS.md

**Written:** 2026-11-12T08:11:37Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `f7ca092` (this checkpoint is the commit after it)

## Just done
Y2 stage 1 SHIPPED and recorded: kestrel-courier-v0.1.0-alpha1 (run 43177953278, 152666-byte APK) + kestrel-g-v0.3.0-alpha19 (run 43178702258); owner told with both links + setup steps + honest limits; emulator kill/restart of the Courier proven (sha256 equal); first hello waits 5 s (courier JVM tests green)

## Do this next
Y2 stage 2 (research/PHONE_OFFLOAD.md section 15): start with the JVM-testable core: (1) JsonStreamStore (StreamStore port; read core/.../media/StreamEngine.kt StreamRec + app/.../download/StreamStoreFiles.kt for what persists), (2) CourierHost kind 'stream' (ctor param streams: StreamEngine?, create -> streams.enqueue(StreamSource), wire() from streams.view, actions/delete/janitor/summaries by kind), (3) CourierClient JobSpec stream fields + PhoneBackend.offloadStream + LocalDownloads.enqueueStream fallback, (4) tests with a fake HLS origin (see StreamEngineMp4Test for the fake), (5) :courier wiring (StreamEngine + JsonStreamStore, finisher = null first, Remuxer port later), (6) DownloadController.enqueueStream tries the phone first; then emulator proof with hls.html on the local server, then ship alpha20 + courier alpha2. Wait for the owner's numbers/answers (Y2a, Y2k) in INBOX. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays (delete after 24 h of owner silence or when nothing is open).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M research/PHONE_OFFLOAD.md

## Last ten checkpoints
```
  6dbba44 ckpt 973: gated kestrel-g-v0.3.0-alpha19 (gecko code 118) and pushed it: handoff self-te
  cebfa82 ckpt 969: gated kestrel-courier-v0.1.0-alpha1 (courier code 1) and pushed it: handoff se
  919c2a3 ckpt 968: pre-ship: first Courier release: the small app for your PHONE. It downloads fi
  1df58a2 ckpt 967: pre-ship: first Courier release: the small app for your PHONE. It downloads fi
  354d25f ckpt 960: Y2g (CI plumbing for the third APK) written: appinfo.sh/ship.sh/record-release
  c7820e2 ckpt 957: Y2e BUILT (not yet shipped): module :courier (CourierRuntime/Service/Activity/
  b6b44e0 ckpt 945: Y2d DONE incl. Android glue: PhoneBridge (pairing code, gateway discovery, deb
  46aaf8d ckpt 938: Y2d steps 1+2 DONE (pure core): SocketHttp, LanFetcher, CourierClient, PhoneBa
  b33092b ckpt 930: Y2d step 1 DONE: SocketHttp (cleartext-safe socket HTTP client), LanFetcher (p
  a042f23 ckpt 923: Y2c DONE (phone-side host logic + staging files + JSON store + SSRF guard; fet
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
