# CHECKPOINT 2829 — read me first, then TASKS.md

**Written:** 2026-11-21T19:34:35Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `ede36761` (this checkpoint is the commit after it)

## Just done
PV1 written into TASKS.md in the owner's words (private tabs: nothing saved at any time, nothing sent anywhere; confirm, and fix if not). alpha85 is released and recorded; nothing else running

## Do this next
PV1a: audit. Enumerate sinks by grep: every Health.log with a host/url, SharedPreferences/file writes, history/session/lastlook/thumbnail/downloads/video rows/crash dumps/LiveDiag, network calls (warm, phone helper, update, live diag, DoH); check each against tab.isPrivateTab; write research/PRIVATE_TABS.md rows; then fix + safety net (PrivateHosts set scrubbed from Health/LiveDiag/Diagnostics) + tests

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  ede36761 ckpt 2828: alpha85 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/
  0f2cd29a ckpt 2827: alpha85 gated (core 1991 incl. FLK1 flake passing alone, app 478, lint 0 err
  b9f6b0b0 ckpt 2826: gated kestrel-g-v0.3.0-alpha85 (gecko code 186) and pushed it: handoff self-
  495ef4db ckpt 2825: pre-ship: alpha85: the live diagnostics now explain WHY a page is slow, whic
  4cf115af ckpt 2824: LD4 gate so far: core 1991 tests (1 failed = FLK1 PhoneBackendTest flake, pa
  a1863e08 ckpt 2823: LD4c done: diag_digest.py LOAD ANALYSIS (per site: loads, load/first paint/f
  0c525795 ckpt 2822: LD4b app side done and compiling: GeckoEngine.networkProfileCode, ShieldsBri
  ca1f0fe2 ckpt 2821: LD4b extension side done: pagetime.js (ES5, top frame, document_start, laten
  029be5ca ckpt 2820: LD4b core done: DiagScrub caps (event 480, snapshot lines 1600: the CPU/Bloc
  0b7be92b ckpt 2819: LD4a done: research/SLOW_LOADS.md written from the real uploads: claude.ai 1
```
