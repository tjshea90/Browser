# CHECKPOINT 732 — read me first, then TASKS.md

**Written:** 2026-11-12T02:57:01Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `b483361` (this checkpoint is the commit after it)

## Just done
alpha16 SHIPPED + RECORDED (run 43143728373 green; the two earlier runs failed on a transient 403 at Publish the Release and a CI-only stream-engine race that is now fixed). Heartbeat trigger trig_01LLoVKsjuTUaDn61UGP1pBN (every 2 h at :17) created. Owner repeated message 10 at 02:56Z (X13 in TASKS).

## Do this next
1) REPLY TO THE OWNER (Release link on its own line: https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha16): what alpha16 contains, answer the repeated message (everything on the list: done/open; audit doc research/AUDIT-2026-11.md; Proton Mail + typing: needs their numbers, steps in section 3; modifying Firefox: no, why), open questions (scheduled CI list build OK?). 2) then work on: X3 rest (M5 rest, M6 digest, M8 queue/limiter UI, M9, M10, T0), X5 scriptlets plan, page-resize idea only with numbers, alpha17. 3) delete heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN when everything is shipped (use delete_trigger)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  dc57dde ckpt 729: stream engine: a dropped job (merged-away sound of a pair, cancelled, cleared)
  9226e37 ckpt 726: TASKS X4/X5/X12 statuses with evidence; HLS auto-resume test; lintRelease clea
  d41caca ckpt 724: M7 content sanity (web page where a file/video was expected -> BAD_CONTENT, HL
  61e1894 ckpt 721: M1b validator-less resume: Sink.readable/pread (Os.pread on both Android sinks
  f17fcb1 ckpt 719: web guard tests fixed (tap reports) + 2 new; CI test-js now gates the Gecko bu
  b1d2872 ckpt 715: Last-Modified splice guard (EngineTest), research/AUDIT-2026-11.md written, do
  7be9a12 ckpt 712: alpha16 dispatched to GitHub Actions (tag kestrel-g-v0.3.0-alpha16 appears whe
  21a22c2 ckpt 711: gated kestrel-g-v0.3.0-alpha16 (gecko code 115) and pushed it: handoff self-te
  7d6778a ckpt 710: pre-ship: alpha16: daily update dialog (app, engine, ad-block lists) with Upda
  6ab16b5 ckpt 709: X3 first batch done+tested: auto-resume after kill (both engines, SQLite v3 cr
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
