# CHECKPOINT 259 — read me first, then TASKS.md

**Written:** 2026-11-11T06:18:33Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `7e8e0f6` (this checkpoint is the commit after it)

## Just done
N2-N5 verified on emulator (Android 10 x86_64, 600x973): black Home + chamfered field, keyboard handling (KeyboardProbe popup on API29), dock lift above keyboard + suggestions, menu/Shields bottom sheets, tab grid with live previews, private pane + violet skin, landscape one-row dock; fixed: grid cards built post-layout, root focus-highlight tint, Home field cleared. docs/DESIGN.md written

## Do this next
Run :app tests (Robolectric) in background; write updater N1 (core UpdateLogic + tests, app UpdateChecker/ApkDownloader/Installer, manifest REQUEST_INSTALL_PACKAGES, sheet UI + menu tile + dock dot); then bump 0.2.0-alpha4/code 103, ship.sh gecko, dispatch android.yml

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f6cb00e ckpt 252: N4/N5 code compiles: TabGrid (card grid with page previews, Tabs|Private segme
  bf260ea ckpt 246: N2/N3 part 1 compiles: CERE dark palette+angular icon set, ChamferDrawable, Bo
  9458fe0 ckpt 227: Owner request 3 recorded in TASKS.md (N1-N7); owner likes alpha3; UI-1 answere
  04080cd ckpt 224: Alpha3 (kestrel-g-v0.2.0-alpha3, run 42961586675) built, signed, released and 
  1ab978e ckpt 221: ship.sh gecko OK; dispatched android.yml app=gecko full_build=true: run 429615
  85eab84 ckpt 220: gated kestrel-g-v0.2.0-alpha3 (gecko code 102) and pushed it: handoff self-tes
  57a6d62 ckpt 219: alpha3 candidate: freeze fixes verified on emulator (worker bridge, cosmetics 
  04dab0c ckpt 214: A2/A4 part: gecko-prefs.txt -> YAML config (AV1 off, memory/cache budget, no a
  1739d76 ckpt 209: A3 evidence layer: core RingLog (+tests), app Health (watchdog, frame jank, re
  9c46642 ckpt 175: A3 P0 part 1 (Kestrel G): GeckoView attached ONCE (ntp/error as overlays, setS
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
