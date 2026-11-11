# CHECKPOINT 400 — read me first, then TASKS.md

**Written:** 2026-11-11T17:06:33Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `ef683ff` (this checkpoint is the commit after it)

## Just done
R1 neon DONE on the emulator (system animations off): pulse no longer gated on the animator scale, keyboard-hidden-but-focused no longer counts as typing, ring+bloom stronger (0.3..1.0), new pulsing ring around the address pill, canRest false on Home/grid/sheet (rest 2 min), tap hold 0.35 s, load hold 3 s, Diagnostics 'Neon glow:' line; docs/DESIGN.md updated

## Do this next
R3 pull-to-refresh (design in TASKS.md R3; KestrelGeckoView.onTouchEventForDetailResult), then R2 scroll work (read sb0-sb4 screenshots in scratchpad first), then alpha7 (106) ship: ship.sh gecko, dispatch android.yml, record-release; reply to owner incl. S1/S2

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  dec2a93 ckpt 392: S1+S2 code done: updater storage plan (core UpdateLogic.storagePlan + tests; U
  f0bff4b ckpt 384: Owner request 7 recorded (S1 in-app update blocked by INSTALL_FAILED_INSUFFICI
  fcf63d9 ckpt 382: Owner request 6 recorded in TASKS.md (R1 neon not visible, R2 scrolling stutte
  4ac5081 ckpt 376: alpha6 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha6, gecko code 105, run 430556
  69d37b2 ckpt 372: gated kestrel-g-v0.2.0-alpha6 (gecko code 105) and pushed it: handoff self-tes
  3c8f9fe ckpt 371: pre-ship: alpha6: a page that Android frees to save memory now comes back wher
  1d4be6f ckpt 369: Q2 work for alpha6 (code compiles, emulator-tested: Q2a recovery + Scroll test
  6339191 ckpt 352: alpha5 SHIPPED + RECORDED (run 42989432755 green, kestrel-g-v0.2.0-alpha5, APK
  e9f12cd ckpt 344: gated kestrel-g-v0.2.0-alpha5 (gecko code 104) and pushed it: handoff self-tes
  b84308a ckpt 342: P3+P4 DONE and verified on the emulator (blue REAPER theme: Home flames/scythe
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
