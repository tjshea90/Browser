# CHECKPOINT 986 — read me first, then TASKS.md

**Written:** 2026-11-12T08:26:34Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `bdb6fa9` (this checkpoint is the commit after it)

## Just done
Y2 stage 2 (video streams through the phone) code complete, JVM + build verified: JsonStreamStore, CourierHost kind=stream, CourierClient StreamSpec, PhoneBackend.offloadStream; :courier runtime now has StreamEngine + JsonStreamStore (finisher null, own TS->MP4 writer does H.264+AAC); tablet DownloadController.enqueueStream tries the phone first, LocalDownloads routes stream ids to the stream engine. Courier suite 119 tests green x3 (ServerTest 413 case now uses HttpParser.MAX_BODY+1 since the limit is 2 MB for long HLS playlists), :app 97 tests + lintDebug (app, courier) green, geckoapp compiles

## Do this next
Y2h emulator proof: (1) ./gradlew :courier:assembleDebug :geckoapp:assembleDebug -PgeckoAbi=x86_64 --offline; adb install -r both; (2) site server: python3 scratchpad/mediaserver.py site 8000 (hls.html?src=hls/720/index.m3u8); setprop debug.kestrel.phone 127.0.0.1:8765 + debug.kestrel.courier.noguard 1; pair with the code the Courier shows; (3) open the hls page in Kestrel G, tap the dock download icon, pick the video: expect the Downloads row 'Downloaded by your phone' with live progress, final .mp4 in Downloads with the same bytes as an on-tablet download; then kill Courier mid-video and check it resumes; (4) write results to research/PHONE_OFFLOAD.md section 14, tick Y2h in TASKS.md with that evidence; (5) bump courier 0.1.0-alpha2 vc2 + geckoapp 0.3.0-alpha20 vc119 (+ bash tools/sync-ext-version.sh), bash ship.sh for courier then gecko, dispatch android.yml (app=both), wait, record-release.sh only after green, send the owner the links. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays until the owner has been silent 24 h

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f6958c7 ckpt 976: Y2 stage 1 SHIPPED and recorded: kestrel-courier-v0.1.0-alpha1 (run 4317795327
  6dbba44 ckpt 973: gated kestrel-g-v0.3.0-alpha19 (gecko code 118) and pushed it: handoff self-te
  cebfa82 ckpt 969: gated kestrel-courier-v0.1.0-alpha1 (courier code 1) and pushed it: handoff se
  919c2a3 ckpt 968: pre-ship: first Courier release: the small app for your PHONE. It downloads fi
  1df58a2 ckpt 967: pre-ship: first Courier release: the small app for your PHONE. It downloads fi
  354d25f ckpt 960: Y2g (CI plumbing for the third APK) written: appinfo.sh/ship.sh/record-release
  c7820e2 ckpt 957: Y2e BUILT (not yet shipped): module :courier (CourierRuntime/Service/Activity/
  b6b44e0 ckpt 945: Y2d DONE incl. Android glue: PhoneBridge (pairing code, gateway discovery, deb
  46aaf8d ckpt 938: Y2d steps 1+2 DONE (pure core): SocketHttp, LanFetcher, CourierClient, PhoneBa
  b33092b ckpt 930: Y2d step 1 DONE: SocketHttp (cleartext-safe socket HTTP client), LanFetcher (p
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
