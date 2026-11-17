# CHECKPOINT 2628 — read me first, then TASKS.md

**Written:** 2026-11-17T12:26:33Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `588bc3ea` (this checkpoint is the commit after it)

## Just done
LAB5 PASS COMPLETE. alpha64 shipped 11:52Z (https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha64, code 165, run 44076463792 green, recorded in BUILDLOG.md). Proofs: JVM 1972+ tests, web 581, verify-apk (omni.ja stored, baseline.prof 5.9 KB), daily before/after (no regression, memory -2..-4 %, UI-thread engine-create 176 -> 125 ms), vp 11/12 + ip 3 scenes 31 rows 0 issues (the one vp failure = alpha62 loop breaker vs the lab's replayed clip, lab fix handreset), aot on the release build (baseline profile compiled 3.75 MB, warm start -12 % inside +-10 % noise; compile at install via base.dm WORKS on Android 10 = LAB6, needs the owner's go), sweep (nothing beyond noise). research/lab-baseline.json = the daily on the released commit (run 44077888907). All LAB3/LAB4/LAB5 rows ticked; LAB6 opened. No lab run is in flight; no heartbeat trigger remains (the send_later fired at 12:25Z, one-shot).

## Do this next
NOTHING RUNNING. Wait for the owner: (1) tablet test of alpha64: update, leave it charging overnight, send the Diagnostics lines 'start art', 'engine runtime-create-took', 'engine config-warm', 'storage purge-doomed'; (2) decide LAB6 (compile at install through the updater, see research/LAB5_PACKAGING.md); (3) decide the Neon idle pulse (off saves about 4 % of a core at idle); (4) tablet test of alpha62 (which site showed the advertisement). Next lab work only when the owner's Diagnostics ask for it.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  cda703be ckpt 2627: alpha64 SHIPPED + RECORDED (Release kestrel-g-v0.3.0-alpha64, run 4407646379
  e5e2e612 ckpt 2626: alpha64 DISPATCHED ONCE at 11:42Z: android.yml run 44076463792 (app=gecko fu
  d9efb312 ckpt 2625: gated kestrel-g-v0.3.0-alpha64 (gecko code 165) and pushed it: handoff self-
  4692623b ckpt 2624: RELEASE PREP (not shipped): versionCode 165 / versionName 0.3.0-alpha64 in g
  1c468757 ckpt 2623: SWEEP READ (run 44059232336, cancelled after the emulator went offline: 17 g
  bd981349 ckpt 2622: LAB5: OwnerRulesTest.lab5MeasuredPackagingProfileAndPrefsStay (33 tests in t
  c83d9f96 ckpt 2621: LAB5c batch 2 proven by tests: JVM suite 1972 tests / 0 failures (core + app
  41f2a308 ckpt 2620: LAB5a digests written (research/LAB5_PACKAGING, _STARTUP, _EXTENSION, _PREFS
  a55d8865 ckpt 2619: LAB5c-1 (first real app change, measured by a same-APK A/B): GeckoEngine.pre
  da746ada ckpt 2618: LAB5b: more lab tooling committed (no run yet): suite 'sweep' = tools/emu/pr
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
