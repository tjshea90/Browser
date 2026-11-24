# CHECKPOINT 3222 — read me first, then TASKS.md

**Written:** 2026-11-24T19:06:34Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `2e56492f` (this checkpoint is the commit after it)

## Just done
wrote FF1 (new Firefox version into Kestrel, optimised for the hardware) and OPT1 (automatic optimizer that runs all experiments, logs, sends diagnostics, asks the owner) into TASKS.md, no code yet

## Do this next
FF1a first: find the newest GeckoView armeabi-v7a on maven.mozilla.org (web-fetch agent on https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview-armeabi-v7a/maven-metadata.xml; current pin is 160.0.20261104181552), read what changed since 160 for low-end Android, write research/FIREFOX_UPGRADE_<ver>.md; then FF1b bump + fix compile + tools/check-gecko-prefs.sh on the new libxul; ship as alpha106 / code 207. After that OPT1a design file research/OPTIMIZER.md, then OPT1b-e (ship as alpha107+).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  2e56492f ckpt 3221: alpha105 (code 206) SHIPPED: run 45352897624 green, Release kestrel-g-v0.3.0
  6fb30f29 ckpt 3219: alpha105 (code 206) gated (ckpt 3218, head 8b6eebf7) and dispatched ONCE: ru
  8b6eebf7 ckpt 3218: gated kestrel-g-v0.3.0-alpha105 (gecko code 206) and pushed it: handoff self
  c45aae47 ckpt 3217: PERF25: app unit tests green; bumped to alpha105 / code 206, release note do
  512c9e18 ckpt 3215: PERF25c: built the hardware drawing trial (core DrawTrial + DrawTrialTest + 
  6d20d18a ckpt 3203: wrote PERF25 (owner: Brave is smooth on the parlayapi site) into TASKS.md, n
  f3c4d330 ckpt 3202: alpha104 (code 205) SHIPPED: run 45346018174 green, Release kestrel-g-v0.3.0
  e168b9a7 ckpt 3199: alpha104 (code 205) gated (ckpt 3198, head 96feacef) and dispatched ONCE: ru
  96feacef ckpt 3198: gated kestrel-g-v0.3.0-alpha104 (gecko code 205) and pushed it: handoff self
  c6ef6bb0 ckpt 3197: pre-ship: alpha104: tab reloads, and the clip page. I read your two alpha103
```
