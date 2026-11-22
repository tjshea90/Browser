# CHECKPOINT 2989 — read me first, then TASKS.md

**Written:** 2026-11-22T08:31:23Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-144aedec-7apwin` · **builds on:** `117a4f55` (this checkpoint is the commit after it)

## Just done
alpha94 (gecko code 195) gated by ship.sh (web 683/683, core+app suites, lint) and ONE dispatch made on ccr-144aedec-7apwin; contents: PERF9 gate asks Kestrel's player for a tap on a held script player that hit no control + calm mark labelled Play video

## Do this next
Next: do NOT dispatch again. (1) wait for tag kestrel-g-v0.3.0-alpha94 (git ls-remote --tags origin) and the newest android.yml run of this branch completed success (mcp__github__actions_list list_workflow_runs branch ccr-144aedec-7apwin perPage 1, then list_workflow_jobs); (2) mcp__github__get_release_by_tag; (3) bash tools/record-release.sh kestrel-g-v0.3.0-alpha94 with the IDENTICAL note in docs/handoff/ALPHA94_RELEASE_NOTE.txt (tr -d newline); (4) answer the owner with the Release link; then next upload: a hand-declined press line for the SECOND film, 'video calm' lines, whether the clipped circle persists (it is NOT page layout: cutBottom -145, Safe drawing applied: next engine candidates pbo-uploads=true / layer-compositor=false / GPU process, or the circle is a small svg drawn by WebRender)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  117a4f55 ckpt 2988: gated kestrel-g-v0.3.0-alpha94 (gecko code 195) and pushed it: handoff self-
  6fcdd4e1 ckpt 2986: pre-ship: alpha94: the second film that never played. Your upload shows what
  ae98cd8b ckpt 2978: PERF9 written to TASKS.md (owner after alpha93: one video worked, another sh
  c97f7f67 ckpt 2977: alpha93 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha93 (gecko code 194, run
  a882ac50 ckpt 2974: alpha93 (gecko code 194) gated by ship.sh and ONE dispatch made on ccr-144ae
  5488bb85 ckpt 2973: gated kestrel-g-v0.3.0-alpha93 (gecko code 194) and pushed it: handoff self-
  bf393943 ckpt 2971: pre-ship: alpha93: quality button in the full-screen player, the stuck loadi
  335cc029 ckpt 2968: PERF8 code (not released): (a) VideoFlow.offerSiblings gives the player that
  06774fc1 ckpt 2959: PERF8 written to TASKS.md (owner after alpha92: fullscreen Kestrel player wo
  f41f8f8b ckpt 2958: alpha92 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha92 (gecko code 193, run
```
