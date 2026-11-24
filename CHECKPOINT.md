# CHECKPOINT 3175 — read me first, then TASKS.md

**Written:** 2026-11-24T05:12:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `959ff2af` (this checkpoint is the commit after it)

## Just done
FLK1 done: real-time waits tripled in StreamEngineTest, PhoneBackendTest, ServerTest; core test failure logging (message + stack) on; the three classes pass through Gradle

## Do this next
PERF22: write research/LOW_END_PLAYBOOK.md now. Inputs already collected: web research (Chromium low-end mode, Android Go docs, MT8168 specs, Mozilla Mali WebRender bugs, SpiderMonkey thresholds 100/1500, Android cpusets, Chrome 64-bit gain 4-8 percent, Chromium WebView single-process only on Android Q low-memory devices, Chrome discards ~100 MiB/tab of offscreen images) + the repo audits (GECKO_TUNING_2 summary: engine already near what prefs can give; HW_SPEED H2 io thread nice 13 done; RAM_FOR_SPEED; BROWSER_TECHNIQUES; PAGE_SPEED) + the owner's CPU lines (Classifier Upda thread 56-96 percent bursts, Cache2 I/O 41 percent, HeapTaskDaemon 10-17 percent, kestrel-io background lane). Then decide the few 'do' items (candidates: lab A/B switches for bytecode -1, gc_parallel_marking, processCount 2 with digest grouping; an eMMC read-speed probe line; measure Classifier update frequency) and ship alpha103 / versionCode 204

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  9e7ae1ef ckpt 3173: alpha102 SHIPPED and recorded: run 45326840944 green on attempt 2 (attempt 1
  b1b76738 ckpt 3171: alpha102 run 45326840944 attempt 1: test-jvm FAILED on the known flaky Strea
  997968a3 ckpt 3169: alpha102 (code 203) gated (ckpt 3168, head 0a423ffb) and dispatched ONCE: ru
  0a423ffb ckpt 3168: gated kestrel-g-v0.3.0-alpha102 (gecko code 203) and pushed it: handoff self
  c251ca3b ckpt 3167: pre-ship: alpha102: the animations on that page. (1) NO PLAY BAR OVER ANIMAT
  15d8bb97 ckpt 3165: owner request PERF22 (deep research: popular browsers' speed techniques, cod
  d452ebef ckpt 3163: PERF21 coded: no bar on decorative clips (vfind decorative + videobar judge)
  09db2cad ckpt 3153: owner request PERF21 (screenshot: a page of looping picture clips is very la
  2057b01b ckpt 3152: alpha101 SHIPPED and recorded: run 45312620908 green on attempt 1 (check, te
  1e465904 ckpt 3150: alpha101 (code 202) gated (ckpt 3148, head 94c20d92 + the corrected note fil
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
