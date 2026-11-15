# CHECKPOINT 2431 — read me first, then TASKS.md

**Written:** 2026-11-15T21:15:53Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `70e3a83f` (this checkpoint is the commit after it)

## Just done
lab run 43760546128 (fsy, first clean run, alpha51 code) read: Keep mode puts the page between the bars and every turn is clean (pause + cover + settle, one size change, turn.js extra resize), sound stops when leaving; FOUND: the engine sizes a page that goes full screen to the DISPLAY height (page innerHeight 956 css = 1024 px) while the view is 947 px, until the next real size change (cuts the bottom controls); FB4 written into TASKS (page reports its viewport, app corrects with a 1 px size change)

## Do this next
NEXT: code FB4: turn.js tellViewport (top frame, fullscreen, 350 ms debounce, k vpt w h f), background.js relay vpt, ShieldsBridge case vpt + constructor param viewport, BrowserActivity.onPageViewport/resyncEngineSize/vpUndo + Diagnostics line + reset at fullscreen entry/exit + onDestroy, tests (turn.test.js: report message; OwnerRulesTest guard), lab report columns (viewport lines), then alpha52

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  e9464a7e ckpt 2430: alpha51 gated and queued (FB1 bars kept by default + Settings row, FB2/FB3 t
  38a628d2 ckpt 2429: gated kestrel-g-v0.3.0-alpha51 (gecko code 152) and pushed it: handoff self-
  7c27ad91 ckpt 2428: pre-ship: alpha51: full-screen video, second attempt after alpha50 changed n
  88fb79ef ckpt 2427: FB1 + FB2 + FB3 coded: system bars kept by default (Prefs.fullscreenBars, Se
  691a247e ckpt 2426: owner message 47 written into TASKS.md (alpha50 changed nothing on the table
  d941af5c ckpt 2425: alpha50 shipped and recorded (Release kestrel-g-v0.3.0-alpha50, run 43758859
  4415fbaf ckpt 2424: lab run 43753283814 (fsy) read: it says NOTHING about the full-screen code: 
  6689210f ckpt 2423: gated kestrel-g-v0.3.0-alpha50 (gecko code 151) and pushed it: handoff self-
  ca8bc3fb ckpt 2422: pre-ship: alpha50: fixes from an independent check of alpha49 (everything el
  342c62e0 ckpt 2421: compat run 1 (43747605584) read: only 4 sites measured, the emulated phone's
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
