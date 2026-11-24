# CHECKPOINT 3259 — read me first, then TASKS.md

**Written:** 2026-11-24T19:53:03Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `662dcb1c` (this checkpoint is the commit after it)

## Just done
alpha106 (code 207) gated (ckpt 3257, head 0f14013b) and dispatched ONCE: run 45474823903 (run 289, 19:52Z)

## Do this next
Poll RUN 45474823903 (mcp__github__actions_list list_workflow_jobs); flaky StreamEngineTest/PhoneBackendTest/ServerTest -> rerun_failed_jobs on the SAME run; when green get_release_by_tag kestrel-g-v0.3.0-alpha106 then bash tools/record-release.sh kestrel-g-v0.3.0-alpha106 with the identical note from docs/handoff/ALPHA106_RELEASE_NOTE.txt (tr -d newline); tick FF1c in TASKS. MEANWHILE (the run is pinned to head 0f14013b so new commits are safe): apply the optimizer integration from docs/handoff/opt1-staging (apply_opt.py, apply_opt2.py, apply_opt3.py), compile :geckoapp, fix errors, tests, then ship alpha107 (Auto-tune) after alpha106 is recorded. Never ship alpha106 twice.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  0f14013b ckpt 3257: gated kestrel-g-v0.3.0-alpha106 (gecko code 207) and pushed it: handoff self
  c510bc41 ckpt 3256: OPT1: the Android-side sources and patch scripts are now saved IN the repo (
  f92c5435 ckpt 3255: OPT1: core done + 25 tests green, bench pages + web test green, tools/optimi
  59efade6 ckpt 3253: FF1: alpha106 prepared (code 207, GeckoView 161): lab smoke GREEN on 161 (ru
  29576308 ckpt 3230: FF1: GeckoView 161.0.20261118180411 + webcompat 161.0 pinned, clean compile,
  e7367846 ckpt 3222: wrote FF1 (new Firefox version into Kestrel, optimised for the hardware) and
  2e56492f ckpt 3221: alpha105 (code 206) SHIPPED: run 45352897624 green, Release kestrel-g-v0.3.0
  6fb30f29 ckpt 3219: alpha105 (code 206) gated (ckpt 3218, head 8b6eebf7) and dispatched ONCE: ru
  8b6eebf7 ckpt 3218: gated kestrel-g-v0.3.0-alpha105 (gecko code 206) and pushed it: handoff self
  c45aae47 ckpt 3217: PERF25: app unit tests green; bumped to alpha105 / code 206, release note do
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
