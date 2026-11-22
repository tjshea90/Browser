# CHECKPOINT 2974 — read me first, then TASKS.md

**Written:** 2026-11-22T07:43:46Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-144aedec-7apwin` · **builds on:** `5488bb85` (this checkpoint is the commit after it)

## Just done
alpha93 (gecko code 194) gated by ship.sh and ONE dispatch made on ccr-144aedec-7apwin (web 681/681, core+app suites, lint green); contents: PERF8a late quality list, PERF8b calm.js, PERF8c draw-prefs line

## Do this next
Next: do NOT dispatch again. (1) wait for tag kestrel-g-v0.3.0-alpha93 (git ls-remote --tags origin) and all jobs success (mcp__github__actions_list list_workflow_jobs on the newest android.yml run of this branch); (2) mcp__github__get_release_by_tag shows the APK; (3) bash tools/record-release.sh kestrel-g-v0.3.0-alpha93 with the IDENTICAL note in docs/handoff/ALPHA93_RELEASE_NOTE.txt (tr -d newline); (4) TASKS.md: PERF8a/b/c shipped-not-confirmed; (5) answer the owner with the Release link; then the next upload: lines 'video calm' (element + parent clip), 'engine draw-prefs', 'inline/native siblings (found after the start)'

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5488bb85 ckpt 2973: gated kestrel-g-v0.3.0-alpha93 (gecko code 194) and pushed it: handoff self-
  bf393943 ckpt 2971: pre-ship: alpha93: quality button in the full-screen player, the stuck loadi
  335cc029 ckpt 2968: PERF8 code (not released): (a) VideoFlow.offerSiblings gives the player that
  06774fc1 ckpt 2959: PERF8 written to TASKS.md (owner after alpha92: fullscreen Kestrel player wo
  f41f8f8b ckpt 2958: alpha92 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha92 (gecko code 193, run
  292067e6 ckpt 2955: alpha92 (gecko code 193) gated by ship.sh (core+app suites, lint, handoff se
  df0b4a8a ckpt 2954: gated kestrel-g-v0.3.0-alpha92 (gecko code 193) and pushed it: handoff self-
  2db9f72a ckpt 2952: pre-ship: alpha92: quality choices for pornhub in Kestrel's player, and a Sa
  ee6c59f9 ckpt 2938: PERF7c step 1: Settings > Network lab 'Safe drawing' (lab_draw, default ON) 
  449480c7 ckpt 2933: PERF7 started on branch ccr-144aedec-7apwin: owner request + 2 screenshots w
```
