# CHECKPOINT 2955 — read me first, then TASKS.md

**Written:** 2026-11-22T06:51:33Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-144aedec-7apwin` · **builds on:** `df0b4a8a` (this checkpoint is the commit after it)

## Just done
alpha92 (gecko code 193) gated by ship.sh (core+app suites, lint, handoff self-test green) and ONE dispatch made on ccr-144aedec-7apwin; contents: PERF5b quality choices from sibling lists in Kestrel's player (full-screen + inline), PERF7c Safe drawing lab switch (4 WebRender prefs, default ON), research/PAINT_GLITCH.md + VIDEO_NOLOAD.md notes

## Do this next
Next: do NOT dispatch again. (1) wait for tag kestrel-g-v0.3.0-alpha92 (git ls-remote --tags origin) and all jobs success (mcp__github__actions_list list_workflow_jobs on the newest android.yml run of this branch); (2) mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha92 shows the APK; (3) bash tools/record-release.sh kestrel-g-v0.3.0-alpha92 with the IDENTICAL note in docs/handoff/ALPHA92_RELEASE_NOTE.txt (tr -d newline); (4) tick PERF5b/PERF7 as SHIPPED-NOT-CONFIRMED in TASKS.md (claims PERF7, PERF5b are mine); (5) answer the owner with the Release link on its own line; then next owner upload: read engine prefs read-back for the 4 drawing prefs, rejected: lines for claude.ai, and PERF5b siblings / quality-switch lines

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  df0b4a8a ckpt 2954: gated kestrel-g-v0.3.0-alpha92 (gecko code 193) and pushed it: handoff self-
  2db9f72a ckpt 2952: pre-ship: alpha92: quality choices for pornhub in Kestrel's player, and a Sa
  ee6c59f9 ckpt 2938: PERF7c step 1: Settings > Network lab 'Safe drawing' (lab_draw, default ON) 
  449480c7 ckpt 2933: PERF7 started on branch ccr-144aedec-7apwin: owner request + 2 screenshots w
  4da54c18 ckpt 2915: alpha91 RELEASED and recorded (run 44987313373 green, Release has the APK). 
  a42e5268 ckpt 2913: alpha91 (code 192) gated (ship.sh green on attempt 3) and ONE dispatch made 
  df3d34d1 ckpt 2912: gated kestrel-g-v0.3.0-alpha91 (gecko code 192) and pushed it: handoff self-
  345be8b0 ckpt 2911: alpha91 ship attempt 3 running: attempt 1 red on OwnerRulesTest pin (10 s wi
  7693a032 ckpt 2908: pre-ship: alpha91: pornhub in Kestrel's own player, and the clipped circle. 
  32a10e12 ckpt 2906: pre-ship: alpha91: pornhub in Kestrel's own player, and the clipped circle. 
```
