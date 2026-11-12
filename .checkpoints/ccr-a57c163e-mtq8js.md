# CHECKPOINT 1185 — read me first, then TASKS.md

**Written:** 2026-11-12T21:04:16Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `b974fe5` (this checkpoint is the commit after it)

## Just done
alpha23 CI run 43305054932 failed in test-jvm: BrowserFlowTest.newTabPageWarnsWhenNoWebViewIsInstalled (1 of 141) - a race in the TEST: the NTP notice is posted from a background thread (refreshNtp: Io.exec -> Io.ui) after launch()'s single looper pass; passed locally 141/141 twice. Fixed the test to wait for the text (awaitText, 8 s), BrowserFlowTest 3x green locally. No tag was created (build jobs skipped), nothing recorded in BUILDLOG, so alpha23 (code 122) is still unshipped. Also found+fixed earlier: a long press on a video was dropped when two tabs held the same address (BrowserActivity.tabOfTouch).

## Do this next
RE-DISPATCH the same build: mcp__github__actions_run_trigger run_workflow android.yml ref ccr-a57c163e-mtq8js inputs app=gecko full_build=true note=(the alpha23 note in the previous ckpt 1183 commit message); wait for the tag with: until git ls-remote origin refs/tags/kestrel-g-v0.3.0-alpha23 shows it (background loop, 10 s steps); when the run is green: mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha23 (APK present), bash tools/record-release.sh kestrel-g-v0.3.0-alpha23 '<note>', then reply to owner message 15 (Release link on its own line), then AA3-2 typing lag (try dom.interactive_widget_default_resizes_visual = true: libxul 160 has the pref; test on the emulator with a page that logs innerHeight vs visualViewport.height when an input is focused)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  65bbb1c ckpt 1183: gated kestrel-g-v0.3.0-alpha23 (gecko code 122) and pushed it: handoff self-t
  8e8c9bc ckpt 1176: AB3 code done + unit-tested: NetProfile (core) now has isFastTunnel (10.111.2
  3a65bfe ckpt 1151: AB2 verified on the Android 10 emulator: long press opens the new Save video 
  d9db0b5 ckpt 1139: AB1 done: ONE master switch for the phone helper: PhoneBridge.setMaster/maste
  98cbc10 ckpt 1135: Owner message 15 written into TASKS.md as AB (AB1 one master switch for the p
  b420dda ckpt 1132: alpha22 SHIPPED and recorded (run 43272414589 green incl. the first publish u
  b51c7bc ckpt 1126: alpha22 first run (#69) FAILED in test-jvm: ConnectProxyTest.statsCountTunnel
  df466e6 ckpt 1125: gated kestrel-g-v0.3.0-alpha22 (gecko code 121) and pushed it: handoff self-t
  e283585 ckpt 1123: AA3-8 engine check (Mozilla Maven <latest> = ours 160.0.20261104181552), AA3-
  b39b8fd ckpt 1118: alpha22 shipped to GitHub: ship.sh gates green, pushed; workflow run 43270755
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
