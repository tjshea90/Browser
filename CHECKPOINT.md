# CHECKPOINT 2693 — read me first, then TASKS.md

**Written:** 2026-11-19T00:52:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kestrel-browser-optimization-h6y8u2` · **builds on:** `de41f802` (this checkpoint is the commit after it)

## Just done
EM1a: baseline lab DISPATCHED 2026-11-19 00:52Z: android.yml run 44432470135 (suite daily, profile tablet, net full, 45 min, head de41f802 = alpha70 code, nothing changed yet); EM1 claimed

## Do this next
While it runs (60-90 min): investigate EM1k (why the owner still sees the Opening <host> cover although EY-1 last look exists: read app/src/main/kotlin/.../ui/LastLook.kt, geckoapp BrowserActivity earlyCover/lookFor/captureLook, core session/LastLook.kt rules); then EM1i (doraby servers, research/DORABY.md + FINDER_LAB.md). Read the lab result with actions_list list_workflow_jobs 44432470135 then the build-report artifact -> research/EM1_RESULTS.md. DO NOT dispatch a second lab until this one ends (queue keeps only one pending).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ce7436f4 ckpt 2692: Owner request 2026-11-19 (11 improvement areas + start GitHub emulator labs)
  06160983 ckpt 2691: Handoff saved (.handoff/handoff.md + archive; CLAUDE.md and AGENTS.md learni
  7cfac9cf ckpt 2690: VS1e ticked (shipped in alpha70, tested, not proven on the real site).
  dc968cf9 ckpt 2689: alpha70 RELEASED: tag kestrel-g-v0.3.0-alpha70, run 44255522156 all jobs gre
  27d9762c ckpt 2688: alpha70 DISPATCHED ONCE at 07:00Z: android.yml run 44255522156 (run_number 2
  2e8c8251 ckpt 2687: alpha70 GATED locally (ship.sh rc=0: handoff self-test, unit suites, version
  30a0825b ckpt 2686: gated kestrel-g-v0.3.0-alpha70 (gecko code 171) and pushed it: handoff self-
  caf37927 ckpt 2685: Lab run 44252661300 (new2) READ: the app behaved right on every check (UC1 v
  e5fc78d7 ckpt 2684: Lab run 44252661300 (suite new2 = tb + lastrun_round, head d0e7d41d) DISPATC
  d0e7d41d ckpt 2683: Lab run 44248671431 READ: freeze scene proves CR1f on a real Android 10 (SCR
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
