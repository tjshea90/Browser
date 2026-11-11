# CHECKPOINT 406 — read me first, then TASKS.md

**Written:** 2026-11-11T17:18:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `df6fba4` (this checkpoint is the commit after it)

## Just done
R3 pull-down-to-refresh DONE + emulator-verified (KestrelGeckoView pull tracking via onTouchEventForDetailResult, PullChip, Settings toggle, Flavor.hasPullRefresh, docs/DESIGN.md); chip seen small+armed, release reloaded, mid-page pull-down scrolled without reload

## Do this next
R2 scroll smoothness: first read research/SCROLL.md B1-B5 + the emulator experiment screenshots sb0-sb4 in the scratchpad, then implement: apz content-response wait lower (now 400), passive touch listeners check via readBack, keep content for scroll-back, priority hint, speculative connect; then alpha7 (106) ship + reply to the owner (S1, S2, R1, R2, R3)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M docs/DESIGN.md

## Last ten checkpoints
```
  a2797c7 ckpt 400: R1 neon DONE on the emulator (system animations off): pulse no longer gated on
  dec2a93 ckpt 392: S1+S2 code done: updater storage plan (core UpdateLogic.storagePlan + tests; U
  f0bff4b ckpt 384: Owner request 7 recorded (S1 in-app update blocked by INSTALL_FAILED_INSUFFICI
  fcf63d9 ckpt 382: Owner request 6 recorded in TASKS.md (R1 neon not visible, R2 scrolling stutte
  4ac5081 ckpt 376: alpha6 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha6, gecko code 105, run 430556
  69d37b2 ckpt 372: gated kestrel-g-v0.2.0-alpha6 (gecko code 105) and pushed it: handoff self-tes
  3c8f9fe ckpt 371: pre-ship: alpha6: a page that Android frees to save memory now comes back wher
  1d4be6f ckpt 369: Q2 work for alpha6 (code compiles, emulator-tested: Q2a recovery + Scroll test
  6339191 ckpt 352: alpha5 SHIPPED + RECORDED (run 42989432755 green, kestrel-g-v0.2.0-alpha5, APK
  e9f12cd ckpt 344: gated kestrel-g-v0.2.0-alpha5 (gecko code 104) and pushed it: handoff self-tes
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
