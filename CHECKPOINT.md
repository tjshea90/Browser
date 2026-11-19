# CHECKPOINT 2694 — read me first, then TASKS.md

**Written:** 2026-11-19T01:05:35Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kestrel-browser-optimization-h6y8u2` · **builds on:** `7f3df72f` (this checkpoint is the commit after it)

## Just done
EM1k step 1 CODED + compiled: core LastLookRules.whyNot (+test, green) names why a saved picture is unusable; app logs 'tabs look-none why=' at the start cover and counts every skipped capture (look.skip.<why>, 'tabs look-skip pause why=' at leaving); new lab suite ro (soak.py reopen_round, android.yml + emu-lab.yml options) measures reopen: picture drawn or not, why, frame/runtime/paint marks -> reopen.md; self-test ok

## Do this next
Dispatch lab suite ro (android.yml app=lab lab_suite=ro lab_profile=tablet lab_minutes=10) NOW so it queues behind baseline run 44432470135 (daily, running); read both with research/EM1_RESULTS.md. Then fix what ro shows (look missing / late). Heartbeat trigger trig_018ZFUWQpV3dBUPXUvKDiXwQ (delete when EM1 ships).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  54c9c34f ckpt 2693: EM1a: baseline lab DISPATCHED 2026-11-19 00:52Z: android.yml run 44432470135
  ce7436f4 ckpt 2692: Owner request 2026-11-19 (11 improvement areas + start GitHub emulator labs)
  06160983 ckpt 2691: Handoff saved (.handoff/handoff.md + archive; CLAUDE.md and AGENTS.md learni
  7cfac9cf ckpt 2690: VS1e ticked (shipped in alpha70, tested, not proven on the real site).
  dc968cf9 ckpt 2689: alpha70 RELEASED: tag kestrel-g-v0.3.0-alpha70, run 44255522156 all jobs gre
  27d9762c ckpt 2688: alpha70 DISPATCHED ONCE at 07:00Z: android.yml run 44255522156 (run_number 2
  2e8c8251 ckpt 2687: alpha70 GATED locally (ship.sh rc=0: handoff self-test, unit suites, version
  30a0825b ckpt 2686: gated kestrel-g-v0.3.0-alpha70 (gecko code 171) and pushed it: handoff self-
  caf37927 ckpt 2685: Lab run 44252661300 (new2) READ: the app behaved right on every check (UC1 v
  e5fc78d7 ckpt 2684: Lab run 44252661300 (suite new2 = tb + lastrun_round, head d0e7d41d) DISPATC
```

(12 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
