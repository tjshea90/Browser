# CHECKPOINT 1042 — read me first, then TASKS.md

**Written:** 2026-11-12T14:12:56Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `9bdec34` (this checkpoint is the commit after it)

## Just done
Z8 tablet side + Z1 unmetered built and unit-tested: ProxyLink-driven PageProxy (PAC data: URL via GeckoPreferenceController on the default branch, DIRECT fallback, plain http/private/single-label hosts direct; JS logic checked in Node on 21 URLs), Settings switches (Phone helper section, pages switch, data saver default OFF + NetworkMonitor/FilterUpdater/DownloadController no longer act metered unless the saver is on), main-menu 'Phone helper' tile (one tap = hotspot only), typing the pairing code switches the helper on, link test measures pages through the phone, Courier screen shows proxy numbers; core 532 tests + app 103 green; GeckoView 160 has network.proxy.type/autoconfig_url/failover_timeout (verified in libxul) but NO network.predictor.*

## Do this next
Emulator proof of Z8: build debug APKs (./gradlew :courier:assembleDebug :geckoapp:assembleDebug -PgeckoAbi=x86_64 --offline), adb install -r -g both, setprops debug.kestrel.phone 127.0.0.1:8780 + debug.kestrel.courier.noguard 1, pair (Courier code -> Settings > Phone helper > Pair), host side: python HTTPS server on 8443 with a self-signed cert (openssl in scratchpad) serving a page; load https://site.test:8443/ (PAC sends it to the proxy, Courier maps .test to 10.0.2.2): expect a cert error page and proxy tunnels>0 in the Courier screen/status; Accept the risk if offered to see the page fully; then the failover tests with setprop debug.kestrel.pac data:...PROXY 127.0.0.1:9; DIRECT (dead proxy) and an unarmed proxy (RST) against https://10.0.2.2:8443 to see whether Gecko falls back to DIRECT; if RST does not fail over make the proxy refuse (stop listening) while nobody is armed. Then prefs batch (gecko-prefs v4: unmetered media cache 60/30, prefetch-next true, bfcache 4, framevisibility heights 2, lazy-load margins, min_expiration, decode_bytes, precache-shaders, memory cache 64MB) with read-back types on the emulator, bfcache probe page, ship Courier alpha3 + Kestrel G alpha21. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M app/src/main/kotlin/app/kestrel/browser/download/PhoneBridge.kt
     M courier/src/main/kotlin/app/kestrel/courier/CourierRuntime.kt
     M geckoapp/src/main/kotlin/app/kestrel/browser/GeckoEngine.kt

## Last ten checkpoints
```
  6208816 ckpt 1027: Z8 core done + Courier wiring: Courier API port now 8780 (CourierPorts, off L
  d886b5f ckpt 1016: Z10 done (read LinkMeter): the owner's carrier caps the HOTSPOT at 5 Mbit/s, 
  83be731 ckpt 1013: Owner message 13 written into TASKS.md as Z0-Z10 in their words (INBOX has it
  3a332d4 ckpt 1010: Y2 stage 2 SHIPPED and recorded: Kestrel Courier alpha2 (kestrel-courier-v0.1
  4389f39 ckpt 1007: gated kestrel-g-v0.3.0-alpha20 (gecko code 119) and pushed it: handoff self-t
  d3a8580 ckpt 1005: gated kestrel-g-v0.3.0-alpha20 (gecko code 119) and pushed it: handoff self-t
  5047bd2 ckpt 1004: gated kestrel-courier-v0.1.0-alpha2 (courier code 2) and pushed it: handoff s
  1b67245 ckpt 1003: Y2h2 verified end to end and documented; full gates green (core 506 tests, ap
  366f38a ckpt 996: Y2h2 code: Remuxer moved to shared-android/kotlin (package app.kestrel.platfor
  eab846d ckpt 989: Y2h stage 2a ticked with emulator evidence (video through the phone: live row,
```

(14 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
