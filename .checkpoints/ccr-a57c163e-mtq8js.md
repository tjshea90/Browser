# CHECKPOINT 960 — read me first, then TASKS.md

**Written:** 2026-11-12T07:30:09Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `8dfc132` (this checkpoint is the commit after it)

## Just done
Y2g (CI plumbing for the third APK) written: appinfo.sh/ship.sh/record-release.sh/verify-apk.sh/publish-release.sh/android.yml (build-courier job, tag kestrel-courier-v*, app=courier input, courier lint step, test-js only for browser apps)/test_handoff.sh/resume.sh; self-test 62 checks green; NOT yet exercised by a real run

## Do this next
Y2j: finish the emulator proof (the address field may now contain the typed URL: dump the UI, press Enter, confirm Download, sample Menu > Downloads), then ship: bump geckoapp to versionCode 118 / 0.3.0-alpha19 and courier stays 1 / 0.1.0-alpha1, bash ship.sh courier 'note' first (new app: gate = lint + unit tests + :courier:lintDebug), dispatch android.yml app=courier full_build=true, then verify the Release, record-release.sh, then the same for gecko alpha19; give the owner both Release links on their own lines + the install/pairing steps; heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M tools/test_handoff.sh

## Last ten checkpoints
```
  c7820e2 ckpt 957: Y2e BUILT (not yet shipped): module :courier (CourierRuntime/Service/Activity/
  b6b44e0 ckpt 945: Y2d DONE incl. Android glue: PhoneBridge (pairing code, gateway discovery, deb
  46aaf8d ckpt 938: Y2d steps 1+2 DONE (pure core): SocketHttp, LanFetcher, CourierClient, PhoneBa
  b33092b ckpt 930: Y2d step 1 DONE: SocketHttp (cleartext-safe socket HTTP client), LanFetcher (p
  a042f23 ckpt 923: Y2c DONE (phone-side host logic + staging files + JSON store + SSRF guard; fet
  e084b8b ckpt 892: Y2b DONE: core/courier package (Json, Sign+Pairing+ReplayGuard+AuthThrottle, H
  89585c0 ckpt 874: alpha18 SHIPPED and recorded (kestrel-g-v0.3.0-alpha18, run 43164931160 green,
  1569d02 ckpt 869: gated kestrel-g-v0.3.0-alpha18 (gecko code 117) and pushed it: handoff self-te
  9932748 ckpt 868: pre-ship: alpha18: the updater now always tries to install (it no longer stops
  879b10f ckpt 867: Y1 DONE (updater always tries: no storage gate in download()/install(), floor2
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
