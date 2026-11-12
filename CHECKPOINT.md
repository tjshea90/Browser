# CHECKPOINT 957 — read me first, then TASKS.md

**Written:** 2026-11-12T07:27:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `7ae4d2a` (this checkpoint is the commit after it)

## Just done
Y2e BUILT (not yet shipped): module :courier (CourierRuntime/Service/Activity/Prefs/Net/BootReceiver, resources, manifest specialUse FGS), debug APK 3.4 MB; Courier screen verified on the emulator (shows code, switch starts the foreground service, 'ready on port 8765', host hello over adb forward = 200 + 401 for unsigned); tablet Settings rows verified on the emulator (pair with code, mode, status line); FOUND+FIXED the hand-over race in PhoneBackend (row grace; test mutation-checked); PeerGuard now also judges by the address the tablet connected to; design note section 14 has the rig recipe

## Do this next
Y2j: finish the emulator proof: type 'http://10.0.2.2:8004/big.bin' into the tablet browser address field IN CHUNKS (adb input text drops characters; verify with a UI dump of the EditText), confirm the Download dialog, then sample Menu > Downloads: expect the row 'On your phone ...' with live progress, then 'Copied from your phone', then COMPLETED; check the file arrived (adb shell ls /sdcard/Download) and sha256 equals site/big.bin; then test: pause/resume/cancel from the row, kill the Courier mid-download (am force-stop) and restart it, 'Test the phone link' with debug.kestrel.phone.testurl; then Y2g CI plumbing (appinfo.sh, ship.sh courier, record-release.sh, verify-apk.sh, android.yml input, test_handoff.sh) and a first Courier release + Kestrel G alpha19. Emulator state right now: both apps installed, Courier ready, tablet paired + mode When-the-phone-is-there, site servers on 8000/8004 running on the host. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b6b44e0 ckpt 945: Y2d DONE incl. Android glue: PhoneBridge (pairing code, gateway discovery, deb
  46aaf8d ckpt 938: Y2d steps 1+2 DONE (pure core): SocketHttp, LanFetcher, CourierClient, PhoneBa
  b33092b ckpt 930: Y2d step 1 DONE: SocketHttp (cleartext-safe socket HTTP client), LanFetcher (p
  a042f23 ckpt 923: Y2c DONE (phone-side host logic + staging files + JSON store + SSRF guard; fet
  e084b8b ckpt 892: Y2b DONE: core/courier package (Json, Sign+Pairing+ReplayGuard+AuthThrottle, H
  89585c0 ckpt 874: alpha18 SHIPPED and recorded (kestrel-g-v0.3.0-alpha18, run 43164931160 green,
  1569d02 ckpt 869: gated kestrel-g-v0.3.0-alpha18 (gecko code 117) and pushed it: handoff self-te
  9932748 ckpt 868: pre-ship: alpha18: the updater now always tries to install (it no longer stops
  879b10f ckpt 867: Y1 DONE (updater always tries: no storage gate in download()/install(), floor2
  ceaa63c ckpt 860: owner message 12 written into TASKS (Y1 updater must always try, Y2 phone offl
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
