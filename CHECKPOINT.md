# CHECKPOINT 342 — read me first, then TASKS.md

**Written:** 2026-11-11T08:43:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `d0256f0` (this checkpoint is the commit after it)

## Just done
P3+P4 DONE and verified on the emulator (blue REAPER theme: Home flames/scythe/glow field, glossy dock/sheets/cards, violet private skin, neon halo above the dock, selected-card glow, Settings 'Neon glow' Off/Steady/Subtle; stepped 10 Hz pulse on hardware layers, rests after 25 s idle; measured frames: 60 Hz anim 199/10 s vs 10 Hz 94/10 s vs resting 0). P1 research (research/HW_SPEED.md) done + safe fixes applied: shield thread normal priority, Io.fast lane, throttled post-load thumbnails. P2 research (research/ENGINES.md): stay on GeckoView, Cromite is a 232 MB standalone arm32 APK, not embeddable. Bugs found+fixed on the emulator: Home field kept portrait width in landscape (placement moved to onMeasure), empty address row after rotating landscape->portrait on Home, lint errors for the download service in the WebView module. N6 ticked. Gate (:core:test :app:testDebugUnitTest compile both lintDebug) green.

## Do this next
1) bump geckoapp/build.gradle.kts to versionCode 104 / 0.2.0-alpha5 + bash tools/sync-ext-version.sh; 2) bash ship.sh gecko '<note>' (gates, push); 3) dispatch android.yml via mcp__github__actions_run_trigger (workflow android.yml, ref ccr-a57c163e-mtq8js, inputs app=gecko full_build=true note=...); 4) wait (background until-loop), mcp__github__get_release_by_tag kestrel-g-v0.2.0-alpha5, then bash tools/record-release.sh kestrel-g-v0.2.0-alpha5 '<note>' ONLY after green; 5) tell the owner (Release link on its own line) what is NOT verified on the tablet; 6) delete heartbeat trigger trig_01HjJLZki7r2XgUqZazC4oup (mcp__claude-code-remote__delete_trigger) once alpha5 is recorded; later: P1d Speed lab, P1e memory profiles, startup transitional state polish (Home overlay with page-mode dock while the engine starts), Settings restyle, tab-grid swipe-to-close

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  bf1ed62 ckpt 316: P3/P4 neon wired and compiling (both modules): Neon engine (RenderThread alpha
  6438c0a ckpt 306: Owner request 4 recorded (P1-P4: hardware speed research, engine research incl
  8d8143c ckpt 301: N6 step 2 compiles: Android download shell (SqliteStore, MediaStoreSinks, UrlC
  c7112dc ckpt 294: N6 step 1 done: pure-Kotlin download engine in core/ (app.kestrel.core.downloa
  ce4c6e7 ckpt 281: handoff saved via context-handoff plugin (.handoff/handoff.md + json + archive
  8a1eac9 ckpt 278: alpha4 RELEASED + RECORDED: kestrel-g-v0.2.0-alpha4 (gecko code 103), run 4297
  c5df399 ckpt 275: gated kestrel-g-v0.2.0-alpha4 (gecko code 103) and pushed it: handoff self-tes
  276ef61 ckpt 266: N1 updater works end to end on the emulator (local feed): auto/manual check vi
  9470db6 ckpt 259: N2-N5 verified on emulator (Android 10 x86_64, 600x973): black Home + chamfere
  f6cb00e ckpt 252: N4/N5 code compiles: TabGrid (card grid with page previews, Tabs|Private segme
```

(25 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
