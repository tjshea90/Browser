# CHECKPOINT 1016 — read me first, then TASKS.md

**Written:** 2026-11-12T13:55:26Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `c317b7d` (this checkpoint is the commit after it)

## Just done
Z10 done (read LinkMeter): the owner's carrier caps the HOTSPOT at 5 Mbit/s, LinkMeter already has Fast Wi-Fi (VPN + phone relay, port 8767), LinkMeter's phone control port is 8765 = the Courier's default (clash!), PhoneBridge.gateway() breaks under a VPN. research/SPEED2.md written: facts, where the time goes, what the phone can/cannot genuinely speed up (the Z3 answer), decisions (HTTP CONNECT proxy in the Courier for page loads, PAC with DIRECT failover, unmetered, ad block stays in the engine)

## Do this next
BUILD ORDER (Z2 claimed): (1) Courier API port 8765->8780 + proxy port 8781 in CourierPrefs/PhoneBridge/Prefs defaults + docs; PhoneBridge.gateway(): loop cm.allNetworks (Wi-Fi, not VPN) instead of activeNetwork; (2) core/courier ConnectProxy (pure Kotlin) + JVM tests; CourierHost POST /v1/proxy/arm + status.proxy; CourierClient.armProxy; (3) :courier runtime starts the proxy; (4) tablet ProxyController: PAC via GeckoPreferenceController (network.proxy.type/autoconfig_url/failover_timeout), arm keepalive, reachability gating, Settings switches + menu tile + Diagnostics + LinkTest 4th measure; (5) unmetered default + prefs batch (verify names in libxul, check-gecko-prefs.sh) + bfcache probe on the emulator; (6) emulator proof, ship Courier alpha3 + Kestrel G alpha21, tell the owner. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  83be731 ckpt 1013: Owner message 13 written into TASKS.md as Z0-Z10 in their words (INBOX has it
  3a332d4 ckpt 1010: Y2 stage 2 SHIPPED and recorded: Kestrel Courier alpha2 (kestrel-courier-v0.1
  4389f39 ckpt 1007: gated kestrel-g-v0.3.0-alpha20 (gecko code 119) and pushed it: handoff self-t
  d3a8580 ckpt 1005: gated kestrel-g-v0.3.0-alpha20 (gecko code 119) and pushed it: handoff self-t
  5047bd2 ckpt 1004: gated kestrel-courier-v0.1.0-alpha2 (courier code 2) and pushed it: handoff s
  1b67245 ckpt 1003: Y2h2 verified end to end and documented; full gates green (core 506 tests, ap
  366f38a ckpt 996: Y2h2 code: Remuxer moved to shared-android/kotlin (package app.kestrel.platfor
  eab846d ckpt 989: Y2h stage 2a ticked with emulator evidence (video through the phone: live row,
  325467d ckpt 986: Y2 stage 2 (video streams through the phone) code complete, JVM + build verifi
  f6958c7 ckpt 976: Y2 stage 1 SHIPPED and recorded: kestrel-courier-v0.1.0-alpha1 (run 4317795327
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
