# CHECKPOINT 1069 — read me first, then TASKS.md

**Written:** 2026-11-12T15:44:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `682f35b` (this checkpoint is the commit after it)

## Just done
AA2 + AA1 core done: Courier closed in the browser (every option OFF by default incl. pagesViaPhone, pairing no longer enables, menu tile only when paired, effective mode OFF under a VPN, PageProxy off under a VPN, Settings says it is finished work and not needed with Fast Wi-Fi), NetProfile (core, 5 tests) + NetworkMonitor.vpn (+ debug.kestrel.vpn) + GeckoEngine.applyNetwork effective values re-applied on VPN change + Diagnostics 'Network:' line + Settings 'Right now'; 2 Robolectric tests; Y2/Y2i/Y2k closed in TASKS, PHONE_OFFLOAD section 17, research/SPEED3.md started. compiles, targeted tests green

## Do this next
Wait for the three review agents (audit-prompts.md, perf-kotlin.md, perf-js.md in the scratchpad; notifications arrive by themselves), meanwhile: add the bfcache readback names (docshell.shistory.bfcache.allow_unload_listeners, require_no_opener, ship_allow_beforeunload_listeners, browser.sessionhistory.max_entries) to GeckoEngine.readBackPrefs + gecko-prefs v5 candidates (viewers 6, size_factor 12, allow_unload_listeners true after reading the default), emulator proof of the VPN profile with setprop debug.kestrel.vpn 1 (Diagnostics Network line + trr/http3 readback), then act on the reports; ship alpha22. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ac00a1f ckpt 1061: Owner message 14 written into TASKS.md as AA1-AA4 in their words (INBOX has i
  d75864a ckpt 1057: Owner message 13 ANSWERED and SHIPPED: Kestrel Courier alpha3 (kestrel-courie
  5c2891c ckpt 1054: Docs: CLAUDE.md + BRIEF.md record the owner's unmetered/5 Mbit/s facts and th
  95c9050 ckpt 1053: gated kestrel-g-v0.3.0-alpha21 (gecko code 120) and pushed it: handoff self-t
  2a514af ckpt 1052: gated kestrel-courier-v0.1.0-alpha3 (courier code 3) and pushed it: handoff s
  d803d90 ckpt 1051: pre-ship: alpha3: pages through the phone. The phone now loads the tablet's w
  8c64199 ckpt 1048: Z8 proven on the emulator (Android 10, GeckoView 160 + Courier in one emulato
  060b94e ckpt 1042: Z8 tablet side + Z1 unmetered built and unit-tested: ProxyLink-driven PagePro
  6208816 ckpt 1027: Z8 core done + Courier wiring: Courier API port now 8780 (CourierPorts, off L
  d886b5f ckpt 1016: Z10 done (read LinkMeter): the owner's carrier caps the HOTSPOT at 5 Mbit/s, 
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
