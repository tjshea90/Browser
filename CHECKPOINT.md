# CHECKPOINT 3171 — read me first, then TASKS.md

**Written:** 2026-11-24T04:47:34Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `c990e83f` (this checkpoint is the commit after it)

## Just done
alpha102 run 45326840944 attempt 1: test-jvm FAILED on the known flaky StreamEngineTest.refusalsThatStartHalfWayAreAskedAgainBeforeTheLadderIsTried (2061 of 2062 passed; same race family as FLK1); rerun_failed_jobs queued on the SAME run (attempt 2), no new dispatch

## Do this next
Poll RUN 45326840944 until completed (attempt 2: test-jvm then build-gecko); if it fails again read get_job_logs job_id of the failing job; when green get_release_by_tag kestrel-g-v0.3.0-alpha102, bash tools/record-release.sh kestrel-g-v0.3.0-alpha102 with the identical note from docs/handoff/ALPHA102_RELEASE_NOTE.txt, answer the owner for PERF21. THEN FLK1 (harden StreamEngineTest line ~495 perPart>=4 race, PhoneBackendTest, courier ServerTest), THEN PERF22 playbook (web research already collected in the conversation: MT8168 = 4x A53 2 GHz, Mali-G52 MC2/MP1, LPDDR4x; hardware file says armeabi-v7a, thermal 0, no sustained-performance mode; Mozilla Mali WebRender bugs 1676474/1689947/1874848; Chromium low-end mode; Android Go docs; SpiderMonkey thresholds 100/1500; Android cpusets top-app)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  997968a3 ckpt 3169: alpha102 (code 203) gated (ckpt 3168, head 0a423ffb) and dispatched ONCE: ru
  0a423ffb ckpt 3168: gated kestrel-g-v0.3.0-alpha102 (gecko code 203) and pushed it: handoff self
  c251ca3b ckpt 3167: pre-ship: alpha102: the animations on that page. (1) NO PLAY BAR OVER ANIMAT
  15d8bb97 ckpt 3165: owner request PERF22 (deep research: popular browsers' speed techniques, cod
  d452ebef ckpt 3163: PERF21 coded: no bar on decorative clips (vfind decorative + videobar judge)
  09db2cad ckpt 3153: owner request PERF21 (screenshot: a page of looping picture clips is very la
  2057b01b ckpt 3152: alpha101 SHIPPED and recorded: run 45312620908 green on attempt 1 (check, te
  1e465904 ckpt 3150: alpha101 (code 202) gated (ckpt 3148, head 94c20d92 + the corrected note fil
  94c20d92 ckpt 3148: gated kestrel-g-v0.3.0-alpha101 (gecko code 202) and pushed it: handoff self
  9d6056a5 ckpt 3146: PERF20 coded and green: TrimGate (core) + TrimGateContext (app, Robolectric-
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
