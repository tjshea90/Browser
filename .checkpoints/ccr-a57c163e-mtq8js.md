# CHECKPOINT 316 — read me first, then TASKS.md

**Written:** 2026-11-11T08:01:50Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `38369f4` (this checkpoint is the commit after it)

## Just done
P3/P4 neon wired and compiling (both modules): Neon engine (RenderThread alpha pulses, rests after 25 s without touch, holds still while typing/loading/power-save/background), NeonBar (progress line + halo above the dock) in Gecko BrowserActivity, HomeView backdrop+field glow, selected tab card glow, Prefs.neon + Settings row 'Neon glow'. NOT yet looked at on the emulator.

## Do this next
1) build x86_64 debug APK (see emu.sh/HANDOFF), start emulator (nohup /opt/android-sdk/emulator/emulator -avd kestrel29x64 -no-window -no-audio -no-snapshot -no-boot-anim -gpu swiftshader_indirect -no-accel -memory 2048), screenshot Home/dock/menu/grid/downloads in blue + private skin, check halo clipping and the glow positions; 2) measure pulse cost (gfxinfo/top) on vs off and write it in docs/DESIGN.md; 3) collect research agents ae5afbdba939d0545 (HW_SPEED) and a72105811b72b7120 (ENGINES) outputs; 4) N6 re-test leaveDownloadTab, bump geckoapp 104/0.2.0-alpha5 + tools/sync-ext-version.sh, ship.sh gecko, dispatch android.yml; heartbeat trig_01HjJLZki7r2XgUqZazC4oup

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6438c0a ckpt 306: Owner request 4 recorded (P1-P4: hardware speed research, engine research incl
  8d8143c ckpt 301: N6 step 2 compiles: Android download shell (SqliteStore, MediaStoreSinks, UrlC
  c7112dc ckpt 294: N6 step 1 done: pure-Kotlin download engine in core/ (app.kestrel.core.downloa
  ce4c6e7 ckpt 281: handoff saved via context-handoff plugin (.handoff/handoff.md + json + archive
  8a1eac9 ckpt 278: alpha4 RELEASED + RECORDED: kestrel-g-v0.2.0-alpha4 (gecko code 103), run 4297
  c5df399 ckpt 275: gated kestrel-g-v0.2.0-alpha4 (gecko code 103) and pushed it: handoff self-tes
  276ef61 ckpt 266: N1 updater works end to end on the emulator (local feed): auto/manual check vi
  9470db6 ckpt 259: N2-N5 verified on emulator (Android 10 x86_64, 600x973): black Home + chamfere
  f6cb00e ckpt 252: N4/N5 code compiles: TabGrid (card grid with page previews, Tabs|Private segme
  bf260ea ckpt 246: N2/N3 part 1 compiles: CERE dark palette+angular icon set, ChamferDrawable, Bo
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
