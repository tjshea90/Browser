# CHECKPOINT 384 — read me first, then TASKS.md

**Written:** 2026-11-11T16:28:19Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `1315fca` (this checkpoint is the commit after it)

## Just done
Owner request 7 recorded (S1 in-app update blocked by INSTALL_FAILED_INSUFFICIENT_STORAGE although 900 MB were freed; S2 'MB downloaded of total' on every download card). These come BEFORE R1-R3 because the owner cannot update to alpha6 at all. Scroll-away/back emulator experiment (R2a) finished in the background: screenshots sb0-sb4 in the scratchpad, not yet read.

## Do this next
1) read UpdateManager.kt / UpdatePanel.kt / InstallResultReceiver.kt: where the APK is stored, how the failure state is kept, what Check now does after a failure; 2) S1: free-space estimate via StorageManager.getAllocatableBytes + need = apk*2.5; show both numbers; Try again from the downloaded file; delete our APK copy after commit success; human error text for INSUFFICIENT_STORAGE; emulator test by filling /data (adb root) and installing; 3) S2: core/download/Planner.kt Format.status + DownloadsScreen card: 'X MB of Y MB'; update EngineTest/PlannerTest; 4) then R1 neon, R3 pull-to-refresh, R2 scrolling; 5) alpha7 (106) ship; the owner must be told how to get past the storage failure NOW (Settings > Storage, or Kestrel G > Clear cache, then Check now) in the reply

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  fcf63d9 ckpt 382: Owner request 6 recorded in TASKS.md (R1 neon not visible, R2 scrolling stutte
  4ac5081 ckpt 376: alpha6 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha6, gecko code 105, run 430556
  69d37b2 ckpt 372: gated kestrel-g-v0.2.0-alpha6 (gecko code 105) and pushed it: handoff self-tes
  3c8f9fe ckpt 371: pre-ship: alpha6: a page that Android frees to save memory now comes back wher
  1d4be6f ckpt 369: Q2 work for alpha6 (code compiles, emulator-tested: Q2a recovery + Scroll test
  6339191 ckpt 352: alpha5 SHIPPED + RECORDED (run 42989432755 green, kestrel-g-v0.2.0-alpha5, APK
  e9f12cd ckpt 344: gated kestrel-g-v0.2.0-alpha5 (gecko code 104) and pushed it: handoff self-tes
  b84308a ckpt 342: P3+P4 DONE and verified on the emulator (blue REAPER theme: Home flames/scythe
  bf1ed62 ckpt 316: P3/P4 neon wired and compiling (both modules): Neon engine (RenderThread alpha
  6438c0a ckpt 306: Owner request 4 recorded (P1-P4: hardware speed research, engine research incl
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
