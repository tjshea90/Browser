# CHECKPOINT 2320 — read me first, then TASKS.md

**Written:** 2026-11-14T19:36:30Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `ce077bc` (this checkpoint is the commit after it)

## Just done
AY2 final: content.js treats only a NEW non-empty selection as text (emulator found: the engine clears an old selection on a blank press), full-screen wrappers do not block, neon glow released when a swallowed touch ends, OwnerRulesTest guards for AY2-AY4, web-tests/openspace.test.js 12 pass, gates green (core+app+geckoapp tests, lint)

## Do this next
Rebuild x86_64 debug APK (assets changed: content.js huge()) and re-smoke blank press + text press on the emulator (presslab on port 8002 + adb reverse), then bash tools/claim.sh AY2 --done with evidence and tick AY2b/AY2c boxes by hand, then AY5: versionCode 141 / 0.3.0-alpha40 in geckoapp/build.gradle.kts, bash ship.sh gecko note, dispatch android.yml (app=gecko full_build=true ref ccr-a57c163e-mtq8js), wait tag+run, record-release, answer owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M research/MINI_NAV.md

## Last ten checkpoints
```
  097f61f ckpt 2312: AY1, AY3, AY4 ticked with evidence (emulator proof of long press matrix, pick
  ca5c468 ckpt 2300: AY2+AY4 coded and unit-tested: MiniNav.showAt, PanelLayout long press pick-up
  bad447a ckpt 2287: AY1 coded+guarded; AY2 compiled (content.js contextmenu classifier -> backgro
  9a31c5a ckpt 2282: AY1 code done (askSlowScript answers CONTINUE with no dialog, Health counter 
  1eb7220 ckpt 2270: owner message 39 written into TASKS (AY1 no slow-page prompt, AY2 long press 
  44a36fb ckpt 2266: alpha39 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha39 (gecko code 140, run 
  418e420 ckpt 2260: alpha39 DISPATCHED (AX1: glide shapes + export/import): android.yml run_workf
  db0854d ckpt 2259: gated kestrel-g-v0.3.0-alpha39 (gecko code 140) and pushed it: handoff self-t
  4c766a1 ckpt 2258: pre-ship: alpha39 (includes alpha38): The keyboard now learns the SHAPE of yo
  899e135 ckpt 2244: AX1: remembered-word injection added (the lab found that for words seen 6+ ti
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
