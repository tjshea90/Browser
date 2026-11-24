# CHECKPOINT 3268 — read me first, then TASKS.md

**Written:** 2026-11-24T20:41:13Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `4bddd81e` (this checkpoint is the commit after it)

## Just done
alpha106 release publish fails with HTTP 403 twice (token has contents: write, GitHub status clean, earlier releases after workflow edits succeeded): added a one-minute probe job to android.yml (app=lab, lab_suite=probe) that reads the repo's Actions permissions / rulesets / tag protection and tries draft + real release and tag creation, then removes them

## Do this next
Dispatch android.yml app=lab lab_suite=probe full_build=false on this branch; read the probe job log (get_job_logs, tail 200): the lines '== gh release create ...' with exit codes say which variant works (target sha vs branch, draft vs real) and the 'actions/permissions/workflow' line says default_workflow_permissions. Then fix: either change publish-release.sh to the variant that works, or tell the owner the exact setting (Settings > Actions > General > Workflow permissions = Read and write; Settings > Rules; Settings > Tags protection). Then redo alpha106: bash ship.sh is NOT needed again for the same commit content but the release run must be re-dispatched ONCE (run 45474823903 cannot publish): check BUILDLOG.md has no alpha106 line first (record-release was never run). Also the lab run 45480427478 (suite opt) is in flight: read its report when done.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  8caaa5ba ckpt 3266: OPT1: Auto-tune integrated (BenchServer, Store, Apply, Guard, restart trampo
  59502d88 ckpt 3259: alpha106 (code 207) gated (ckpt 3257, head 0f14013b) and dispatched ONCE: ru
  0f14013b ckpt 3257: gated kestrel-g-v0.3.0-alpha106 (gecko code 207) and pushed it: handoff self
  c510bc41 ckpt 3256: OPT1: the Android-side sources and patch scripts are now saved IN the repo (
  f92c5435 ckpt 3255: OPT1: core done + 25 tests green, bench pages + web test green, tools/optimi
  59efade6 ckpt 3253: FF1: alpha106 prepared (code 207, GeckoView 161): lab smoke GREEN on 161 (ru
  29576308 ckpt 3230: FF1: GeckoView 161.0.20261118180411 + webcompat 161.0 pinned, clean compile,
  e7367846 ckpt 3222: wrote FF1 (new Firefox version into Kestrel, optimised for the hardware) and
  2e56492f ckpt 3221: alpha105 (code 206) SHIPPED: run 45352897624 green, Release kestrel-g-v0.3.0
  6fb30f29 ckpt 3219: alpha105 (code 206) gated (ckpt 3218, head 8b6eebf7) and dispatched ONCE: ru
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
