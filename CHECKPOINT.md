# CHECKPOINT 3053 — read me first, then TASKS.md

**Written:** 2026-11-23T19:04:39Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `09eaca29` (this checkpoint is the commit after it)

## Just done
alpha96 (gecko code 197) gated by ship.sh (core + app + geckoapp suites, handoff 92, web tests) and ONE dispatch made on claude/kind-brahmagupta-ae3yl8 (android.yml app=gecko full_build=true); contents: PERF12 diagnostics (cut-short loads, page loop, quieter journal, page-seen, ext-version) + PERF13 Back (pagehide verdict left=, 9 s place retries, no Still loading box)

## Do this next
Next: wait for tag kestrel-g-v0.3.0-alpha96 (git ls-remote --tags origin) and all jobs success (mcp__github__actions_list list_workflow_jobs for the run on branch claude/kind-brahmagupta-ae3yl8), mcp__github__get_release_by_tag shows the APK, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha96 with the IDENTICAL note (scratchpad note96.txt), tick PERF12b/PERF13c [~] with the run id, answer the owner with the Release link on its own line. NEVER dispatch alpha96 twice.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  09eaca29 ckpt 3052: gated kestrel-g-v0.3.0-alpha96 (gecko code 197) and pushed it: handoff self-
  bd5370ef ckpt 3049: PERF12 + PERF13 coded and tested (targeted): cut-short loads, page loop, qui
  fab351e4 ckpt 3039: PERF13 written to TASKS.md (owner: instant first load and instant Back, find
  d41227d4 ckpt 3034: PERF12 written to TASKS.md (owner 2026-11-23: look at the latest diagnostics
  b375e29e ckpt 3033: PERF11a done + research written (alpha95 uploads: Safe drawing + PBO off app
  489d966d ckpt 3032: PERF11 written to TASKS.md (owner after alpha95: invisible blocks covering w
  eefcc152 ckpt 3031: alpha95 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha95 (gecko code 196, run
  f107edb6 ckpt 3029: alpha95 (gecko code 196) gated by ship.sh (core+app+geckoapp suites, handoff
  12cc7bbd ckpt 3028: gated kestrel-g-v0.3.0-alpha95 (gecko code 196) and pushed it: handoff self-
  36dea037 ckpt 3024: pre-ship: alpha95: half-drawn text, attaching photos, and the files permissi
```
