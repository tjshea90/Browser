# CHECKPOINT 418 — read me first, then TASKS.md

**Written:** 2026-11-11T17:52:04Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `fd3136e` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.2.0-alpha7 (gecko code 106) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha7: the in-app update now says exactly how much space to free (fixes the stuck 'insufficient storage' message) and retries from the file it already downloaded; download cards show 'MB of MB'; the neon glow visibly pulses (even with system animations off) incl. a ring around the address bar; pull down to refresh a page; scrolling: Smoother is the new default (paints about twice the screen around your finger, kept longer, shorter touch wait) + Diagnostics > Compare 3 feels"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.2.0-alpha7), then:  bash tools/record-release.sh kestrel-g-v0.2.0-alpha7 "alpha7: the in-app update now says exactly how much space to free (fixes the stuck 'insufficient storage' message) and retries from the file it already downloaded; download cards show 'MB of MB'; the neon glow visibly pulses (even with system animations off) incl. a ring around the address bar; pull down to refresh a page; scrolling: Smoother is the new default (paints about twice the screen around your finger, kept longer, shorter touch wait) + Diagnostics > Compare 3 feels"
NEVER ship kestrel-g-v0.2.0-alpha7 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  903dc4e ckpt 413: R2 first pass DONE on the emulator: Gecko source says display-port multipliers
  6cbe9d9 ckpt 406: R3 pull-down-to-refresh DONE + emulator-verified (KestrelGeckoView pull tracki
  a2797c7 ckpt 400: R1 neon DONE on the emulator (system animations off): pulse no longer gated on
  dec2a93 ckpt 392: S1+S2 code done: updater storage plan (core UpdateLogic.storagePlan + tests; U
  f0bff4b ckpt 384: Owner request 7 recorded (S1 in-app update blocked by INSTALL_FAILED_INSUFFICI
  fcf63d9 ckpt 382: Owner request 6 recorded in TASKS.md (R1 neon not visible, R2 scrolling stutte
  4ac5081 ckpt 376: alpha6 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha6, gecko code 105, run 430556
  69d37b2 ckpt 372: gated kestrel-g-v0.2.0-alpha6 (gecko code 105) and pushed it: handoff self-tes
  3c8f9fe ckpt 371: pre-ship: alpha6: a page that Android frees to save memory now comes back wher
  1d4be6f ckpt 369: Q2 work for alpha6 (code compiles, emulator-tested: Q2a recovery + Scroll test
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
