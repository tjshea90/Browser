# CHECKPOINT 973 — read me first, then TASKS.md

**Written:** 2026-11-12T07:54:08Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `26d9139` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha19 (gecko code 118) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha19: Download on my phone. New Settings > Download on my phone (off by default; needs the small Kestrel Courier app on your phone, see its Release): your phone downloads the file over its own connection and sends it to the tablet over the hotspot wifi, with live progress in the same Downloads row (Pause, Resume, Cancel work). Test the phone link measures the wifi to the phone, the phone's internet and the tablet's internet and says whether it is worth it. When the phone is not there, or cannot do a download, the tablet downloads it by itself and the row says why. Private tabs, app updates and video streams are still downloaded by the tablet itself for now."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha19), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha19 "alpha19: Download on my phone. New Settings > Download on my phone (off by default; needs the small Kestrel Courier app on your phone, see its Release): your phone downloads the file over its own connection and sends it to the tablet over the hotspot wifi, with live progress in the same Downloads row (Pause, Resume, Cancel work). Test the phone link measures the wifi to the phone, the phone's internet and the tablet's internet and says whether it is worth it. When the phone is not there, or cannot do a download, the tablet downloads it by itself and the row says why. Private tabs, app updates and video streams are still downloaded by the tablet itself for now."
NEVER ship kestrel-g-v0.3.0-alpha19 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  cebfa82 ckpt 969: gated kestrel-courier-v0.1.0-alpha1 (courier code 1) and pushed it: handoff se
  919c2a3 ckpt 968: pre-ship: first Courier release: the small app for your PHONE. It downloads fi
  1df58a2 ckpt 967: pre-ship: first Courier release: the small app for your PHONE. It downloads fi
  354d25f ckpt 960: Y2g (CI plumbing for the third APK) written: appinfo.sh/ship.sh/record-release
  c7820e2 ckpt 957: Y2e BUILT (not yet shipped): module :courier (CourierRuntime/Service/Activity/
  b6b44e0 ckpt 945: Y2d DONE incl. Android glue: PhoneBridge (pairing code, gateway discovery, deb
  46aaf8d ckpt 938: Y2d steps 1+2 DONE (pure core): SocketHttp, LanFetcher, CourierClient, PhoneBa
  b33092b ckpt 930: Y2d step 1 DONE: SocketHttp (cleartext-safe socket HTTP client), LanFetcher (p
  a042f23 ckpt 923: Y2c DONE (phone-side host logic + staging files + JSON store + SSRF guard; fet
  e084b8b ckpt 892: Y2b DONE: core/courier package (Json, Sign+Pairing+ReplayGuard+AuthThrottle, H
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
