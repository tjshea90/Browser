# CHECKPOINT 721 — read me first, then TASKS.md

**Written:** 2026-11-12T02:29:37Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `c87ab3f` (this checkpoint is the commit after it)

## Just done
M1b validator-less resume: Sink.readable/pread (Os.pread on both Android sinks), Engine.openWindow starts 4 KiB before the cursor and sameTail() compares; mismatch -> NEEDS_DECISION(FILE_CHANGED); 3 new EngineTest cases; full :core/:app suite green

## Do this next
alpha16 run 43140828439 should be finishing (tag kestrel-g-v0.3.0-alpha16 not yet on origin when last checked): verify success via python parse of the saved actions_list JSON, get_release_by_tag, record-release.sh, tell owner. Then alpha17: preconnect, M1b, workflow test-js gate, guard test fixes

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f17fcb1 ckpt 719: web guard tests fixed (tap reports) + 2 new; CI test-js now gates the Gecko bu
  b1d2872 ckpt 715: Last-Modified splice guard (EngineTest), research/AUDIT-2026-11.md written, do
  7be9a12 ckpt 712: alpha16 dispatched to GitHub Actions (tag kestrel-g-v0.3.0-alpha16 appears whe
  21a22c2 ckpt 711: gated kestrel-g-v0.3.0-alpha16 (gecko code 115) and pushed it: handoff self-te
  7d6778a ckpt 710: pre-ship: alpha16: daily update dialog (app, engine, ad-block lists) with Upda
  6ab16b5 ckpt 709: X3 first batch done+tested: auto-resume after kill (both engines, SQLite v3 cr
  2d7afd4 ckpt 700: X2 verified on emulator (dialog found/ready/install flow); X12 first pieces co
  2cfcc7a ckpt 691: alpha15 SHIPPED+RECORDED (run 43136503549 green, tag kestrel-g-v0.3.0-alpha15,
  2f132e9 ckpt 682: alpha15 shipped to GitHub: workflow dispatched (run queued on ccr-a57c163e-mtq
  4b80497 ckpt 681: gated kestrel-g-v0.3.0-alpha15 (gecko code 114) and pushed it: handoff self-te
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
