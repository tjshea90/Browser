# CHECKPOINT 1061 — read me first, then TASKS.md

**Written:** 2026-11-12T15:38:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `3dd7239` (this checkpoint is the commit after it)

## Just done
Owner message 14 written into TASKS.md as AA1-AA4 in their words (INBOX has it verbatim): optimise for LinkMeter Fast Wi-Fi, close the Courier work and keep it OFF by default, audit every past prompt and finish the gaps, full speed/scroll/Back/caching review. No code yet

## Do this next
Launch 3 read-only agents in the background (audit of all owner prompts -> scratchpad audit-prompts.md; Kotlin perf review -> perf-kotlin.md; extension JS perf review -> perf-js.md), meanwhile: AA2 (Courier closed: defaults off, pairing no longer enables, menu tile only when paired, effectiveMode off while a VPN is active, PageProxy off under VPN, Y2i won't-do + closing note), AA1 (NetworkMonitor.vpn + core NetProfile pure logic + tests, GeckoEngine.applyNetwork effective doh/h3, Diagnostics line, debug.kestrel.vpn override), RAM prefs (max_total_viewers 6, size_factor 12), grep libxul for a no-store bfcache pref. Then act on the three reports, ship alpha22. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d75864a ckpt 1057: Owner message 13 ANSWERED and SHIPPED: Kestrel Courier alpha3 (kestrel-courie
  5c2891c ckpt 1054: Docs: CLAUDE.md + BRIEF.md record the owner's unmetered/5 Mbit/s facts and th
  95c9050 ckpt 1053: gated kestrel-g-v0.3.0-alpha21 (gecko code 120) and pushed it: handoff self-t
  2a514af ckpt 1052: gated kestrel-courier-v0.1.0-alpha3 (courier code 3) and pushed it: handoff s
  d803d90 ckpt 1051: pre-ship: alpha3: pages through the phone. The phone now loads the tablet's w
  8c64199 ckpt 1048: Z8 proven on the emulator (Android 10, GeckoView 160 + Courier in one emulato
  060b94e ckpt 1042: Z8 tablet side + Z1 unmetered built and unit-tested: ProxyLink-driven PagePro
  6208816 ckpt 1027: Z8 core done + Courier wiring: Courier API port now 8780 (CourierPorts, off L
  d886b5f ckpt 1016: Z10 done (read LinkMeter): the owner's carrier caps the HOTSPOT at 5 Mbit/s, 
  83be731 ckpt 1013: Owner message 13 written into TASKS.md as Z0-Z10 in their words (INBOX has it
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
