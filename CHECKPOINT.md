# CHECKPOINT 2312 — read me first, then TASKS.md

**Written:** 2026-11-14T19:23:30Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `feef7ff` (this checkpoint is the commit after it)

## Just done
AY1, AY3, AY4 ticked with evidence (emulator proof of long press matrix, pick-up drag, grip tap close, space slide in page field and address bar, busy page no dialog); content.js selection before/after + selected-text rects; .pdf uses the fallback detector; research/MINI_NAV.md AY section; web-tests/openspace.test.js (10 pass)

## Do this next
Background script running full gates: core:test app:testDebugUnitTest geckoapp:testDebugUnitTest (log scratchpad full.log), lint, then x86_64 debug APK (apk2.log); when 'APK EXIT 0': adb install -r the new APK, rerun a quick smoke (long press blank -> panel; link -> menu; text select then blank press -> panel), tick AY2b/AY2c via claim.sh AY2 --done, HANDOFF.md entry, then AY5: bump geckoapp/build.gradle.kts to versionCode 141 / 0.3.0-alpha40, bash ship.sh gecko note, dispatch android.yml, wait, record-release, answer owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ca5c468 ckpt 2300: AY2+AY4 coded and unit-tested: MiniNav.showAt, PanelLayout long press pick-up
  bad447a ckpt 2287: AY1 coded+guarded; AY2 compiled (content.js contextmenu classifier -> backgro
  9a31c5a ckpt 2282: AY1 code done (askSlowScript answers CONTINUE with no dialog, Health counter 
  1eb7220 ckpt 2270: owner message 39 written into TASKS (AY1 no slow-page prompt, AY2 long press 
  44a36fb ckpt 2266: alpha39 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha39 (gecko code 140, run 
  418e420 ckpt 2260: alpha39 DISPATCHED (AX1: glide shapes + export/import): android.yml run_workf
  db0854d ckpt 2259: gated kestrel-g-v0.3.0-alpha39 (gecko code 140) and pushed it: handoff self-t
  4c766a1 ckpt 2258: pre-ship: alpha39 (includes alpha38): The keyboard now learns the SHAPE of yo
  899e135 ckpt 2244: AX1: remembered-word injection added (the lab found that for words seen 6+ ti
  54435a1 ckpt 2232: AX1 core built and tested: Fingerprint + PatternMemory (core/input/glide/Patt
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
