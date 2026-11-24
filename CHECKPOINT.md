# CHECKPOINT 3111 — read me first, then TASKS.md

**Written:** 2026-11-24T00:42:59Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `77f06ebc` (this checkpoint is the commit after it)

## Just done
owner request PERF17 written into TASKS.md in his words (a-e): faster speed, laggy site animations, lazy restore if logical and what major browsers do, very slow CDN bot-check pages

## Do this next
PERF17a: run python3 -B -I tools/diag_digest.py --days 2 (FOCUS REPORT first), read research/PAGE_SPEED_2026-11-23.md + research/COLD_START.md + research/PAINT_GLITCH.md, then write research/SPEED4.md with ranked causes; then PERF17b lazy restore (find where BrowserActivity restores sessions at start), PERF17c animations, PERF17d bot checks; claim with bash tools/claim.sh PERF17 first

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  77f06ebc ckpt 3110: alpha99 SHIPPED and recorded: run 45292001575 green on attempt 1 (check, tes
  5ae527e4 ckpt 3108: alpha99 run is 45292001575 (workflow_dispatch, started 23:48Z, head da26da05
  fad5637b ckpt 3107: alpha99 (code 200) gated and pushed (ckpt 3106); workflow dispatched ONCE on
  da26da05 ckpt 3106: gated kestrel-g-v0.3.0-alpha99 (gecko code 200) and pushed it: handoff self-
  feb56817 ckpt 3104: PERF16 a-d done: focus_report, sec lines, journal bookkeeping thinning, advi
  a6a2207d ckpt 3092: owner after alpha98: it seems faster; NEW PERF16: focus future diagnostics o
  c5e4207f ckpt 3091: alpha98 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha98 (gecko code 199, run
  946e2e68 ckpt 3089: alpha98 (gecko code 199) gated by ship.sh and ONE dispatch made on claude/ki
  665bc7a9 ckpt 3088: gated kestrel-g-v0.3.0-alpha98 (gecko code 199) and pushed it: handoff self-
  6b92a4b3 ckpt 3087: pre-ship: alpha98: switching between tabs no longer reloads them. Your diagn
```
