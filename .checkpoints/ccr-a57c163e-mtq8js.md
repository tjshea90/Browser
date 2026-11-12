# CHECKPOINT 1048 — read me first, then TASKS.md

**Written:** 2026-11-12T14:55:35Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `7bcd64e` (this checkpoint is the commit after it)

## Just done
Z8 proven on the emulator (Android 10, GeckoView 160 + Courier in one emulator): the PAC reaches Gecko (Diagnostics read-back type=2 + autoconfig_url), https://site.test:8443 went through the phone proxy (Courier log 'tunnel to site.test:8443 open after 36 ms', the test server saw ClientHello SNI=site.test; the tablet never resolved the name), a dead proxy first in the PAC falls back to DIRECT (connection refused), a proxy that accepts then resets an unarmed tablet ALSO falls back to DIRECT (Courier turned away 157 attempts, the page still loaded). Back cache: Gecko restores page A from the bfcache on Back (persisted=true, same JS stamp) but NOT when the page sends Cache-Control: no-store. Config v4 read back with types: viewers 4, timeout 3600 s, memory cache 64 MB, decode step 64 KB, framevisibility heights 2 (an int, my first string was ignored), lazy margins '1500.0' (float as string), precache shaders on, media 60/30, prefetch-next true. Proxy keeps the phone awake while tunnels are open

## Do this next
Finish Z8/Z1/Z5-Z7 and ship: (1) research/SPEED2.md section 6 (experiment results, prefs table, bfcache finding, no-store caveat) + TASKS ticks with evidence; (2) full gates: ./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :courier:lintDebug :geckoapp:compileDebugKotlin (run in background to a log file, not piped to head); bash tools/check-gecko-prefs.sh; (3) bump courier 0.1.0-alpha3 code 3, geckoapp 0.3.0-alpha21 code 120 (+ bash tools/sync-ext-version.sh), bash ship.sh courier then gecko (ship.sh commits + gates), dispatch android.yml twice (app=courier, app=gecko), wait, mcp__github__get_release_by_tag, record-release.sh after green; (4) answer the owner: what the phone can/cannot speed up (LinkMeter fact: 5 Mbit/s hotspot cap, Fast Wi-Fi exists; the Courier port clash with LinkMeter 8765 fixed), ad block stays in the engine, new switches (menu tile Phone helper, Settings pages switch, data saver off), instant Back works except no-store pages, 5 GHz advice, what is NOT verified (real devices). Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M core/src/test/kotlin/app/kestrel/core/courier/ConnectProxyTest.kt

## Last ten checkpoints
```
  060b94e ckpt 1042: Z8 tablet side + Z1 unmetered built and unit-tested: ProxyLink-driven PagePro
  6208816 ckpt 1027: Z8 core done + Courier wiring: Courier API port now 8780 (CourierPorts, off L
  d886b5f ckpt 1016: Z10 done (read LinkMeter): the owner's carrier caps the HOTSPOT at 5 Mbit/s, 
  83be731 ckpt 1013: Owner message 13 written into TASKS.md as Z0-Z10 in their words (INBOX has it
  3a332d4 ckpt 1010: Y2 stage 2 SHIPPED and recorded: Kestrel Courier alpha2 (kestrel-courier-v0.1
  4389f39 ckpt 1007: gated kestrel-g-v0.3.0-alpha20 (gecko code 119) and pushed it: handoff self-t
  d3a8580 ckpt 1005: gated kestrel-g-v0.3.0-alpha20 (gecko code 119) and pushed it: handoff self-t
  5047bd2 ckpt 1004: gated kestrel-courier-v0.1.0-alpha2 (courier code 2) and pushed it: handoff s
  1b67245 ckpt 1003: Y2h2 verified end to end and documented; full gates green (core 506 tests, ap
  366f38a ckpt 996: Y2h2 code: Remuxer moved to shared-android/kotlin (package app.kestrel.platfor
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
