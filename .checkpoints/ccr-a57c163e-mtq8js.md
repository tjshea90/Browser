# CHECKPOINT 2287 — read me first, then TASKS.md

**Written:** 2026-11-14T18:45:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `21b81cd` (this checkpoint is the commit after it)

## Just done
AY1 coded+guarded; AY2 compiled (content.js contextmenu classifier -> background os -> ShieldsBridge openSpace -> BrowserActivity.onOpenSpace -> MiniNav.showAt; hotCornerHost fallback zone = whole page when page cannot answer); AY3 space-slide coded+unit tests

## Do this next
AY4: MiniNav PanelLayout long press (not on keys) + drag moves the panel, grip tap closes, drop snap-dock; then MiniNavTest+HotCornerTest+web-tests for classifier; then rebuild x86_64 debug APK, emulator proof (blank/link/text/image long press, space slide, busy page no dialog); then ship alpha40 (versionCode 141)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  9a31c5a ckpt 2282: AY1 code done (askSlowScript answers CONTINUE with no dialog, Health counter 
  1eb7220 ckpt 2270: owner message 39 written into TASKS (AY1 no slow-page prompt, AY2 long press 
  44a36fb ckpt 2266: alpha39 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha39 (gecko code 140, run 
  418e420 ckpt 2260: alpha39 DISPATCHED (AX1: glide shapes + export/import): android.yml run_workf
  db0854d ckpt 2259: gated kestrel-g-v0.3.0-alpha39 (gecko code 140) and pushed it: handoff self-t
  4c766a1 ckpt 2258: pre-ship: alpha39 (includes alpha38): The keyboard now learns the SHAPE of yo
  899e135 ckpt 2244: AX1: remembered-word injection added (the lab found that for words seen 6+ ti
  54435a1 ckpt 2232: AX1 core built and tested: Fingerprint + PatternMemory (core/input/glide/Patt
  2383c78 ckpt 2206: alpha38 RELEASED AND RECORDED (run 43616372537 green, tag kestrel-g-v0.3.0-al
  2d5a35b ckpt 2201: alpha38 DISPATCHED (AX2 fix: passkey API off + crash note): android.yml run_w
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
