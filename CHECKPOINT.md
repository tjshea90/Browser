# CHECKPOINT 1365 — read me first, then TASKS.md

**Written:** 2026-11-13T01:35:26Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `0849251` (this checkpoint is the commit after it)

## Just done
AD1a-1 DONE: link warm-up triggers (finger down + top three search results) in content.js/background.js/ShieldsBridge/BrowserActivity with core WarmPolicy (9 JVM tests) + 8 web tests; EMULATOR proofs: finger-down ClientHello while the finger is still down (EX-15), image Accept header without image/jxl (EX-4), web-font fallback 0.5 s readable ~2.5 s sooner (EX-16); defaults flipped ON: link look-ups ahead (not with Data saver / private tab in front), font fallback; Settings 'Warm up connections'; EX-13 decided not to build (two workers cover each other's gaps); TASKS/EX_RESULTS updated. Full suites NOT yet rerun after these edits

## Do this next
Run the full gates (./gradlew :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin, cd web-tests && npm test) in the background, then AD1b-1 download manager: read app/ui downloads screen + store schema, add queue order/start now/reorder + digest check at completion + read coalescing + raw-socket fake server tests; also small AD1a-3: Decode lab recommendation button 'Skip AVIF under Fast Wi-Fi' (image.http.accept USER branch) and optional Wi-Fi sleep test for EX-7

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ac0f553 ckpt 1353: Owner message 17 written into TASKS as AD1 (AD1a review rows, AD1b earlier AA
  16fc889 ckpt 1350: alpha25 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha25 (gecko code 124), run 
  62b643d ckpt 1348: alpha25 (code 124) gated + pushed (ckpt 1347) and DISPATCHED: android.yml run
  e95b292 ckpt 1347: gated kestrel-g-v0.3.0-alpha25 (gecko code 124) and pushed it: handoff self-t
  bede630 ckpt 1346: EX-10 PROVEN on the emulator (Tap to load box replaced the third-party frame;
  d7c8bb0 ckpt 1341: EX-1 PROVEN on the emulator (WifiService acquireWifiLock lockMode=3 at the st
  ae14f0d ckpt 1334: :style() restyling rules in core (697 of 770 real rules accepted; CosmeticSty
  217004a ckpt 1327: EX-2 PROVEN on the emulator (Gecko's own nsHttp log + host TLS listener: old 
  59c0013 ckpt 1322: Data saver choice re-applies the engine prefs (the look-up-links lab switch f
  49dea49 ckpt 1320: Docs: docs/ARCHITECTURE.md alpha25 section, research/EX_RESULTS.md (what was 
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
