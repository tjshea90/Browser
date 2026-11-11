# CHECKPOINT 98 — read me first, then TASKS.md

**Written:** 2026-11-11T03:18:09Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `cdbefdf` (this checkpoint is the commit after it)

## Just done
ship.sh gecko OK (local gates green, pushed). Dispatched android.yml app=both full_build=true: run 42951742468 on cdbefdf, started 03:17Z.

## Do this next
Wait for run 42951742468 (check, then test-jvm/test-js, then build-web/build-gecko; expect 15-25 min): actions_list list_workflow_jobs resource_id 42951742468 (step status is live; get_job_logs 404s until a job ENDS). GREEN: get_release_by_tag kestrel-v0.1.1 and kestrel-g-v0.2.0-alpha2 show the APK -> bash tools/record-release.sh <tag> <note> for each -> tick TASKS.md -> give the owner both Release links on their own lines. RED: read the failing job log tail, fix, dispatch again (equal versionCode = rebuild, allowed).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  cdbefdf ckpt 97: gated kestrel-g-v0.2.0-alpha2 (gecko code 101) and pushed it: handoff self-test
  d41eb73 ckpt 94: Ported the owner's save system (Launcher/Portfolio) into Browser: tools/{ckpt,a
```
