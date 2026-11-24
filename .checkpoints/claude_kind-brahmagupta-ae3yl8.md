# CHECKPOINT 3217 — read me first, then TASKS.md

**Written:** 2026-11-24T07:50:13Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `425da5ec` (this checkpoint is the commit after it)

## Just done
PERF25: app unit tests green; bumped to alpha105 / code 206, release note docs/handoff/ALPHA105_RELEASE_NOTE.txt written

## Do this next
Run bash ship.sh gecko with the note from docs/handoff/ALPHA105_RELEASE_NOTE.txt (background; it gates and pushes), then ONE dispatch of android.yml (app=gecko full_build=true note), poll the RUN, get_release_by_tag kestrel-g-v0.3.0-alpha105, tools/record-release.sh with the identical note, tick PERF25c in TASKS, answer the owner and ask which device Brave ran on

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/build.gradle.kts
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  512c9e18 ckpt 3215: PERF25c: built the hardware drawing trial (core DrawTrial + DrawTrialTest + 
  6d20d18a ckpt 3203: wrote PERF25 (owner: Brave is smooth on the parlayapi site) into TASKS.md, n
  f3c4d330 ckpt 3202: alpha104 (code 205) SHIPPED: run 45346018174 green, Release kestrel-g-v0.3.0
  e168b9a7 ckpt 3199: alpha104 (code 205) gated (ckpt 3198, head 96feacef) and dispatched ONCE: ru
  96feacef ckpt 3198: gated kestrel-g-v0.3.0-alpha104 (gecko code 205) and pushed it: handoff self
  c6ef6bb0 ckpt 3197: pre-ship: alpha104: tab reloads, and the clip page. I read your two alpha103
  b46f56e2 ckpt 3193: PERF24 coded: tab sleep plan (core LiveBudget.sleepPlan + Tab.reloadMs + Pag
  64c28c57 ckpt 3188: owner request PERF24 (laggy typing in text boxes, the sports-odds page and i
  5a4fdea9 ckpt 3187: TEST1: the owner's step-by-step test guide written (research/TEST_PLAN_alpha
  2775495b ckpt 3186: alpha103 SHIPPED and recorded: run 45333801406 green on attempt 1, Release h
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
