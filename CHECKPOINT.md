# CHECKPOINT 3307 — read me first, then TASKS.md

**Written:** 2026-11-25T00:22:46Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `0a52f63e` (this checkpoint is the commit after it)

## Just done
OPT1c lab v9 read: the restart after a wedged page was lost because the old process needed 12 s to die and the trampoline started the new one after 300 ms. v10: RestartActivity waits for /proc/<pid> to vanish (max 30 s) + logs KestrelRestart lines; compile + OwnerRulesTest green

## Do this next
Dispatch lab v10 (android.yml app=lab lab_suite=opt lab_profile=standard lab_net=full lab_minutes=5 full_build=false ref=claude/kind-brahmagupta-ae3yl8), record run id in a ckpt, send_later ~22 min, then get_job_logs of 'lab / lab' (saved file; parse with python3 -B -I: lines with live: / opt / KestrelRestart / scene / ISSUE). Expect: 'KestrelRestart old process N gone', guard boot=1 in the new process, more samples ok, restarts for gcpar/fps30, a finished run (last.json). If it finishes: Flavor.hasAutoTune=true in GeckoApp.kt, fill RESULT_PLACEHOLDER in docs/handoff/ALPHA107_RELEASE_NOTE.txt, bump geckoapp/build.gradle.kts 0.3.0-alpha107 / code 208, bash tools/sync-ext-version.sh, ship.sh gecko, ONE dispatch, get_release_by_tag, tools/record-release.sh, tick TASKS, answer the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M research/OPTIMIZER.md

## Last ten checkpoints
```
  11859008 ckpt 3304: OPT1c: lab opt v9 dispatched (run 45514217127, head 4a0912a0)
  4a0912a0 ckpt 3303: OPT1c lab v8 read: first full sample ok; gl page still wedges the emulator's
  1c21a8de ckpt 3301: OPT1c: lab opt v8 dispatched (run 45510570085, head e03c0fb6)
  e03c0fb6 ckpt 3300: OPT1c lab v7 read: the stall was a WebGL probe wedging the page process (not
  a1521080 ckpt 3297: OPT1c: lab opt v7 dispatched (run 45506674935, head b94293e1)
  b94293e1 ckpt 3296: OPT1c lab v6 read (first real samples; compat page hangs inside one probe; s
  1a07b8f7 ckpt 3294: OPT1c: lab opt v6 dispatched (run 45502159321, head ea602fba)
  ea602fba ckpt 3293: OPT1c lab v5 read: rAF still dead in the Auto-tune view on the emulator; scr
  66748756 ckpt 3288: OPT1c: lab opt v5 dispatched (run 45496974270, head 371afa1e, includes setAc
  371afa1e ckpt 3287: OPT1c lab v4 read: anim page timed out because the Auto-tune session was nev
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
