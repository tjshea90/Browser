# CHECKPOINT 1027 — read me first, then TASKS.md

**Written:** 2026-11-12T14:04:01Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `de5618c` (this checkpoint is the commit after it)

## Just done
Z8 core done + Courier wiring: Courier API port now 8780 (CourierPorts, off LinkMeter's 8765), PhoneBridge.gateway() ignores a VPN; core ConnectProxy (HTTP CONNECT relay: half-close, limits, private-destination refusal, reset for strangers) + ProxyGate + GuardedDialer, 18 tests incl. a real HttpsURLConnection through it; CourierHost POST /v1/proxy/arm|disarm + status.proxy; CourierClient.armProxy/disarmProxy; ProxyLink (tablet upkeep thread, states Off/Active/Unavailable) with 8 tests against a real phone host; :courier runtime starts the proxy (port api+1) and ticks only every 5 s when idle. Courier + app compile

## Do this next
Z8 tablet side: (1) app: ProxyController (engine-neutral) owning a ProxyLink: wanted = Prefs.phoneHelper && Prefs.pagesViaPhone; onState -> post to main thread -> EngineProxy.apply(host,port)/clear(); hook refresh() into PhoneBridge.onNetwork + BrowserActivity.onResume; (2) geckoapp GeckoEngine.setPhoneProxy(host?, port): GeckoPreferenceController.setGeckoPrefs on PREF_BRANCH_DEFAULT: network.proxy.type=2 + network.proxy.autoconfig_url=data:application/x-ns-proxy-autoconfig,<PAC> (PROXY host:port; DIRECT for https/wss only, DIRECT for http, plain names, private ranges) + network.proxy.failover_timeout=10; clear = type 5 + empty url; add the pref names to tools/check-gecko-prefs.sh list; (3) Prefs: phoneHelper (master, default true once paired), pagesViaPhone (default true), downloadsMinMb; Settings section 'Phone helper' + main-menu tile; Diagnostics line (state + phone status.proxy numbers); LinkTest 4th measure; (4) CourierActivity line with proxy stats; (5) emulator proof: HTTPS test server on host 8443 (self-signed: Gecko cert error page proves the CONNECT tunnel) + fallback test with proxy reset. Z1 unmetered + prefs batch after. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d886b5f ckpt 1016: Z10 done (read LinkMeter): the owner's carrier caps the HOTSPOT at 5 Mbit/s, 
  83be731 ckpt 1013: Owner message 13 written into TASKS.md as Z0-Z10 in their words (INBOX has it
  3a332d4 ckpt 1010: Y2 stage 2 SHIPPED and recorded: Kestrel Courier alpha2 (kestrel-courier-v0.1
  4389f39 ckpt 1007: gated kestrel-g-v0.3.0-alpha20 (gecko code 119) and pushed it: handoff self-t
  d3a8580 ckpt 1005: gated kestrel-g-v0.3.0-alpha20 (gecko code 119) and pushed it: handoff self-t
  5047bd2 ckpt 1004: gated kestrel-courier-v0.1.0-alpha2 (courier code 2) and pushed it: handoff s
  1b67245 ckpt 1003: Y2h2 verified end to end and documented; full gates green (core 506 tests, ap
  366f38a ckpt 996: Y2h2 code: Remuxer moved to shared-android/kotlin (package app.kestrel.platfor
  eab846d ckpt 989: Y2h stage 2a ticked with emulator evidence (video through the phone: live row,
  325467d ckpt 986: Y2 stage 2 (video streams through the phone) code complete, JVM + build verifi
```

(10 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
