# CHECKPOINT 3266 — read me first, then TASKS.md

**Written:** 2026-11-24T20:23:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `72ceed92` (this checkpoint is the commit after it)

## Just done
OPT1: Auto-tune integrated (BenchServer, Store, Apply, Guard, restart trampoline, Runner, Activity, Settings row, gate exemption for 127.0.0.1, lab suite opt); core 2103 + app 508 tests green, web gate 29 + bench 8 green, :geckoapp compiles; staging folder removed. alpha106 release job failed ONLY at 'Publish the Release' with HTTP 403 (workflow permissions unchanged): rerun_failed_jobs asked once on run 45474823903

## Do this next
(1) Check run 45474823903 build-gecko after the rerun: if Publish the Release is green -> get_release_by_tag kestrel-g-v0.3.0-alpha106 -> bash tools/record-release.sh kestrel-g-v0.3.0-alpha106 <note from docs/handoff/ALPHA106_RELEASE_NOTE.txt, tr -d newline> -> tick FF1c. If it fails with 403 again: read GitHub repo Settings > Actions > General > Workflow permissions (the owner must allow Read and write) and tell the owner; do NOT dispatch a second run for the same tag. (2) dispatch the emulator lab: android.yml app=lab lab_suite=opt lab_profile=standard lab_net=full lab_minutes=5; read its result (soak report: auto-tune-quick row; opt_events); fix what it finds. (3) write docs/handoff/ALPHA107_RELEASE_NOTE.txt, bump 0.3.0-alpha107 / code 208, bash ship.sh gecko, ONE dispatch, record-release; (4) answer the owner (FF1 + OPT1).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
    D  docs/handoff/opt1-staging/apply_opt.py
    D  docs/handoff/opt1-staging/apply_opt2.py
    D  docs/handoff/opt1-staging/apply_opt3.py
    D  docs/handoff/opt1-staging/core/Report.kt
    D  docs/handoff/opt1-staging/gecko/BenchServer.kt
    D  docs/handoff/opt1-staging/gecko/OptimizerActivity.kt
    D  docs/handoff/opt1-staging/gecko/OptimizerApply.kt
    D  docs/handoff/opt1-staging/gecko/OptimizerGuard.kt
    D  docs/handoff/opt1-staging/gecko/OptimizerRunner.kt
    D  docs/handoff/opt1-staging/gecko/OptimizerStore.kt
    D  docs/handoff/opt1-staging/gecko/ProcessRestart.kt
    D  docs/handoff/opt1-staging/gecko/RestartActivity.kt
    D  docs/handoff/opt1-staging/tests/OptimizerPrefsTest.kt

## Last ten checkpoints
```
  59502d88 ckpt 3259: alpha106 (code 207) gated (ckpt 3257, head 0f14013b) and dispatched ONCE: ru
  0f14013b ckpt 3257: gated kestrel-g-v0.3.0-alpha106 (gecko code 207) and pushed it: handoff self
  c510bc41 ckpt 3256: OPT1: the Android-side sources and patch scripts are now saved IN the repo (
  f92c5435 ckpt 3255: OPT1: core done + 25 tests green, bench pages + web test green, tools/optimi
  59efade6 ckpt 3253: FF1: alpha106 prepared (code 207, GeckoView 161): lab smoke GREEN on 161 (ru
  29576308 ckpt 3230: FF1: GeckoView 161.0.20261118180411 + webcompat 161.0 pinned, clean compile,
  e7367846 ckpt 3222: wrote FF1 (new Firefox version into Kestrel, optimised for the hardware) and
  2e56492f ckpt 3221: alpha105 (code 206) SHIPPED: run 45352897624 green, Release kestrel-g-v0.3.0
  6fb30f29 ckpt 3219: alpha105 (code 206) gated (ckpt 3218, head 8b6eebf7) and dispatched ONCE: ru
  8b6eebf7 ckpt 3218: gated kestrel-g-v0.3.0-alpha105 (gecko code 206) and pushed it: handoff self
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
