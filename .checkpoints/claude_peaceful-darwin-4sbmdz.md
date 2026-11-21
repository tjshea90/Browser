# CHECKPOINT 2836 — read me first, then TASKS.md

**Written:** 2026-11-21T20:26:22Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `94df5df7` (this checkpoint is the commit after it)

## Just done
alpha86 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha86 (gecko code 187, run 44944600777 green, APK + sha256 + dm in the Release, BUILDLOG line pushed). PV1 done (7 holes closed, research/PRIVATE_TABS.md); PV1f (owner test) and PV1g (decision: Safe Browsing pause in private tabs) open

## Do this next
Nothing is running. Waiting for the owner: install alpha86, try a private tab (Diagnostics > Private tabs line; Shields change gone after closing), answer PV1g (keep engine malware checks on in private tabs or paused). Also open: LD3f (first automatic diagnostics send), LD4f (a day of browsing then python3 -B -I tools/diag_digest.py LOAD ANALYSIS), LD4g, YTC1/YTC2, ATT1, FLK1 PhoneBackendTest flake, PB8

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  2f0f8b2b ckpt 2835: alpha86 gated (core + app full suites via ship.sh, lint 0 errors, test_hando
  0df22017 ckpt 2834: gated kestrel-g-v0.3.0-alpha86 (gecko code 187) and pushed it: handoff self-
  620bf412 ckpt 2833: pre-ship: alpha86: private tabs checked end to end, and every hole found is 
  253c10df ckpt 2832: PV1: audit written (research/PRIVATE_TABS.md, 26 rows), 7 holes fixed: Shiel
  dcf47bcb ckpt 2831: PV1: tests (PrivateTabsTest 12 incl. canary leak scans of ring / live journa
  8abe214c ckpt 2830: PV1 code in place and compiling: core PrivateScrub + PrivateWindow (71 core 
  985f693e ckpt 2829: PV1 written into TASKS.md in the owner's words (private tabs: nothing saved 
  ede36761 ckpt 2828: alpha85 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/
  0f2cd29a ckpt 2827: alpha85 gated (core 1991 incl. FLK1 flake passing alone, app 478, lint 0 err
  b9f6b0b0 ckpt 2826: gated kestrel-g-v0.3.0-alpha85 (gecko code 186) and pushed it: handoff self-
```
