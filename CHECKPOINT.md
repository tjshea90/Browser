# CHECKPOINT 369 — read me first, then TASKS.md

**Written:** 2026-11-11T16:00:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `97f3933` (this checkpoint is the commit after it)

## Just done
Q2 work for alpha6 (code compiles, emulator-tested: Q2a recovery + Scroll test tool): sessionGone restores SessionState (scroll kept) + RestoreOverlay (dimmed preview, 'Page freed to save memory - bringing it back') + Health recover-after-kill/crash with memFacts + per-level trim counters; Neon holds still while a finger is down +1.5 s; post-load thumbnail capture needs 3 s without touch; Diagnostics 'Scroll test' (assets/scrolltest, ScrollTest.kt: 32 real-touch swipes, compositor-frame stats + verdict); research/SCROLL.md (agent, 145 lines) CORRECTS my belief: surface cache cap = RAM/size_factor(4) = ~475 MB, max_size_kb is ignored on 32-bit => gecko-prefs.txt v2 sets image.mem.surfacecache.size_factor=20 (~95 MB) and drops the dead max_size_kb line; Settings 'Scroll feel' (Prefs.scrollFeel, GeckoEngine.applyScrollFeel live apz.* prefs, Flavor.hasScrollFeel); GlInfo (EGL pbuffer GPU strings in Diagnostics); onFirstComposite/onPaintStatusReset logging.

## Do this next
1) wait for the x86_64 build (scratchpad build_x86.log), install, check on the emulator: Settings > Scroll feel row, Diagnostics shows config v2 / size_factor=20 / GPU line / pref read-backs, Scroll test prints the Result line; 2) fix TASKS.md Q2b text (it still says cap ~30 MB: WRONG, see research/SCROLL.md); 3) tick Q2a-e with evidence; 4) bump geckoapp to versionCode 105 / 0.2.0-alpha6 + tools/sync-ext-version.sh, bash ship.sh gecko '<note>', dispatch android.yml (ToolSearch select mcp__github__actions_run_trigger first), record-release only after green; 5) reply to the owner (Q1 Cromite answer was already sent mid-turn: summarize again briefly + alpha6 link + what to try: Settings > Scroll feel, Diagnostics > Scroll test, tell me the Result line + whether white screens now come back at the same place; ask: did alpha3 scroll smoothly?)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6339191 ckpt 352: alpha5 SHIPPED + RECORDED (run 42989432755 green, kestrel-g-v0.2.0-alpha5, APK
  e9f12cd ckpt 344: gated kestrel-g-v0.2.0-alpha5 (gecko code 104) and pushed it: handoff self-tes
  b84308a ckpt 342: P3+P4 DONE and verified on the emulator (blue REAPER theme: Home flames/scythe
  bf1ed62 ckpt 316: P3/P4 neon wired and compiling (both modules): Neon engine (RenderThread alpha
  6438c0a ckpt 306: Owner request 4 recorded (P1-P4: hardware speed research, engine research incl
  8d8143c ckpt 301: N6 step 2 compiles: Android download shell (SqliteStore, MediaStoreSinks, UrlC
  c7112dc ckpt 294: N6 step 1 done: pure-Kotlin download engine in core/ (app.kestrel.core.downloa
  ce4c6e7 ckpt 281: handoff saved via context-handoff plugin (.handoff/handoff.md + json + archive
  8a1eac9 ckpt 278: alpha4 RELEASED + RECORDED: kestrel-g-v0.2.0-alpha4 (gecko code 103), run 4297
  c5df399 ckpt 275: gated kestrel-g-v0.2.0-alpha4 (gecko code 103) and pushed it: handoff self-tes
```

(16 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
