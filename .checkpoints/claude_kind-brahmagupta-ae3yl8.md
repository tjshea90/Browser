# CHECKPOINT 3148 — read me first, then TASKS.md

**Written:** 2026-11-24T02:27:56Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `d1074587` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha101 (gecko code 202) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/kind-brahmagupta-ae3yl8, inputs {"app":"gecko","full_build":"true","note":"alpha101: the browser now keeps its warm memory when memory is not short. I measured your tablet and read how the engine, Chrome and Android handle memory (research/RAM_FOR_SPEED.md). (1) FINDING: Android sends the browser a critical low memory warning about four times a day, and one every time you leave the app, while 340 to 800 MB are still free (all 33 of the last warnings of the strongest kind came with at least 340 MB free). The page engine obeys every one of them: it throws away its decoded pictures, its frozen Back pages and, through a clean up run, the compiled script code of the pages you use, which is exactly what makes a heavy page fast when you come back to it. (2) CHANGE: those warnings now pass through a gate. A warning is held when 350 MB or more are available, the system itself does not say low memory, the engine was not ended in the last 6 hours, and fewer than 6 were already held in the last half hour. Anything else is passed on exactly as before, so real shortage is still answered. Settings, Keep the engine's memory when it is not short, turns it off. Diagnostics shows the line Memory signals to the engine with the held and passed on counts. (3) NOT DONE, with reasons: memory is not your main limit (median 481 MB free at page loads, 2 engine kills in 14 days; the page thread's processor is), a second page process would cost 65 to 95 MB and your numbers show no sign it would help (first paint with 2 live tabs 1.8 s, with 1 tab 2.0 s), keeping the app process alive after Android closes its screen would gain 1 to 2 seconds for 300 MB, a bigger Back cache was already flagged as too much. Not run on the tablet: the gate is tested in the unit tests and compiled into the engine build; the first upload shows whether warnings are held (mem trim-held lines), whether engine kills stay at about one a week, and the speed gain is a hypothesis until then."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha101), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha101 "alpha101: the browser now keeps its warm memory when memory is not short. I measured your tablet and read how the engine, Chrome and Android handle memory (research/RAM_FOR_SPEED.md). (1) FINDING: Android sends the browser a critical low memory warning about four times a day, and one every time you leave the app, while 340 to 800 MB are still free (all 33 of the last warnings of the strongest kind came with at least 340 MB free). The page engine obeys every one of them: it throws away its decoded pictures, its frozen Back pages and, through a clean up run, the compiled script code of the pages you use, which is exactly what makes a heavy page fast when you come back to it. (2) CHANGE: those warnings now pass through a gate. A warning is held when 350 MB or more are available, the system itself does not say low memory, the engine was not ended in the last 6 hours, and fewer than 6 were already held in the last half hour. Anything else is passed on exactly as before, so real shortage is still answered. Settings, Keep the engine's memory when it is not short, turns it off. Diagnostics shows the line Memory signals to the engine with the held and passed on counts. (3) NOT DONE, with reasons: memory is not your main limit (median 481 MB free at page loads, 2 engine kills in 14 days; the page thread's processor is), a second page process would cost 65 to 95 MB and your numbers show no sign it would help (first paint with 2 live tabs 1.8 s, with 1 tab 2.0 s), keeping the app process alive after Android closes its screen would gain 1 to 2 seconds for 300 MB, a bigger Back cache was already flagged as too much. Not run on the tablet: the gate is tested in the unit tests and compiled into the engine build; the first upload shows whether warnings are held (mem trim-held lines), whether engine kills stay at about one a week, and the speed gain is a hypothesis until then."
NEVER ship kestrel-g-v0.3.0-alpha101 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  9d6056a5 ckpt 3146: PERF20 coded and green: TrimGate (core) + TrimGateContext (app, Robolectric-
  f082d388 ckpt 3138: owner request PERF20 (research how the browser can be as fast as possible us
  2b1119f3 ckpt 3137: TASKS.md closed for PERF17e/PERF18, PERF17 left open for the c/d fixes (clai
  7a224ab8 ckpt 3135: alpha100 SHIPPED and recorded: run 45303459458 green on attempt 1 (check, te
  654a7ecd ckpt 3133: alpha100 (code 201) gated, pushed (ckpt 3132) and dispatched ONCE: run 45303
  4b69f1eb ckpt 3132: gated kestrel-g-v0.3.0-alpha100 (gecko code 201) and pushed it: handoff self
  39ac98cc ckpt 3130: PERF17/PERF18 coded and green: lazy restore after 4 h (core LazyStart, Home 
  2d2398f7 ckpt 3120: PERF17b lazy restore CODED and compiled (core LazyStart + LazyStartTest gree
  0cf14251 ckpt 3113: owner request PERF18 (research online what major browsers do for fast loadin
  9dcb9f9f ckpt 3111: owner request PERF17 written into TASKS.md in his words (a-e): faster speed,
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
