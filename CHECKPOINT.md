# CHECKPOINT 2647 — read me first, then TASKS.md

**Written:** 2026-11-18T01:20:07Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `f346b9f1` (this checkpoint is the commit after it)

## Just done
CR1: lab run DISPATCHED ONCE: android.yml run 44214894368 (run_number 223, app=lab lab_suite=yt lab_minutes=32 lab_profile=tablet lab_net=full, ref ccr-587430ac-royssd, head f346b9f1, queued 01:19Z). DO NOT DISPATCH ANOTHER yt RUN UNTIL IT IS READ. Poll with mcp__github__actions_list list_workflow_jobs resource_id 44214894368 (small); never list_workflow_runs without per_page 1 and a branch filter (78 KB answer); artifact: emulator-lab-text via actions_get download_workflow_run_artifact then python3 -I tools/emu/fetch_artifact.py <url> <empty dir>.

## Do this next
while the lab runs: write research/CR1_YOUTUBE_CRASH.md (facts so far), then build CR1c = RunMarker/flight recorder (app/diag): a small file written on lifecycle (fg/bg), page loads, trims, gecko kill/crash, read at next start -> Diagnostics line 'Last run ended'; read 44214894368 when done

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f346b9f1 ckpt 2646: CR1: added the lab suite 'yt' (tools/emu/soak.py yt_journey + yt_after + mem
  05bdbf31 ckpt 2645: CR1 written into TASKS.md in the owner's words (YouTube crash investigation)
  cef3338a ckpt 2644: gated kestrel-g-v0.3.0-alpha68 (gecko code 169) and pushed it: handoff self-
  e38fb0d8 ckpt 2643: alpha67 SHIPPED and RECORDED (Release kestrel-g-v0.3.0-alpha67 code 168, run
  765d1c8a ckpt 2642: alpha67 DISPATCHED ONCE (android.yml app=gecko full_build=true, ref ccr-e89b
  d8340975 ckpt 2641: gated kestrel-g-v0.3.0-alpha67 (gecko code 168) and pushed it: handoff self-
  fd0ea0b5 ckpt 2640: Diagnostics line 'start oat' added (BrowserActivity.oatSizes: sizes of oat/<
  63fdb931 ckpt 2639: LAB7 CONFIRMATION lab run DISPATCHED ONCE: android.yml run 44137227924 (run_
  ebd3854a ckpt 2638: LAB7 first lab run 44126148207 READ (research/LAB7_RESULTS.md next): (1) the
  5b6a24c8 ckpt 2637: LAB7 CORRECTED lab run DISPATCHED ONCE: android.yml run 44126148207 (run_num
```
