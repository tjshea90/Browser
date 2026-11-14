# CHECKPOINT 2270 — read me first, then TASKS.md

**Written:** 2026-11-14T18:21:13Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `d4a0028` (this checkpoint is the commit after it)

## Just done
owner message 39 written into TASKS (AY1 no slow-page prompt, AY2 long press anywhere on an open space opens the panel under the finger / link options for links, AY3 space-bar cursor drag, AY4 floating panel moved by long press + drag, AY5 ship); AX1 answer for alpha39 was given

## Do this next
START AY1: grep -rn 'onSlowScript\|This page is slow' geckoapp app --include=*.kt; answer GeckoResult.fromValue(SlowScriptResponse.CONTINUE) with no dialog, count it for Diagnostics, unit test; then AY2a: find out on the emulator (x86_64 debug APK, tools/emu/glide_emu.py, fieldlab/adlab pages) what a long press on blank space / a link / an image / text delivers (GeckoView onContextMenu fires only for links+media; test a content-script contextmenu listener in geckoapp/src/main/assets/kestrel-shields/content.js); read MiniNav.kt HotCorner/PanelLayout/KeysView touch code and BrowserActivity dispatchTouchEvent first

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  44a36fb ckpt 2266: alpha39 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha39 (gecko code 140, run 
  418e420 ckpt 2260: alpha39 DISPATCHED (AX1: glide shapes + export/import): android.yml run_workf
  db0854d ckpt 2259: gated kestrel-g-v0.3.0-alpha39 (gecko code 140) and pushed it: handoff self-t
  4c766a1 ckpt 2258: pre-ship: alpha39 (includes alpha38): The keyboard now learns the SHAPE of yo
  899e135 ckpt 2244: AX1: remembered-word injection added (the lab found that for words seen 6+ ti
  54435a1 ckpt 2232: AX1 core built and tested: Fingerprint + PatternMemory (core/input/glide/Patt
  2383c78 ckpt 2206: alpha38 RELEASED AND RECORDED (run 43616372537 green, tag kestrel-g-v0.3.0-al
  2d5a35b ckpt 2201: alpha38 DISPATCHED (AX2 fix: passkey API off + crash note): android.yml run_w
  da5124b ckpt 2200: gated kestrel-g-v0.3.0-alpha38 (gecko code 139) and pushed it: handoff self-t
  b72f9b2 ckpt 2199: pre-ship: alpha38 (includes alpha37): Fixes the app closing on some bank page
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
