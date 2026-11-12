# CHECKPOINT 1176 — read me first, then TASKS.md

**Written:** 2026-11-12T20:24:08Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `363f23d` (this checkpoint is the commit after it)

## Just done
AB3 code done + unit-tested: NetProfile (core) now has isFastTunnel (10.111.222.x on the VPN link = LinkMeter's own tunnel), Out.disableIPv6 + pageYieldKBps (150 standard/other VPN, 1000 behind LinkMeter's tunnel), Input.fastTunnel; NetworkMonitor.fastTunnel from LinkProperties (onLinkPropertiesChanged, debug.kestrel.vpn=2 pretends the tunnel); GeckoEngine.applyNetwork sets network.dns.disableIPv6 + DownloadController.setPageYieldKBps; BrowserActivity.onNetworkChanged watches vpn and fastTunnel; Diagnostics Network line, Home 'Fast Wi-Fi' chip, Settings yield text; research/SPEED4.md written (LinkMeter read: relay sound, NODELAY on, 256 KB windows; no LinkMeter change without device numbers; ranked candidates; what to measure). Tests: core 575 (0 fail, 1 pre-existing skip), app 141 (0 fail; NetProfileTest 10, PageYieldTest 7, VpnProfileTest 4), web-tests 76/76, :geckoapp:compileDebugKotlin OK. Not yet seen on the emulator; TASKS AB1/AB2/AB3 not ticked yet.

## Do this next
1) wait for the x86_64 debug build (scratchpad build-ab3.log, EXIT line), adb install -r geckoapp/build/outputs/apk/debug/geckoapp-debug.apk, 2) emulator: setprop debug.kestrel.vpn 2 + restart app -> Diagnostics 'Network:' must say LinkMeter tunnel recognised: true and 1000 KB/s, engine read-back disableIPv6=true, Home line 'Fast Wi-Fi'; setprop 0 -> back to standard; re-open http://10.0.2.2:8000/multi.html long press -> 'Other files' rows must show getit.php/dl.php/track.php names, 3) claim.sh AB1/AB2/AB3 --done with evidence, 4) ship alpha23 (versionCode 122, 0.3.0-alpha23, tools/sync-ext-version.sh, ship.sh gecko, dispatch android.yml app=gecko full_build=true, record-release.sh after green), 5) answer owner message 15 in plain language with the Release link on its own line

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  3a65bfe ckpt 1151: AB2 verified on the Android 10 emulator: long press opens the new Save video 
  d9db0b5 ckpt 1139: AB1 done: ONE master switch for the phone helper: PhoneBridge.setMaster/maste
  98cbc10 ckpt 1135: Owner message 15 written into TASKS.md as AB (AB1 one master switch for the p
  b420dda ckpt 1132: alpha22 SHIPPED and recorded (run 43272414589 green incl. the first publish u
  b51c7bc ckpt 1126: alpha22 first run (#69) FAILED in test-jvm: ConnectProxyTest.statsCountTunnel
  df466e6 ckpt 1125: gated kestrel-g-v0.3.0-alpha22 (gecko code 121) and pushed it: handoff self-t
  e283585 ckpt 1123: AA3-8 engine check (Mozilla Maven <latest> = ours 160.0.20261104181552), AA3-
  b39b8fd ckpt 1118: alpha22 shipped to GitHub: ship.sh gates green, pushed; workflow run 43270755
  4f54c04 ckpt 1117: gated kestrel-g-v0.3.0-alpha22 (gecko code 121) and pushed it: handoff self-t
  5c226ea ckpt 1103: AA4: K1 K2 K5 K6 verified on the emulator (evidence in research/SPEED3.md 6.1
```

(24 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
