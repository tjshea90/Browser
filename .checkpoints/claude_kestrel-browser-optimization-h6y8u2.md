# CHECKPOINT 2697 — read me first, then TASKS.md

**Written:** 2026-11-19T01:19:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kestrel-browser-optimization-h6y8u2` · **builds on:** `055eaeb7` (this checkpoint is the commit after it)

## Just done
EM1k step 2 CODED + tests green + Gecko module compiles: a page that painted but never finishes loading is now photographed when the app is LEFT (core LastLookRules.mayTakeWhileLoading + test; GeckoTab.paintedFor set at first paint, reset with painted; lookSkip uses it). Real tablet pastes say 'Last look: on, 1-2 stored, shown at start 0' in 4 of 5 starts: pictures exist but are not usable at start (page differs), so the look-none why= line is the proof to read next.

## Do this next
Read lab run 44434241145 (ro) when its artifacts appear (actions_list list_workflow_run_artifacts) -> reopen.md + health-lines.txt (grep look-none/look-skip/look-shown); then em run 44435517079 (ro + dr). After both: dispatch compat (items 5/7) and decide the alpha71 content (EM1k step 1+2, whatever ro/dr justify). Heartbeat trig_018ZFUWQpV3dBUPXUvKDiXwQ.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  7c63b314 ckpt 2696: EM1a baseline READ (daily run 44432470135 green: security 17/0, 68 pages pai
  69bb5648 ckpt 2695: EM1i research: real doraby chain measured from the sandbox (6 servers, resea
  609d22a7 ckpt 2694: EM1k step 1 CODED + compiled: core LastLookRules.whyNot (+test, green) names
  54c9c34f ckpt 2693: EM1a: baseline lab DISPATCHED 2026-11-19 00:52Z: android.yml run 44432470135
  ce7436f4 ckpt 2692: Owner request 2026-11-19 (11 improvement areas + start GitHub emulator labs)
  06160983 ckpt 2691: Handoff saved (.handoff/handoff.md + archive; CLAUDE.md and AGENTS.md learni
  7cfac9cf ckpt 2690: VS1e ticked (shipped in alpha70, tested, not proven on the real site).
  dc968cf9 ckpt 2689: alpha70 RELEASED: tag kestrel-g-v0.3.0-alpha70, run 44255522156 all jobs gre
  27d9762c ckpt 2688: alpha70 DISPATCHED ONCE at 07:00Z: android.yml run 44255522156 (run_number 2
  2e8c8251 ckpt 2687: alpha70 GATED locally (ship.sh rc=0: handoff self-test, unit suites, version
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
