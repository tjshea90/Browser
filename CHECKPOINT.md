# CHECKPOINT 3311 — read me first, then TASKS.md

**Written:** 2026-11-25T01:05:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `5fbe60ec` (this checkpoint is the commit after it)

## Just done
OPT1c: lab opt v11 dispatched (run 45523035361, head 5fbe60ec)

## Do this next
Wait for run 45523035361 (the opt step may take up to ~22 min; expect it to end about 01:30Z; a send_later wake is set; do NOT cancel while a step hangs, the log is lost). Then list_workflow_jobs, get_job_logs of the 'lab / lab' job (saved file; parse with python3 -B -I: live:/opt/KestrelRestart/scene/ISSUE). Question to answer: did the run get past the first restart (KestrelRestart old process N gone, guard boot=1)? Where did it sit when it stopped? If it finished: Flavor.hasAutoTune=true in GeckoApp.kt, fill RESULT_PLACEHOLDER in docs/handoff/ALPHA107_RELEASE_NOTE.txt, bump geckoapp/build.gradle.kts 0.3.0-alpha107 / code 208, bash tools/sync-ext-version.sh, ship.sh gecko, ONE dispatch app=gecko full_build=true, get_release_by_tag, tools/record-release.sh, tick TASKS, answer the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5fbe60ec ckpt 3310: OPT1c lab v10 lost: opt step ran to its 17 min cap, then a dead emulator hun
  599acf9b ckpt 3308: OPT1c: lab opt v10 dispatched (run 45517648960, head d8c21030)
  d8c21030 ckpt 3307: OPT1c lab v9 read: the restart after a wedged page was lost because the old 
  11859008 ckpt 3304: OPT1c: lab opt v9 dispatched (run 45514217127, head 4a0912a0)
  4a0912a0 ckpt 3303: OPT1c lab v8 read: first full sample ok; gl page still wedges the emulator's
  1c21a8de ckpt 3301: OPT1c: lab opt v8 dispatched (run 45510570085, head e03c0fb6)
  e03c0fb6 ckpt 3300: OPT1c lab v7 read: the stall was a WebGL probe wedging the page process (not
  a1521080 ckpt 3297: OPT1c: lab opt v7 dispatched (run 45506674935, head b94293e1)
  b94293e1 ckpt 3296: OPT1c lab v6 read (first real samples; compat page hangs inside one probe; s
  1a07b8f7 ckpt 3294: OPT1c: lab opt v6 dispatched (run 45502159321, head ea602fba)
```
