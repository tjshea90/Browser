# CHECKPOINT 1487 — read me first, then TASKS.md

**Written:** 2026-11-13T05:30:46Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `8d9d819` (this checkpoint is the commit after it)

## Just done
AH1d done and emulator-proven: the Video line stayed on 'decoders unknown' because setGeckoPref threw 'Must have a Handler' on the IO pool after its first pref (only vp9 was applied); GeckoEngine.hopToMain now guards applyVideo/applyNetwork/applyLab/applyScrollFeel

## Do this next
AH1c: finish the build with debug.kestrel.notp (ah1_build2.log), install, run site/etpflag.html through 10.0.2.2:8000 with pref1 urlclassifier.trackingTable.testEntries=127.0.0.1 with and without notp=1 to see whether the per-session useTrackingProtection flag lets a classified host through; if not, fall back to switching AntiTracking live for the front tab; then AH1b updater sheet

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  7d3044a ckpt 1482: Owner message 21 (their Diagnostics copy from the tablet, alpha27) written in
  29bfbb4 ckpt 1478: handoff saved (.handoff/handoff.md + archive 2026-11-13T05-01-56Z; CLAUDE.md/
  2e2935d ckpt 1476: alpha28 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha28 (gecko code 127), run 
  ec67869 ckpt 1474: gated kestrel-g-v0.3.0-alpha28 (gecko code 127) and pushed it: handoff self-t
  a562cab ckpt 1473: alpha28 gates green: JVM (core + app + gecko compile), 123 web tests, pref na
  a3a8e82 ckpt 1470: AF1 cause found + fixed + proven in real Gecko on the emulator: Kestrel asked
  a0403ec ckpt 1458: AF1 diagnostics built (not yet compiled/shipped): extension page log (blocked
  697f62b ckpt 1451: Owner message 20 (their Video lab / Tap lab / Decode lab numbers from the tab
  101b4b1 ckpt 1447: Owner message 19 (doraby.com: no photos, no search bar in Kestrel; Brave fine
  c0c149f ckpt 1443: alpha27 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha27 (gecko code 126), run 
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
