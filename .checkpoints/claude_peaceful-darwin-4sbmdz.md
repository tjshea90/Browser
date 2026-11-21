# CHECKPOINT 2828 — read me first, then TASKS.md

**Written:** 2026-11-21T18:30:26Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `845117ff` (this checkpoint is the commit after it)

## Just done
alpha85 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha85 (gecko code 186, run 44934161819 green, APK + sha256 + dm in the Release, BUILDLOG line pushed). LD4a-e ticked; LD4 stays open: LD4f (owner test: a day of browsing, then LOAD ANALYSIS) and LD4g (next steps by verdict)

## Do this next
Nothing is running. Waiting for the owner to install alpha85 and browse normally (claude.ai, YouTube, once with Shields off for claude.ai); then python3 -B -I tools/diag_digest.py: LOAD ANALYSIS + INTEGRITY + 'Sends: N automatic' (LD3f). Next code step depends on the verdict (research/SLOW_LOADS.md section 5). Also open: YTC1/YTC2, ATT1, FLK1 PhoneBackendTest flake (failed once under gate load, passes alone), PB8, LD3g ideas

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  0f2cd29a ckpt 2827: alpha85 gated (core 1991 incl. FLK1 flake passing alone, app 478, lint 0 err
  b9f6b0b0 ckpt 2826: gated kestrel-g-v0.3.0-alpha85 (gecko code 186) and pushed it: handoff self-
  495ef4db ckpt 2825: pre-ship: alpha85: the live diagnostics now explain WHY a page is slow, whic
  4cf115af ckpt 2824: LD4 gate so far: core 1991 tests (1 failed = FLK1 PhoneBackendTest flake, pa
  a1863e08 ckpt 2823: LD4c done: diag_digest.py LOAD ANALYSIS (per site: loads, load/first paint/f
  0c525795 ckpt 2822: LD4b app side done and compiling: GeckoEngine.networkProfileCode, ShieldsBri
  ca1f0fe2 ckpt 2821: LD4b extension side done: pagetime.js (ES5, top frame, document_start, laten
  029be5ca ckpt 2820: LD4b core done: DiagScrub caps (event 480, snapshot lines 1600: the CPU/Bloc
  0b7be92b ckpt 2819: LD4a done: research/SLOW_LOADS.md written from the real uploads: claude.ai 1
  a6b43548 ckpt 2818: LD4 written into TASKS.md in the owner's words (richer live diagnostics + cl
```
