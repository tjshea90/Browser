# CHECKPOINT 3313 — read me first, then TASKS.md

**Written:** 2026-11-25T01:34:59Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `7437bd58` (this checkpoint is the commit after it)

## Just done
OPT1c lab v11 read (log readable thanks to the step timeout): first restart OK; WebGL context creation kills the emulator's drawing stack and the app with it. v12: gl page skipped in the lab quick run, and skipped for the rest of a real run after its first hang; compile + OwnerRulesTest green

## Do this next
Dispatch lab v12 (android.yml app=lab lab_suite=opt lab_profile=standard lab_net=full lab_minutes=5 full_build=false ref=claude/kind-brahmagupta-ae3yl8), record run id in a ckpt, send_later ~16 min (the quick plan should finish in ~10 min), read get_job_logs of 'lab / lab' (parse live:/opt/KestrelRestart/scene/ISSUE). Expect: 14-16 samples, restarts for gcpar/fps30/combined/closing, a finished last.json. If it finishes: Flavor.hasAutoTune=true in GeckoApp.kt, fill RESULT_PLACEHOLDER in docs/handoff/ALPHA107_RELEASE_NOTE.txt, bump geckoapp/build.gradle.kts 0.3.0-alpha107 / code 208, bash tools/sync-ext-version.sh, ship.sh gecko, ONE dispatch app=gecko full_build=true, get_release_by_tag, tools/record-release.sh, tick TASKS, answer the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M research/OPTIMIZER.md

## Last ten checkpoints
```
  ef7e930c ckpt 3311: OPT1c: lab opt v11 dispatched (run 45523035361, head 5fbe60ec)
  5fbe60ec ckpt 3310: OPT1c lab v10 lost: opt step ran to its 17 min cap, then a dead emulator hun
  599acf9b ckpt 3308: OPT1c: lab opt v10 dispatched (run 45517648960, head d8c21030)
  d8c21030 ckpt 3307: OPT1c lab v9 read: the restart after a wedged page was lost because the old 
  11859008 ckpt 3304: OPT1c: lab opt v9 dispatched (run 45514217127, head 4a0912a0)
  4a0912a0 ckpt 3303: OPT1c lab v8 read: first full sample ok; gl page still wedges the emulator's
  1c21a8de ckpt 3301: OPT1c: lab opt v8 dispatched (run 45510570085, head e03c0fb6)
  e03c0fb6 ckpt 3300: OPT1c lab v7 read: the stall was a WebGL probe wedging the page process (not
  a1521080 ckpt 3297: OPT1c: lab opt v7 dispatched (run 45506674935, head b94293e1)
  b94293e1 ckpt 3296: OPT1c lab v6 read (first real samples; compat page hangs inside one probe; s
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
