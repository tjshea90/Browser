# CHECKPOINT 3193 — read me first, then TASKS.md

**Written:** 2026-11-24T06:44:35Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `17f25087` (this checkpoint is the commit after it)

## Just done
PERF24 coded: tab sleep plan (core LiveBudget.sleepPlan + Tab.reloadMs + PageTimes.stop returns ms + BrowserActivity.enforceLiveLimit), decoclips.js one clip at a time + none while scrolling (8 Chromium tests pass), Settings/Diagnostics text, OwnerRulesTest perf24, research/LAG_2026-11-24.md

## Do this next
run ./gradlew --offline -q :core:test :geckoapp:compileDebugKotlin :app:compileDebugKotlin (Monitor); then bump geckoapp to versionCode 205 / 0.3.0-alpha104, sync-ext-version, docs/handoff/ALPHA104_RELEASE_NOTE.txt (ASCII, no double quotes, no site name; say what was NOT run on the tablet and the Scroll feel setting to try), ship.sh gecko (background), ONE dispatch, poll the RUN (rerun_failed_jobs on the same run if a known flaky test fails: the message now prints), record-release, answer the owner (PERF24) with the Release link and re-list open items

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M app/src/main/kotlin/app/kestrel/browser/ui/SettingsView.kt
     M core/src/test/kotlin/app/kestrel/core/OwnerRulesTest.kt
     M geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt
    ?? research/LAG_2026-11-24.md

## Last ten checkpoints
```
  64c28c57 ckpt 3188: owner request PERF24 (laggy typing in text boxes, the sports-odds page and i
  5a4fdea9 ckpt 3187: TEST1: the owner's step-by-step test guide written (research/TEST_PLAN_alpha
  2775495b ckpt 3186: alpha103 SHIPPED and recorded: run 45333801406 green on attempt 1, Release h
  4a09769f ckpt 3184: alpha103 (code 204) gated (ckpt 3183, head 3596f285) and dispatched ONCE: ru
  3596f285 ckpt 3183: gated kestrel-g-v0.3.0-alpha103 (gecko code 204) and pushed it: handoff self
  cb169fe5 ckpt 3182: pre-ship: alpha103: the deep low-end research, three speed experiments, and 
  ee5eb3f1 ckpt 3179: PERF22: research/LOW_END_PLAYBOOK.md written; three Network lab experiments 
  3b1d4574 ckpt 3175: FLK1 done: real-time waits tripled in StreamEngineTest, PhoneBackendTest, Se
  9e7ae1ef ckpt 3173: alpha102 SHIPPED and recorded: run 45326840944 green on attempt 2 (attempt 1
  b1b76738 ckpt 3171: alpha102 run 45326840944 attempt 1: test-jvm FAILED on the known flaky Strea
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
