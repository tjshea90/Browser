# CHECKPOINT 3230 — read me first, then TASKS.md

**Written:** 2026-11-24T19:13:27Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `c0323985` (this checkpoint is the commit after it)

## Just done
FF1: GeckoView 161.0.20261118180411 + webcompat 161.0 pinned, clean compile, check-gecko-prefs 71 names 0 missing on 161, five new 161 prefs added to the draw-prefs read-back, core + app tests green, research/FIREFOX_UPGRADE_161.md written

## Do this next
Dispatch ONE emulator smoke lab on this commit (android.yml app=lab lab_suite=smoke lab_profile=standard lab_net=full lab_minutes=5) to prove the 161 engine starts and loads pages; while it runs do OPT1a (research/OPTIMIZER.md). When the lab is green (or its failure is understood): bump geckoapp/build.gradle.kts to versionCode 207 / 0.3.0-alpha106, bash tools/sync-ext-version.sh, write docs/handoff/ALPHA106_RELEASE_NOTE.txt, bash ship.sh gecko, ONE dispatch, record-release, tick FF1c, answer owner (FF1 + OPT1 status)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  e7367846 ckpt 3222: wrote FF1 (new Firefox version into Kestrel, optimised for the hardware) and
  2e56492f ckpt 3221: alpha105 (code 206) SHIPPED: run 45352897624 green, Release kestrel-g-v0.3.0
  6fb30f29 ckpt 3219: alpha105 (code 206) gated (ckpt 3218, head 8b6eebf7) and dispatched ONCE: ru
  8b6eebf7 ckpt 3218: gated kestrel-g-v0.3.0-alpha105 (gecko code 206) and pushed it: handoff self
  c45aae47 ckpt 3217: PERF25: app unit tests green; bumped to alpha105 / code 206, release note do
  512c9e18 ckpt 3215: PERF25c: built the hardware drawing trial (core DrawTrial + DrawTrialTest + 
  6d20d18a ckpt 3203: wrote PERF25 (owner: Brave is smooth on the parlayapi site) into TASKS.md, n
  f3c4d330 ckpt 3202: alpha104 (code 205) SHIPPED: run 45346018174 green, Release kestrel-g-v0.3.0
  e168b9a7 ckpt 3199: alpha104 (code 205) gated (ckpt 3198, head 96feacef) and dispatched ONCE: ru
  96feacef ckpt 3198: gated kestrel-g-v0.3.0-alpha104 (gecko code 205) and pushed it: handoff self
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
