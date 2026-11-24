# CHECKPOINT 3253 — read me first, then TASKS.md

**Written:** 2026-11-24T19:37:55Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `8959300b` (this checkpoint is the commit after it)

## Just done
FF1: alpha106 prepared (code 207, GeckoView 161): lab smoke GREEN on 161 (run 45467963575), core optimizer tests 25 green, bench page tests 8 green, check-gecko-prefs 85 names 0 missing; OPT1 core (Metrics, Catalog, Decide, Config, RunState, Machine) + bench pages + web test written and green

## Do this next
Run bash ship.sh gecko with the note in docs/handoff/ALPHA106_RELEASE_NOTE.txt (background), ONE dispatch (app=gecko full_build=true, same note), poll the run, get_release_by_tag kestrel-g-v0.3.0-alpha106, record-release with the identical note, tick FF1c. Then OPT1c: Android side (BenchServer loopback, OptimizerActivity + runner, ProcessRestart/RestartActivity, OptimizerGuard before the engine config, Prefs optPrefs + generic apply, Settings row, LiveDiag.sendFile, Catalog change application), OPT1d digest tool tools/optimizer_digest.py, lab suite opt.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M geckoapp/build.gradle.kts
     M geckoapp/src/main/assets/kestrel-shields/manifest.json
    ?? docs/handoff/ALPHA106_RELEASE_NOTE.txt

## Last ten checkpoints
```
  29576308 ckpt 3230: FF1: GeckoView 161.0.20261118180411 + webcompat 161.0 pinned, clean compile,
  e7367846 ckpt 3222: wrote FF1 (new Firefox version into Kestrel, optimised for the hardware) and
  2e56492f ckpt 3221: alpha105 (code 206) SHIPPED: run 45352897624 green, Release kestrel-g-v0.3.0
  6fb30f29 ckpt 3219: alpha105 (code 206) gated (ckpt 3218, head 8b6eebf7) and dispatched ONCE: ru
  8b6eebf7 ckpt 3218: gated kestrel-g-v0.3.0-alpha105 (gecko code 206) and pushed it: handoff self
  c45aae47 ckpt 3217: PERF25: app unit tests green; bumped to alpha105 / code 206, release note do
  512c9e18 ckpt 3215: PERF25c: built the hardware drawing trial (core DrawTrial + DrawTrialTest + 
  6d20d18a ckpt 3203: wrote PERF25 (owner: Brave is smooth on the parlayapi site) into TASKS.md, n
  f3c4d330 ckpt 3202: alpha104 (code 205) SHIPPED: run 45346018174 green, Release kestrel-g-v0.3.0
  e168b9a7 ckpt 3199: alpha104 (code 205) gated (ckpt 3198, head 96feacef) and dispatched ONCE: ru
```

(22 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
