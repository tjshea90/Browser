# CHECKPOINT 3179 — read me first, then TASKS.md

**Written:** 2026-11-24T05:16:51Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `eadf09d6` (this checkpoint is the commit after it)

## Just done
PERF22: research/LOW_END_PLAYBOOK.md written; three Network lab experiments coded (labDither, labBytecode, labGcPar via GeckoPrefs.labOverrides, Settings rows, backup specs, digest WATCH, check-gecko-prefs.sh 66 names 0 missing, OwnerRulesTest perf22 guard); PERF23 written

## Do this next
run ./gradlew --offline -q :core:test :geckoapp:compileDebugKotlin :app:compileDebugKotlin (Monitor), then bump geckoapp to versionCode 204 / 0.3.0-alpha103, sync-ext-version, docs/handoff/ALPHA103_RELEASE_NOTE.txt (ASCII, no double quotes; says: FLK1 test hardening, 3 experiments OFF by default, research file; NOT run on the tablet), ship.sh gecko (background), ONE dispatch, poll the RUN (rerun_failed_jobs on the same run if a known flaky test fails again: now with failure messages in the log), record-release, answer the owner (PERF22 + FLK1), re-list open items (PERF17c/d fixes wait for alpha100+ numbers, PERF19 blocker, PERF23, PERF14e, PERF9, BackLab MISSED false positive)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M core/src/test/kotlin/app/kestrel/core/OwnerRulesTest.kt

## Last ten checkpoints
```
  3b1d4574 ckpt 3175: FLK1 done: real-time waits tripled in StreamEngineTest, PhoneBackendTest, Se
  9e7ae1ef ckpt 3173: alpha102 SHIPPED and recorded: run 45326840944 green on attempt 2 (attempt 1
  b1b76738 ckpt 3171: alpha102 run 45326840944 attempt 1: test-jvm FAILED on the known flaky Strea
  997968a3 ckpt 3169: alpha102 (code 203) gated (ckpt 3168, head 0a423ffb) and dispatched ONCE: ru
  0a423ffb ckpt 3168: gated kestrel-g-v0.3.0-alpha102 (gecko code 203) and pushed it: handoff self
  c251ca3b ckpt 3167: pre-ship: alpha102: the animations on that page. (1) NO PLAY BAR OVER ANIMAT
  15d8bb97 ckpt 3165: owner request PERF22 (deep research: popular browsers' speed techniques, cod
  d452ebef ckpt 3163: PERF21 coded: no bar on decorative clips (vfind decorative + videobar judge)
  09db2cad ckpt 3153: owner request PERF21 (screenshot: a page of looping picture clips is very la
  2057b01b ckpt 3152: alpha101 SHIPPED and recorded: run 45312620908 green on attempt 1 (check, te
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
