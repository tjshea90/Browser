# CHECKPOINT 94 — read me first, then TASKS.md

**Written:** 2026-11-11T03:15:22Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `ea2d0ab` (this checkpoint is the commit after it)

## Just done
Ported the owner's save system (Launcher/Portfolio) into Browser: tools/{ckpt,autosave,push,unpushed,resume,install-hooks,posttool,capture_inbox,capture_outbox,secretscan,toobig,record-release,verify-apk,appinfo,test_handoff}.sh + ship.sh + bootstrap.sh, hooks switched in .claude/settings.json (old scripts/autosave.sh+resume.sh are compat shims), docs CLAUDE/HANDOFF/BRIEF/TASKS/BUILDLOG/README rewritten. New android.yml (check -> test-jvm/test-js -> build-web/build-gecko -> GitHub Release, tag created by GitHub; no push trigger) + .github/scripts/publish-release.sh + .github/actions/setup-build. Versions bumped for the first GitHub-built releases: web 0.1.1 (code 2), gecko 0.2.0-alpha2 (code 101; same code as alpha1). tools/test_handoff.sh 53/53 green. NOT yet run on GitHub.

## Do this next
1) Dispatch android.yml with app=both full_build=false (gates only, ~1 min): mcp__github__actions_run_trigger run_workflow workflow_id android.yml ref ccr-a57c163e-mtq8js inputs app=both full_build=false; read the result (actions_list list_workflow_runs resource_id android.yml event workflow_dispatch perPage 1 -> list_workflow_jobs -> get_job_logs) and fix any workflow error. 2) bash ship.sh gecko 'first GitHub-built Kestrel G, same code as alpha1' then dispatch app=gecko full_build=true note=...; same for web. 3) When green and the Release shows the APK: bash tools/record-release.sh <tag> '<note>', tick TASKS.md, give the owner the Release links (plain text, own line).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
    ?? CHECKPOINT.md

## Last ten checkpoints
```
```
