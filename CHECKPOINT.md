# CHECKPOINT 3219 — read me first, then TASKS.md

**Written:** 2026-11-24T08:04:24Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `8b6eebf7` (this checkpoint is the commit after it)

## Just done
alpha105 (code 206) gated (ckpt 3218, head 8b6eebf7) and dispatched ONCE: run 45352897624 (run 287, 08:03Z)

## Do this next
Poll RUN 45352897624 (mcp__github__actions_list list_workflow_jobs resource_id 45352897624; known flaky tests StreamEngineTest/PhoneBackendTest/ServerTest -> rerun_failed_jobs on the SAME run, never a new dispatch); when green get_release_by_tag kestrel-g-v0.3.0-alpha105, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha105 with the identical note from docs/handoff/ALPHA105_RELEASE_NOTE.txt (tr -d newline); tick PERF25c in TASKS; answer the owner (PERF25) in plain words with the Release link, NOT run on the tablet, ask which device Brave ran on, the 3-minute test; re-list open items (PERF17c/d, PERF19, PERF23, PERF14e, PERF9, Back MISSED false positive, owner test plan Days 2-5). Never ship alpha105 again; next is alpha106 / code 207

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  8b6eebf7 ckpt 3218: gated kestrel-g-v0.3.0-alpha105 (gecko code 206) and pushed it: handoff self
  c45aae47 ckpt 3217: PERF25: app unit tests green; bumped to alpha105 / code 206, release note do
  512c9e18 ckpt 3215: PERF25c: built the hardware drawing trial (core DrawTrial + DrawTrialTest + 
  6d20d18a ckpt 3203: wrote PERF25 (owner: Brave is smooth on the parlayapi site) into TASKS.md, n
  f3c4d330 ckpt 3202: alpha104 (code 205) SHIPPED: run 45346018174 green, Release kestrel-g-v0.3.0
  e168b9a7 ckpt 3199: alpha104 (code 205) gated (ckpt 3198, head 96feacef) and dispatched ONCE: ru
  96feacef ckpt 3198: gated kestrel-g-v0.3.0-alpha104 (gecko code 205) and pushed it: handoff self
  c6ef6bb0 ckpt 3197: pre-ship: alpha104: tab reloads, and the clip page. I read your two alpha103
  b46f56e2 ckpt 3193: PERF24 coded: tab sleep plan (core LiveBudget.sleepPlan + Tab.reloadMs + Pag
  64c28c57 ckpt 3188: owner request PERF24 (laggy typing in text boxes, the sports-odds page and i
```
