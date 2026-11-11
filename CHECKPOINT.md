# CHECKPOINT 376 — read me first, then TASKS.md

**Written:** 2026-11-11T16:17:00Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `36c6abc` (this checkpoint is the commit after it)

## Just done
alpha6 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha6, gecko code 105, run 43055646360 green, APK sha256 4741e7a0...). Owner request 5: Q1 answered in chat, Q2a-e shipped, Q3 verdict given. Contents: sessionGone restores SessionState behind RestoreOverlay; gecko-prefs v2 (size_factor 20); Settings > Scroll feel; Diagnostics > Scroll test + GPU line + pref read-back; Neon holds while touching; thumbnails wait for 3 s without touch. Emulator-verified (kill -9 content process => preview + same scroll position; Diagnostics shows config v2, size_factor=20, apz prefs change live; Scroll test prints a Result line). NOT seen on the tablet. No heartbeat exists (deleted).

## Do this next
WAIT FOR THE OWNER (Q2f): their answers: did alpha 3 scroll smoothly; Scroll test 'Result:' lines under Standard / Smoother / Neon Off; Diagnostics > Copy after a white screen; the Cromite 15-minute A/B if they want it. Then decide: B1-B3 defaults (apz multipliers, discard_factor), D1 in-extension blocking engine (research/ADBLOCK_ENGINE.md; removes the per-request native hop = lazy-load blanks + UI-thread messages), memory budget/profiles (research/HW_SPEED.md H6/H7), P5 cold-start state polish, tab-grid swipe-to-close, Settings restyle. If starting a long job: create a heartbeat (mcp__claude-code-remote__create_trigger, cron every 2 h, fresh session preferred) and put its id in the next ckpt.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  69d37b2 ckpt 372: gated kestrel-g-v0.2.0-alpha6 (gecko code 105) and pushed it: handoff self-tes
  3c8f9fe ckpt 371: pre-ship: alpha6: a page that Android frees to save memory now comes back wher
  1d4be6f ckpt 369: Q2 work for alpha6 (code compiles, emulator-tested: Q2a recovery + Scroll test
  6339191 ckpt 352: alpha5 SHIPPED + RECORDED (run 42989432755 green, kestrel-g-v0.2.0-alpha5, APK
  e9f12cd ckpt 344: gated kestrel-g-v0.2.0-alpha5 (gecko code 104) and pushed it: handoff self-tes
  b84308a ckpt 342: P3+P4 DONE and verified on the emulator (blue REAPER theme: Home flames/scythe
  bf1ed62 ckpt 316: P3/P4 neon wired and compiling (both modules): Neon engine (RenderThread alpha
  6438c0a ckpt 306: Owner request 4 recorded (P1-P4: hardware speed research, engine research incl
  8d8143c ckpt 301: N6 step 2 compiles: Android download shell (SqliteStore, MediaStoreSinks, UrlC
  c7112dc ckpt 294: N6 step 1 done: pure-Kotlin download engine in core/ (app.kestrel.core.downloa
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
