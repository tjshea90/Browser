# CHECKPOINT 1183 — read me first, then TASKS.md

**Written:** 2026-11-12T20:55:00Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `aaedcf8` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha23 (gecko code 122) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha23: a new Save video sheet and Fast Wi-Fi tuning. Long press a video: the sheet picks the exact video you pressed (small or odd files such as .php endpoints are tucked under Other files), shows the name the site gives it with a Rename button, every quality with its size and length, and one big Download button. One Phone helper switch now turns every phone feature on or off together. With LinkMeter Fast Wi-Fi on, the browser turns IPv6 off and lets downloads use more of the link while a page loads (Diagnostics shows whether LinkMeter's tunnel was recognised), and Home says Fast Wi-Fi. Also: swipe a tab card away to close it, a tab comes back with a picture while it loads after the app was closed, and a long press on a video works even when two tabs hold the same page."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha23), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha23 "alpha23: a new Save video sheet and Fast Wi-Fi tuning. Long press a video: the sheet picks the exact video you pressed (small or odd files such as .php endpoints are tucked under Other files), shows the name the site gives it with a Rename button, every quality with its size and length, and one big Download button. One Phone helper switch now turns every phone feature on or off together. With LinkMeter Fast Wi-Fi on, the browser turns IPv6 off and lets downloads use more of the link while a page loads (Diagnostics shows whether LinkMeter's tunnel was recognised), and Home says Fast Wi-Fi. Also: swipe a tab card away to close it, a tab comes back with a picture while it loads after the app was closed, and a long press on a video works even when two tabs hold the same page."
NEVER ship kestrel-g-v0.3.0-alpha23 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  8e8c9bc ckpt 1176: AB3 code done + unit-tested: NetProfile (core) now has isFastTunnel (10.111.2
  3a65bfe ckpt 1151: AB2 verified on the Android 10 emulator: long press opens the new Save video 
  d9db0b5 ckpt 1139: AB1 done: ONE master switch for the phone helper: PhoneBridge.setMaster/maste
  98cbc10 ckpt 1135: Owner message 15 written into TASKS.md as AB (AB1 one master switch for the p
  b420dda ckpt 1132: alpha22 SHIPPED and recorded (run 43272414589 green incl. the first publish u
  b51c7bc ckpt 1126: alpha22 first run (#69) FAILED in test-jvm: ConnectProxyTest.statsCountTunnel
  df466e6 ckpt 1125: gated kestrel-g-v0.3.0-alpha22 (gecko code 121) and pushed it: handoff self-t
  e283585 ckpt 1123: AA3-8 engine check (Mozilla Maven <latest> = ours 160.0.20261104181552), AA3-
  b39b8fd ckpt 1118: alpha22 shipped to GitHub: ship.sh gates green, pushed; workflow run 43270755
  4f54c04 ckpt 1117: gated kestrel-g-v0.3.0-alpha22 (gecko code 121) and pushed it: handoff self-t
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
