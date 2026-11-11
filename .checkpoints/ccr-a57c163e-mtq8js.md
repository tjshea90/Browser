# CHECKPOINT 275 — read me first, then TASKS.md

**Written:** 2026-11-11T06:37:29Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `dfd3703` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.2.0-alpha4 (gecko code 103) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"New look (alpha 4): the whole navigation bar is at the bottom (address, reload, back, forward, Home, new tab, tabs, menu) and lifts above the keyboard without resizing the page; Home button and a black Home screen with one big search field (words search Google, addresses open); private tabs are separate from regular tabs and violet with a clear indicator; the tab switcher is a card grid with live page previews and a Tabs/Private switch; the menu and Shields open from the bottom; in-app updates (Menu > Updates). Downloads is still the old system screen: the new download manager comes next."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.2.0-alpha4), then:  bash tools/record-release.sh kestrel-g-v0.2.0-alpha4 "New look (alpha 4): the whole navigation bar is at the bottom (address, reload, back, forward, Home, new tab, tabs, menu) and lifts above the keyboard without resizing the page; Home button and a black Home screen with one big search field (words search Google, addresses open); private tabs are separate from regular tabs and violet with a clear indicator; the tab switcher is a card grid with live page previews and a Tabs/Private switch; the menu and Shields open from the bottom; in-app updates (Menu > Updates). Downloads is still the old system screen: the new download manager comes next."
NEVER ship kestrel-g-v0.2.0-alpha4 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  276ef61 ckpt 266: N1 updater works end to end on the emulator (local feed): auto/manual check vi
  9470db6 ckpt 259: N2-N5 verified on emulator (Android 10 x86_64, 600x973): black Home + chamfere
  f6cb00e ckpt 252: N4/N5 code compiles: TabGrid (card grid with page previews, Tabs|Private segme
  bf260ea ckpt 246: N2/N3 part 1 compiles: CERE dark palette+angular icon set, ChamferDrawable, Bo
  9458fe0 ckpt 227: Owner request 3 recorded in TASKS.md (N1-N7); owner likes alpha3; UI-1 answere
  04080cd ckpt 224: Alpha3 (kestrel-g-v0.2.0-alpha3, run 42961586675) built, signed, released and 
  1ab978e ckpt 221: ship.sh gecko OK; dispatched android.yml app=gecko full_build=true: run 429615
  85eab84 ckpt 220: gated kestrel-g-v0.2.0-alpha3 (gecko code 102) and pushed it: handoff self-tes
  57a6d62 ckpt 219: alpha3 candidate: freeze fixes verified on emulator (worker bridge, cosmetics 
  04dab0c ckpt 214: A2/A4 part: gecko-prefs.txt -> YAML config (AV1 off, memory/cache budget, no a
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
