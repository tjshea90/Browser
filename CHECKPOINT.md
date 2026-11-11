# CHECKPOINT 392 — read me first, then TASKS.md

**Written:** 2026-11-11T16:46:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `7ff59eb` (this checkpoint is the commit after it)

## Just done
S1+S2 code done: updater storage plan (core UpdateLogic.storagePlan + tests; UpdateManager.space/needSpace/recheckSpace/installNeedsSpace with a rising floor, abandon leftover sessions, drop own APK copy only when needed, mapped installer failures), Updates sheet NEEDS_SPACE screen (numbers, Try again, Storage settings), download cards show 'X MB of Y MB'. Emulator-verified with a filled /data (pre-download check, auto-clear after freeing space, cancel keeps file, simulated STATUS_FAILURE_STORAGE). The 'floor' variant (ask 150 MB more than Android's own refusal) compiled but not yet run.

## Do this next
1) R1 neon (drop areAnimatorsEnabled gate, stronger pulse, more places, Home/tab grid never rest, Diagnostics line); 2) R3 pull-to-refresh; 3) R2 scroll work (emulator evidence first: sb0-sb4 screenshots in scratchpad were never read; extend readBack with candidate prefs; apz wait; speculative connect; priority hint; live-session policy); 4) rebuild debug, re-run the STORAGE broadcast test (expect 'about 150 MB more') + check download card text on a Range-server download; 5) alpha7 (106): ship.sh gecko, dispatch android.yml, record-release; 6) reply to the owner: how to get past the storage failure NOW was already told; tell them the exact number logic + that they need Menu > Updates > Try again

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  f0bff4b ckpt 384: Owner request 7 recorded (S1 in-app update blocked by INSTALL_FAILED_INSUFFICI
  fcf63d9 ckpt 382: Owner request 6 recorded in TASKS.md (R1 neon not visible, R2 scrolling stutte
  4ac5081 ckpt 376: alpha6 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha6, gecko code 105, run 430556
  69d37b2 ckpt 372: gated kestrel-g-v0.2.0-alpha6 (gecko code 105) and pushed it: handoff self-tes
  3c8f9fe ckpt 371: pre-ship: alpha6: a page that Android frees to save memory now comes back wher
  1d4be6f ckpt 369: Q2 work for alpha6 (code compiles, emulator-tested: Q2a recovery + Scroll test
  6339191 ckpt 352: alpha5 SHIPPED + RECORDED (run 42989432755 green, kestrel-g-v0.2.0-alpha5, APK
  e9f12cd ckpt 344: gated kestrel-g-v0.2.0-alpha5 (gecko code 104) and pushed it: handoff self-tes
  b84308a ckpt 342: P3+P4 DONE and verified on the emulator (blue REAPER theme: Home flames/scythe
  bf1ed62 ckpt 316: P3/P4 neon wired and compiling (both modules): Neon engine (RenderThread alpha
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
