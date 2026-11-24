# CHECKPOINT 3199 — read me first, then TASKS.md

**Written:** 2026-11-24T07:11:21Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `96feacef` (this checkpoint is the commit after it)

## Just done
alpha104 (code 205) gated (ckpt 3198, head 96feacef) and dispatched ONCE: run 45346018174 (07:11Z)

## Do this next
Poll RUN 45346018174 (mcp__github__actions_list list_workflow_jobs resource_id 45346018174) until completed; if test-jvm fails read mcp__github__get_job_logs job_id (failure messages print now) and, if it is a known flaky test, rerun_failed_jobs run_id 45346018174 (never a new dispatch); when green get_release_by_tag kestrel-g-v0.3.0-alpha104, bash tools/record-release.sh kestrel-g-v0.3.0-alpha104 with the identical note from docs/handoff/ALPHA104_RELEASE_NOTE.txt; tick PERF24e in TASKS.md; answer the owner (PERF24) with the Release link, NOT run on the tablet, the Scroll feel setting to try, and open items (PERF17c/d, PERF19 + PERF23, PERF14e, PERF9, BackLab MISSED false positive). Never ship alpha104 again; next is alpha105 / versionCode 206

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  96feacef ckpt 3198: gated kestrel-g-v0.3.0-alpha104 (gecko code 205) and pushed it: handoff self
  c6ef6bb0 ckpt 3197: pre-ship: alpha104: tab reloads, and the clip page. I read your two alpha103
  b46f56e2 ckpt 3193: PERF24 coded: tab sleep plan (core LiveBudget.sleepPlan + Tab.reloadMs + Pag
  64c28c57 ckpt 3188: owner request PERF24 (laggy typing in text boxes, the sports-odds page and i
  5a4fdea9 ckpt 3187: TEST1: the owner's step-by-step test guide written (research/TEST_PLAN_alpha
  2775495b ckpt 3186: alpha103 SHIPPED and recorded: run 45333801406 green on attempt 1, Release h
  4a09769f ckpt 3184: alpha103 (code 204) gated (ckpt 3183, head 3596f285) and dispatched ONCE: ru
  3596f285 ckpt 3183: gated kestrel-g-v0.3.0-alpha103 (gecko code 204) and pushed it: handoff self
  cb169fe5 ckpt 3182: pre-ship: alpha103: the deep low-end research, three speed experiments, and 
  ee5eb3f1 ckpt 3179: PERF22: research/LOW_END_PLAYBOOK.md written; three Network lab experiments 
```
