# CHECKPOINT 724 — read me first, then TASKS.md

**Written:** 2026-11-12T02:34:18Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `5454681` (this checkpoint is the commit after it)

## Just done
M7 content sanity (web page where a file/video was expected -> BAD_CONTENT, HLS segment sanity) + tests; docs. alpha16 first run FAILED only at Publish the Release with HTTP 403 Resource not accessible by integration (build+verify steps green); rerun_failed_jobs queued for run 43140828439

## Do this next
1) watch for tag kestrel-g-v0.3.0-alpha16 (/tmp/wait16.log); if the rerun fails again with 403: read_documentation/diagnose (repo Settings > Actions > Workflow permissions? ), do NOT retag. 2) after green: record-release.sh kestrel-g-v0.3.0-alpha16 '<note>' (note text in /tmp/ship16.log), tell owner. 3) alpha17 = preconnect + M1/M7 download safety + web test fixes + CI test-js gate

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M docs/ARCHITECTURE.md

## Last ten checkpoints
```
  61e1894 ckpt 721: M1b validator-less resume: Sink.readable/pread (Os.pread on both Android sinks
  f17fcb1 ckpt 719: web guard tests fixed (tap reports) + 2 new; CI test-js now gates the Gecko bu
  b1d2872 ckpt 715: Last-Modified splice guard (EngineTest), research/AUDIT-2026-11.md written, do
  7be9a12 ckpt 712: alpha16 dispatched to GitHub Actions (tag kestrel-g-v0.3.0-alpha16 appears whe
  21a22c2 ckpt 711: gated kestrel-g-v0.3.0-alpha16 (gecko code 115) and pushed it: handoff self-te
  7d6778a ckpt 710: pre-ship: alpha16: daily update dialog (app, engine, ad-block lists) with Upda
  6ab16b5 ckpt 709: X3 first batch done+tested: auto-resume after kill (both engines, SQLite v3 cr
  2d7afd4 ckpt 700: X2 verified on emulator (dialog found/ready/install flow); X12 first pieces co
  2cfcc7a ckpt 691: alpha15 SHIPPED+RECORDED (run 43136503549 green, tag kestrel-g-v0.3.0-alpha15,
  2f132e9 ckpt 682: alpha15 shipped to GitHub: workflow dispatched (run queued on ccr-a57c163e-mtq
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
