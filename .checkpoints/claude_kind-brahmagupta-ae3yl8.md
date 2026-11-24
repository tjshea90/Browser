# CHECKPOINT 3182 — read me first, then TASKS.md

**Written:** 2026-11-24T05:28:43Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `af8a1202` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha103: the deep low-end research, three speed experiments, and a test fix. (1) RESEARCH: research/LOW_END_PLAYBOOK.md compares what popular browsers do for speed on weak hardware with what Kestrel does today, using your own tablet numbers: four ARM cores, Mali-G52 graphics, 1954 MB of memory, Android 10 Go with a 32-bit only system, and no heat slowdown in any of 35 uploads. Almost everything popular browsers do, Kestrel already does. What is left and measured: the engine's main thread inside the app process is the busiest competitor for your four cores (it carries the ad blocker; median 34 percent of one core when busy, in 121 of 175 busy moments), then the disk cache thread, the engine's tracking list updater (rare but up to 96 percent of a core) and the app's own memory clean-up; those are the next tasks. Android 10 Go specifics I found: its memory warnings do not follow free memory (filtered since alpha101), apps in the background die first (hence the start screen after 4 hours, alpha100), this device has no performance hint or sustained performance mode to call, and a 64-bit engine cannot run on a 32-bit only system. (2) THREE EXPERIMENTS in Settings, Network lab, all OFF: Cheaper gradients (no dithering), Compile scripts on first load, and Parallel script clean-up. I claim no gain: each is a setting your engine has that popular browser notes point at, and only your tablet can judge it. Each takes effect at the next start of Kestrel and every upload records it, so my report compares page times with and without. Turn them on one at a time for a day. (3) The download test that failed once on the build computer, because a 30 second wait ran out on its two slow cores, now waits longer, and failing tests print their message; this changes nothing in the app. Not run on the tablet: the three switches are tested in unit tests and the engine setting names are checked against your engine.

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ee5eb3f1 ckpt 3179: PERF22: research/LOW_END_PLAYBOOK.md written; three Network lab experiments 
  3b1d4574 ckpt 3175: FLK1 done: real-time waits tripled in StreamEngineTest, PhoneBackendTest, Se
  9e7ae1ef ckpt 3173: alpha102 SHIPPED and recorded: run 45326840944 green on attempt 2 (attempt 1
  b1b76738 ckpt 3171: alpha102 run 45326840944 attempt 1: test-jvm FAILED on the known flaky Strea
  997968a3 ckpt 3169: alpha102 (code 203) gated (ckpt 3168, head 0a423ffb) and dispatched ONCE: ru
  0a423ffb ckpt 3168: gated kestrel-g-v0.3.0-alpha102 (gecko code 203) and pushed it: handoff self
  c251ca3b ckpt 3167: pre-ship: alpha102: the animations on that page. (1) NO PLAY BAR OVER ANIMAT
  15d8bb97 ckpt 3165: owner request PERF22 (deep research: popular browsers' speed techniques, cod
  d452ebef ckpt 3163: PERF21 coded: no bar on decorative clips (vfind decorative + videobar judge)
  09db2cad ckpt 3153: owner request PERF21 (screenshot: a page of looping picture clips is very la
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
