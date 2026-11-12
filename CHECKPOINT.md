# CHECKPOINT 1151 — read me first, then TASKS.md

**Written:** 2026-11-12T20:14:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `95a8d0a` (this checkpoint is the commit after it)

## Just done
AB2 verified on the Android 10 emulator: long press opens the new Save video sheet (NAME + RENAME, formats with sizes, THIS VIDEO badge, other files collapsed); typed name 'My Holiday Clip' + 720p -> Download -> cleartext confirm -> server saw GET /v/clip_720p.mp4 and /sdcard/Download/My Holiday Clip.mp4 (1900673 bytes) exists. Added the address's file name to rows under 'Other files' (VideoFlow.namedOther, not yet rebuilt/seen on the emulator). Tests green earlier: MediaRankTest 14, MediaNamesTest 7, VideoPanelTest 6, core 549. NOT yet ticked in TASKS.md: needs one full run of :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin + web-tests after AB3 code.

## Do this next
AB3 (item 3, owner message 15): 1) core NetProfile.Out gets disableIPv6 + pageYieldKBps (+ NetProfileTest), 2) GeckoEngine.applyNetwork sets network.dns.disableIPv6 on PREF_BRANCH_DEFAULT when the VPN is active, 3) DownloadController page-yield value from the profile, 4) BrowserActivity.onNetworkChanged reloads a still-loading current page when NetworkMonitor.vpn flips, 5) Diagnostics/readBackPrefs wording, 6) write research/SPEED4.md (LinkMeter review: relay sound, nothing safe to change LinkMeter-side without device numbers, list of what to measure), 7) run :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin + cd web-tests && npm test, 8) claim.sh AB1/AB2/AB3 --done with evidence, 9) ship alpha23 (versionCode 122, 0.3.0-alpha23, tools/sync-ext-version.sh, ship.sh gecko, dispatch android.yml app=gecko full_build=true, record-release.sh after green), 10) answer owner message 15 in plain language with the Release link on its own line

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d9db0b5 ckpt 1139: AB1 done: ONE master switch for the phone helper: PhoneBridge.setMaster/maste
  98cbc10 ckpt 1135: Owner message 15 written into TASKS.md as AB (AB1 one master switch for the p
  b420dda ckpt 1132: alpha22 SHIPPED and recorded (run 43272414589 green incl. the first publish u
  b51c7bc ckpt 1126: alpha22 first run (#69) FAILED in test-jvm: ConnectProxyTest.statsCountTunnel
  df466e6 ckpt 1125: gated kestrel-g-v0.3.0-alpha22 (gecko code 121) and pushed it: handoff self-t
  e283585 ckpt 1123: AA3-8 engine check (Mozilla Maven <latest> = ours 160.0.20261104181552), AA3-
  b39b8fd ckpt 1118: alpha22 shipped to GitHub: ship.sh gates green, pushed; workflow run 43270755
  4f54c04 ckpt 1117: gated kestrel-g-v0.3.0-alpha22 (gecko code 121) and pushed it: handoff self-t
  5c226ea ckpt 1103: AA4: K1 K2 K5 K6 verified on the emulator (evidence in research/SPEED3.md 6.1
  084b5ba ckpt 1101: AA4 speed batch code step: K1 LiveBudget (core, 9 tests) + RAM-adaptive live 
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
