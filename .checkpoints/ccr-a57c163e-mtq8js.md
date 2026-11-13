# CHECKPOINT 1495 — read me first, then TASKS.md

**Written:** 2026-11-13T05:45:04Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `6caba9c` (this checkpoint is the commit after it)

## Just done
AH1c done and emulator-proven end to end (Shields master switch off -> Firefox's own tracking protection off for the site -> blocked probe script loads on the reload); AH1b code written but not yet compiled/tested: UpdateLogic.otherWayPlan (+2 tests with the owner's numbers), UpdateManager own footprint + other-way State fields, UpdatePanel NEEDS_SPACE rewrite (honest numbers, Clear cache, biggest downloads, other way guarded), Actions.clearCache/openDownloads in BrowserActivity, StorageInfo.cacheBytes

## Do this next
AH1b: compile (:core:test --tests UpdateLogicTest, :geckoapp:compileDebugKotlin), then see the NEEDS_SPACE sheet on the emulator with a fake releases.json whose update is far bigger than the free space (site/releases.json + the updater override), screenshot it; then pelisjuanita.com analysis (AH1e), then ship alpha29 only after AH1b-e are tested

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  939a120 ckpt 1487: AH1d done and emulator-proven: the Video line stayed on 'decoders unknown' be
  7d3044a ckpt 1482: Owner message 21 (their Diagnostics copy from the tablet, alpha27) written in
  29bfbb4 ckpt 1478: handoff saved (.handoff/handoff.md + archive 2026-11-13T05-01-56Z; CLAUDE.md/
  2e2935d ckpt 1476: alpha28 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha28 (gecko code 127), run 
  ec67869 ckpt 1474: gated kestrel-g-v0.3.0-alpha28 (gecko code 127) and pushed it: handoff self-t
  a562cab ckpt 1473: alpha28 gates green: JVM (core + app + gecko compile), 123 web tests, pref na
  a3a8e82 ckpt 1470: AF1 cause found + fixed + proven in real Gecko on the emulator: Kestrel asked
  a0403ec ckpt 1458: AF1 diagnostics built (not yet compiled/shipped): extension page log (blocked
  697f62b ckpt 1451: Owner message 20 (their Video lab / Tap lab / Decode lab numbers from the tab
  101b4b1 ckpt 1447: Owner message 19 (doraby.com: no photos, no search bar in Kestrel; Brave fine
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
