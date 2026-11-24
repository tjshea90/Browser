# CHECKPOINT 3184 — read me first, then TASKS.md

**Written:** 2026-11-24T05:30:56Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `3596f285` (this checkpoint is the commit after it)

## Just done
alpha103 (code 204) gated (ckpt 3183, head 3596f285) and dispatched ONCE: run 45333801406 (05:30Z)

## Do this next
Poll RUN 45333801406 (mcp__github__actions_list list_workflow_jobs resource_id 45333801406) until completed; if test-jvm fails, read mcp__github__get_job_logs job_id (the failure message now prints) and if it is a known flaky test rerun_failed_jobs run_id 45333801406 (never a new dispatch); when green get_release_by_tag kestrel-g-v0.3.0-alpha103, bash tools/record-release.sh kestrel-g-v0.3.0-alpha103 with the identical note from docs/handoff/ALPHA103_RELEASE_NOTE.txt; mark PERF22c done in TASKS.md; answer the owner (PERF22 + FLK1) with the Release link, NOT run on the tablet, open items (PERF17c/d fixes wait for alpha100+ numbers, PERF19 blocker + PERF23, PERF14e, PERF9, BackLab MISSED false positive). Never ship alpha103 again; next is alpha104 / versionCode 205

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  3596f285 ckpt 3183: gated kestrel-g-v0.3.0-alpha103 (gecko code 204) and pushed it: handoff self
  cb169fe5 ckpt 3182: pre-ship: alpha103: the deep low-end research, three speed experiments, and 
  ee5eb3f1 ckpt 3179: PERF22: research/LOW_END_PLAYBOOK.md written; three Network lab experiments 
  3b1d4574 ckpt 3175: FLK1 done: real-time waits tripled in StreamEngineTest, PhoneBackendTest, Se
  9e7ae1ef ckpt 3173: alpha102 SHIPPED and recorded: run 45326840944 green on attempt 2 (attempt 1
  b1b76738 ckpt 3171: alpha102 run 45326840944 attempt 1: test-jvm FAILED on the known flaky Strea
  997968a3 ckpt 3169: alpha102 (code 203) gated (ckpt 3168, head 0a423ffb) and dispatched ONCE: ru
  0a423ffb ckpt 3168: gated kestrel-g-v0.3.0-alpha102 (gecko code 203) and pushed it: handoff self
  c251ca3b ckpt 3167: pre-ship: alpha102: the animations on that page. (1) NO PLAY BAR OVER ANIMAT
  15d8bb97 ckpt 3165: owner request PERF22 (deep research: popular browsers' speed techniques, cod
```
