# CHECKPOINT 2300 — read me first, then TASKS.md

**Written:** 2026-11-14T19:01:34Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `5f83d67` (this checkpoint is the commit after it)

## Just done
AY2+AY4 coded and unit-tested: MiniNav.showAt, PanelLayout long press pick-up (400 ms, not on keys) + drag, grip tap closes, no snap-dock, lifted outline; content.js open-space classifier with web-tests/openspace.test.js (8 pass); detector skips Home field/error buttons/panel; Settings texts; MiniNavTest+HotCornerTest green

## Do this next
Rebuild x86_64 debug APK (./gradlew :geckoapp:assembleDebug -PgeckoAbi=x86_64 in background, poll log for EXIT), install on emulator-5554 (start emulator if gone), emulator proof: presslab long presses (blank/link/text/image/field) -> panel only on blank; long press panel + drag; grip tap closes; space slide in address bar + page field; busy page (slow script) no dialog; then research/MINI_NAV.md AY2-AY4 section, HANDOFF entry, tick AY1-AY4 with claim.sh --done, ship alpha40 (versionCode 141)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  bad447a ckpt 2287: AY1 coded+guarded; AY2 compiled (content.js contextmenu classifier -> backgro
  9a31c5a ckpt 2282: AY1 code done (askSlowScript answers CONTINUE with no dialog, Health counter 
  1eb7220 ckpt 2270: owner message 39 written into TASKS (AY1 no slow-page prompt, AY2 long press 
  44a36fb ckpt 2266: alpha39 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha39 (gecko code 140, run 
  418e420 ckpt 2260: alpha39 DISPATCHED (AX1: glide shapes + export/import): android.yml run_workf
  db0854d ckpt 2259: gated kestrel-g-v0.3.0-alpha39 (gecko code 140) and pushed it: handoff self-t
  4c766a1 ckpt 2258: pre-ship: alpha39 (includes alpha38): The keyboard now learns the SHAPE of yo
  899e135 ckpt 2244: AX1: remembered-word injection added (the lab found that for words seen 6+ ti
  54435a1 ckpt 2232: AX1 core built and tested: Fingerprint + PatternMemory (core/input/glide/Patt
  2383c78 ckpt 2206: alpha38 RELEASED AND RECORDED (run 43616372537 green, tag kestrel-g-v0.3.0-al
```

(12 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
