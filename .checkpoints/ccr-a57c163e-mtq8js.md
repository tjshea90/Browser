# CHECKPOINT 726 — read me first, then TASKS.md

**Written:** 2026-11-12T02:36:09Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `882fc3e` (this checkpoint is the commit after it)

## Just done
TASKS X4/X5/X12 statuses with evidence; HLS auto-resume test; lintRelease clean

## Do this next
alpha16 rerun in progress (run 43140828439 attempt 2): when tag appears verify, record-release, tell owner; then alpha17 (preconnect, M1/M7, web test gate)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  d41caca ckpt 724: M7 content sanity (web page where a file/video was expected -> BAD_CONTENT, HL
  61e1894 ckpt 721: M1b validator-less resume: Sink.readable/pread (Os.pread on both Android sinks
  f17fcb1 ckpt 719: web guard tests fixed (tap reports) + 2 new; CI test-js now gates the Gecko bu
  b1d2872 ckpt 715: Last-Modified splice guard (EngineTest), research/AUDIT-2026-11.md written, do
  7be9a12 ckpt 712: alpha16 dispatched to GitHub Actions (tag kestrel-g-v0.3.0-alpha16 appears whe
  21a22c2 ckpt 711: gated kestrel-g-v0.3.0-alpha16 (gecko code 115) and pushed it: handoff self-te
  7d6778a ckpt 710: pre-ship: alpha16: daily update dialog (app, engine, ad-block lists) with Upda
  6ab16b5 ckpt 709: X3 first batch done+tested: auto-resume after kill (both engines, SQLite v3 cr
  2d7afd4 ckpt 700: X2 verified on emulator (dialog found/ready/install flow); X12 first pieces co
  2cfcc7a ckpt 691: alpha15 SHIPPED+RECORDED (run 43136503549 green, tag kestrel-g-v0.3.0-alpha15,
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
