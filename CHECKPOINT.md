# CHECKPOINT 938 — read me first, then TASKS.md

**Written:** 2026-11-12T06:53:12Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `20dee78` (this checkpoint is the commit after it)

## Just done
Y2d steps 1+2 DONE (pure core): SocketHttp, LanFetcher, CourierClient, PhoneBackend (offload, mirror, pull, fallback, ALWAYS mode, restart-safe state file), LinkTest verdicts; CourierHost now remembers canceled jobs (engine forgets them at once); 111 courier JVM tests green 6 runs in a row

## Do this next
Y2d step 3 (Android glue in :app shared tree): (a) read DownloadController.init/allViews/enqueue, Notifier, DownloadsScreen, SettingsScreen, Diagnostics to see id usage (Long->Int casts?), streams firstId, how settings rows are built; (b) PhonePrefs + gateway discovery (LinkProperties default route, debug.kestrel.phone=host:port override) + PhoneBridge singleton creating PhoneBackend(clientFor, local=engine, stateFile=filesDir/phone-jobs.json, firstId distinct from engine/stream ids, modeOf, onChange=uiChanged); (c) DownloadController: offload in enqueue, route pause/resume/cancel/restart/remove/clearFinished by phone.owns(id), merge phone.views() + wrap engine views with phone.noteFor/createdFor, needsService, setFetcher(FETCHER_LAN, LanFetcher()); new Action.DOWNLOAD_HERE label; (d) Settings rows: mode (Off / When the phone is there / Always), Pair with phone (type 16-char code, Pairing.parse), Test the phone link (LinkTest.run on IO thread, show report); (e) Diagnostics 'phone link' lines; (f) Robolectric/JVM tests where cheap; then Y2e :courier module. Design = research/PHONE_OFFLOAD.md 4.3, 7, 8. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b33092b ckpt 930: Y2d step 1 DONE: SocketHttp (cleartext-safe socket HTTP client), LanFetcher (p
  a042f23 ckpt 923: Y2c DONE (phone-side host logic + staging files + JSON store + SSRF guard; fet
  e084b8b ckpt 892: Y2b DONE: core/courier package (Json, Sign+Pairing+ReplayGuard+AuthThrottle, H
  89585c0 ckpt 874: alpha18 SHIPPED and recorded (kestrel-g-v0.3.0-alpha18, run 43164931160 green,
  1569d02 ckpt 869: gated kestrel-g-v0.3.0-alpha18 (gecko code 117) and pushed it: handoff self-te
  9932748 ckpt 868: pre-ship: alpha18: the updater now always tries to install (it no longer stops
  879b10f ckpt 867: Y1 DONE (updater always tries: no storage gate in download()/install(), floor2
  ceaa63c ckpt 860: owner message 12 written into TASKS (Y1 updater must always try, Y2 phone offl
  eadd5fc ckpt 856: post-alpha17 in the tree (NOT shipped): Make MP4 for finished .ts rows (Stream
  0948a0d ckpt 843: post-dispatch hardening (NOT in alpha17): TsToMp4.peekFormatChange (unmarked s
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
