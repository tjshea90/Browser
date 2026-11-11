# CHECKPOINT 97 — read me first, then TASKS.md

**Written:** 2026-11-11T03:16:58Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `9233c81` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.2.0-alpha2 (gecko code 101) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"First GitHub-built Kestrel G: same code as alpha1, now built and signed by GitHub Actions"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.2.0-alpha2), then:  bash tools/record-release.sh kestrel-g-v0.2.0-alpha2 "First GitHub-built Kestrel G: same code as alpha1, now built and signed by GitHub Actions"
NEVER ship kestrel-g-v0.2.0-alpha2 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d41eb73 ckpt 94: Ported the owner's save system (Launcher/Portfolio) into Browser: tools/{ckpt,a
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
