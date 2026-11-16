# CHECKPOINT 2552 — read me first, then TASKS.md

**Written:** 2026-11-16T18:58:46Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `47d2920c` (this checkpoint is the commit after it)

## Just done
RELEASE kestrel-g-v0.3.0-alpha60 (gecko code 161) DISPATCHED ONCE: android.yml run 43934390638 (head 47d2920c, started 18:58:16Z, full_build=true, note = docs/handoff/ALPHA60_RELEASE_NOTE.txt). vp lab before it: run 43930125170 = 11 scenes, 349 checks, 0 failed. The stale queued lab run 43920043728 was cancelled.

## Do this next
DO NOT DISPATCH AGAIN (ckpt 2551 above still says 'trigger the build': that is done). 1) wait for run 43934390638: use mcp__github__actions_get get_workflow_job / list_workflow_jobs filter latest (NOT list_workflow_runs: huge); expect ~10-15 min; 2) verify the tag: git ls-remote --tags origin kestrel-g-v0.3.0-alpha60, all jobs green, mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha60 shows the APK; 3) ONLY THEN: bash tools/record-release.sh kestrel-g-v0.3.0-alpha60 "$(cat docs/handoff/ALPHA60_RELEASE_NOTE.txt)" (identical note); 4) update HANDOFF (alpha60 SHIPPED bullet), tick ANS55f5 / ANS58d / ANS58 / ANS59 rows; 5) answer the owner (messages 55, 57, 58, 59: plain words, Release link https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha60 on its own line, what was NOT run on the tablet, open items: VP12 YouTube-style films, VP10, ROT2e, ANS48, SOAK1d, LAB2f, ANS52j, ANS55h return-to-inline after full screen, ANS58e lazy script injection, SLOW Cloudflare/typing lag unmeasured) and delete heartbeat trig_01LyMjhKLgSaFigb7F61XAfe.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  47d2920c ckpt 2551: gated kestrel-g-v0.3.0-alpha60 (gecko code 161) and pushed it: handoff self-
  4f54303f ckpt 2550: pre-ship: alpha60: your messages 55, 57, 58 and 59 (the player inside the we
  2ef63cb0 ckpt 2549: lab vp (all video suites kp np cp ap ho ip gp pv, one boot) dispatched: run 
  cd7443ee ckpt 2548: lab run 43928839921 (gp, head af4a8675): 21/21 checks, 0 issues = ANS57 prov
  11d927ab ckpt 2547: lab run 43927135769 (gp, head 42e933e5): Kestrel's OWN player passes every g
  d6fe07e1 ckpt 2546: heartbeat created: trig_01LyMjhKLgSaFigb7F61XAfe (every 2 h, bound to this s
  42e933e5 ckpt 2545: lab run 43925029376 (gv, head 3c1b38d8): pv scene 7/7 OK on the real engine 
  306ef253 ckpt 2544: dispatched lab gv run 43925029376 (head 3c1b38d8: input motionevent finger p
  3c1b38d8 ckpt 2543: lab touch fix coded (not yet run): raw dd finger paths never reached the app
  dcf083e4 ckpt 2542: ANS59a coded: regular tabs are not put to sleep by a trip to another app eit
```
