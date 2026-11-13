# CHECKPOINT 1482 — read me first, then TASKS.md

**Written:** 2026-11-13T05:11:00Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `f1ba26b` (this checkpoint is the commit after it)

## Just done
Owner message 21 (their Diagnostics copy from the tablet, alpha27) written into TASKS as AH1 (a-g) and read in research/TABLET_DIAG_2026-11-13.md: the tablet cannot install alpha28 (1.62 GB free, about 3.1 GB needed), doraby 18 blocked vs 7 of ours confirms the Firefox tracking-protection cause, hardware VP9/H.265/H.264 decoders confirmed, Video policy line stale, no gesture bug

## Do this next
AH1d: GeckoEngine.applyVideo set videoPolicyText before the prefs + UI-thread hop + Health.log; then AH1c ContentBlockingController exceptions on the per-site Shields switch; then AH1b updater sheet (after-uninstall numbers, Clear cache); then answer the owner (storage first)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  29bfbb4 ckpt 1478: handoff saved (.handoff/handoff.md + archive 2026-11-13T05-01-56Z; CLAUDE.md/
  2e2935d ckpt 1476: alpha28 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha28 (gecko code 127), run 
  ec67869 ckpt 1474: gated kestrel-g-v0.3.0-alpha28 (gecko code 127) and pushed it: handoff self-t
  a562cab ckpt 1473: alpha28 gates green: JVM (core + app + gecko compile), 123 web tests, pref na
  a3a8e82 ckpt 1470: AF1 cause found + fixed + proven in real Gecko on the emulator: Kestrel asked
  a0403ec ckpt 1458: AF1 diagnostics built (not yet compiled/shipped): extension page log (blocked
  697f62b ckpt 1451: Owner message 20 (their Video lab / Tap lab / Decode lab numbers from the tab
  101b4b1 ckpt 1447: Owner message 19 (doraby.com: no photos, no search bar in Kestrel; Brave fine
  c0c149f ckpt 1443: alpha27 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha27 (gecko code 126), run 
  d107bc4 ckpt 1440: alpha27 (kestrel-g-v0.3.0-alpha27, gecko code 126) gated, pushed and DISPATCH
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
