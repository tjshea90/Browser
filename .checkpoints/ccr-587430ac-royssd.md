# CHECKPOINT 2691 — read me first, then TASKS.md

**Written:** 2026-11-18T07:15:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `7cfac9cf` (this checkpoint is the commit after it)

## Just done
Handoff saved (.handoff/handoff.md + archive; CLAUDE.md and AGENTS.md learnings block updated). alpha70 released; nothing running.

## Do this next
NOTHING running. Wait for the owner's tablet test of alpha70 (Release https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha70). Open: VS2c vidsrc/111movies need Diagnostics taken while they play; CR1d crash cause needs the next vanish with the memory report. Note: the handoff tool wrote from /home/user/Browser while its hooks read /home/user, so auto-restore may not fire; the project's own cold resume (bash tools/resume.sh, CHECKPOINT.md) is authoritative.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M AGENTS.md
     M CLAUDE.md

## Last ten checkpoints
```
  7cfac9cf ckpt 2690: VS1e ticked (shipped in alpha70, tested, not proven on the real site).
  dc968cf9 ckpt 2689: alpha70 RELEASED: tag kestrel-g-v0.3.0-alpha70, run 44255522156 all jobs gre
  27d9762c ckpt 2688: alpha70 DISPATCHED ONCE at 07:00Z: android.yml run 44255522156 (run_number 2
  2e8c8251 ckpt 2687: alpha70 GATED locally (ship.sh rc=0: handoff self-test, unit suites, version
  30a0825b ckpt 2686: gated kestrel-g-v0.3.0-alpha70 (gecko code 171) and pushed it: handoff self-
  caf37927 ckpt 2685: Lab run 44252661300 (new2) READ: the app behaved right on every check (UC1 v
  e5fc78d7 ckpt 2684: Lab run 44252661300 (suite new2 = tb + lastrun_round, head d0e7d41d) DISPATC
  d0e7d41d ckpt 2683: Lab run 44248671431 READ: freeze scene proves CR1f on a real Android 10 (SCR
  6617fbe2 ckpt 2682: Full JVM suite 9 GREEN (rc=0) + web 616/616 with TP1, UC1 (dock), MT1, PV1 c
  3dda7232 ckpt 2681: PV1 coded: CurtainRule (core, 3 tests) + curtain view over the shared page v
```
