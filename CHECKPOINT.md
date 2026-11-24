# CHECKPOINT 3215 — read me first, then TASKS.md

**Written:** 2026-11-24T07:49:00Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `d7d755b6` (this checkpoint is the commit after it)

## Just done
PERF25c: built the hardware drawing trial (core DrawTrial + DrawTrialTest + OwnerRulesTest perf25, Prefs draw_trial/draw_trial_start/draw_trial_why, GeckoPrefs lines + guardTrial, BrowserActivity steady timer + early-crash rule, Settings row, Diagnostics read-back, digest A/B, check-gecko-prefs 71 names 0 missing); :core tests, :geckoapp and :app compile green; research/BRAVE_SMOOTH.md written

## Do this next
Run ./gradlew --offline -q :app:testDebugUnitTest (Robolectric: BackupManager/Prefs specs) then bash tools/test_handoff.sh; fix anything red; then release alpha105: bump geckoapp/build.gradle.kts versionCode 206 / 0.3.0-alpha105, bash tools/sync-ext-version.sh, write docs/handoff/ALPHA105_RELEASE_NOTE.txt (ASCII, no double quotes/backslashes; say NOT run on the tablet; owner test = Settings > Network lab > Hardware drawing trial 1, restart, claude.ai table + YouTube, bands -> 2 -> Off), bash ship.sh gecko with the note, ONE dispatch, verify, record-release, answer PERF25 and ask which device Brave ran on

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  6d20d18a ckpt 3203: wrote PERF25 (owner: Brave is smooth on the parlayapi site) into TASKS.md, n
  f3c4d330 ckpt 3202: alpha104 (code 205) SHIPPED: run 45346018174 green, Release kestrel-g-v0.3.0
  e168b9a7 ckpt 3199: alpha104 (code 205) gated (ckpt 3198, head 96feacef) and dispatched ONCE: ru
  96feacef ckpt 3198: gated kestrel-g-v0.3.0-alpha104 (gecko code 205) and pushed it: handoff self
  c6ef6bb0 ckpt 3197: pre-ship: alpha104: tab reloads, and the clip page. I read your two alpha103
  b46f56e2 ckpt 3193: PERF24 coded: tab sleep plan (core LiveBudget.sleepPlan + Tab.reloadMs + Pag
  64c28c57 ckpt 3188: owner request PERF24 (laggy typing in text boxes, the sports-odds page and i
  5a4fdea9 ckpt 3187: TEST1: the owner's step-by-step test guide written (research/TEST_PLAN_alpha
  2775495b ckpt 3186: alpha103 SHIPPED and recorded: run 45333801406 green on attempt 1, Release h
  4a09769f ckpt 3184: alpha103 (code 204) gated (ckpt 3183, head 3596f285) and dispatched ONCE: ru
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
