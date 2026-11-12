# CHECKPOINT 1013 — read me first, then TASKS.md

**Written:** 2026-11-12T09:42:47Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `ca67779` (this checkpoint is the commit after it)

## Just done
Owner message 13 written into TASKS.md as Z0-Z10 in their words (INBOX has it verbatim): unmetered hotspot, speed and snappiness first (scrolling, instant Back, caching, no white areas while scrolling), an honest answer on what the phone can speed up, one switch for hotspot-only, ad block 'whatever is logical', Shizuku/linkmeter. No code yet

## Do this next
Z10 first (cheap): mcp__claude-code-remote__list_repos query linkmeter -> add_repo (read) -> read how it uses Shizuku + what it measures; then Z2: write research/SPEED2.md (read geckoapp/src/main/assets gecko-prefs.txt + app Prefs/GeckoRuntime setup, verify candidate pref names exist in the GeckoView libxul strings under the scratchpad aar folder, web-check semantics), then implement tablet-side speed (Z1 unmetered default, Z5-Z7 scrolling/bfcache/caches) and the Z8 toggle in the menu; claim Z2 with bash tools/claim.sh Z2 first. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  3a332d4 ckpt 1010: Y2 stage 2 SHIPPED and recorded: Kestrel Courier alpha2 (kestrel-courier-v0.1
  4389f39 ckpt 1007: gated kestrel-g-v0.3.0-alpha20 (gecko code 119) and pushed it: handoff self-t
  d3a8580 ckpt 1005: gated kestrel-g-v0.3.0-alpha20 (gecko code 119) and pushed it: handoff self-t
  5047bd2 ckpt 1004: gated kestrel-courier-v0.1.0-alpha2 (courier code 2) and pushed it: handoff s
  1b67245 ckpt 1003: Y2h2 verified end to end and documented; full gates green (core 506 tests, ap
  366f38a ckpt 996: Y2h2 code: Remuxer moved to shared-android/kotlin (package app.kestrel.platfor
  eab846d ckpt 989: Y2h stage 2a ticked with emulator evidence (video through the phone: live row,
  325467d ckpt 986: Y2 stage 2 (video streams through the phone) code complete, JVM + build verifi
  f6958c7 ckpt 976: Y2 stage 1 SHIPPED and recorded: kestrel-courier-v0.1.0-alpha1 (run 4317795327
  6dbba44 ckpt 973: gated kestrel-g-v0.3.0-alpha19 (gecko code 118) and pushed it: handoff self-te
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
