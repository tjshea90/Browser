# CHECKPOINT 2592 — read me first, then TASKS.md

**Written:** 2026-11-17T02:07:08Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `c59a2c9a` (this checkpoint is the commit after it)

## Just done
LAB3e: lab run 2 DISPATCHED ONCE: android.yml run 43997607118 (app=lab lab_suite=daily lab_minutes=45 lab_net=full lab_profile=auto, head c59a2c9a, started 01:42Z, x86_64 image + low-RAM prop attempt + font scale 1.2). HEARTBEAT trigger trig_01K9rvGwTaZNy2NbUQeBeWMg (every 2 h, bound to this session; delete it with delete_trigger when LAB3 is answered). Do NOT dispatch again until 43997607118 ends and its failure/finding is read

## Do this next
When run 43997607118 ends (about 1-1.5 h): mcp__github__actions_list list_workflow_jobs resource_id 43997607118 (the job named 'lab / lab'; failed step names tell where). If green or red-in-driver: list_workflow_run_artifacts 43997607118, download build-report + emulator-lab-text via actions_get download_workflow_run_artifact and python3 -I tools/emu/fetch_artifact.py <url> <empty dir under the scratchpad>; read BUILD-REPORT.md, ui-*.txt (real control names), events.jsonl; fix tools/emu/daily.py faults; write research/lab-baseline.json via buildreport.py --write-baseline once the numbers are trustworthy; then LAB3f improvement research (delegate the 'already done / already refused' check against research/*.md to ONE Explore agent), LAB3g answer the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  c59a2c9a ckpt 2591: LAB3 run 1 (43997001918) failed at the Gradle step: Mozilla publishes NO gec
  3b71b73b ckpt 2590: LAB3d done: tools/emu/buildreport.py (build identity from geckoapp/build.gra
  a198ecf8 ckpt 2589: LAB3e: first daily-suite run DISPATCHED ONCE: android.yml run 43997001918 (a
  f1cb9443 ckpt 2588: LAB3a/b/c code written: emu-lab.yml (profile input: tablet = 32-bit x86 Andr
  2d9c59f4 ckpt 2587: LAB3 (owner message 62: github runner + hardware-matched emulator + daily-us
  fe809368 ckpt 2586: alpha62 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha62 (code 163, ru
  a29d8d50 ckpt 2585: alpha62 build DISPATCHED ONCE: run 43989321170 (app=gecko full_build=true, h
  e7fd1dd9 ckpt 2584: gated kestrel-g-v0.3.0-alpha62 (gecko code 163) and pushed it: handoff self-
  75920d6d ckpt 2583: ANS61a-c ticked in TASKS.md with evidence: lab ho run 43986542855 = 61 check
  eb522196 ckpt 2582: alpha62 prepared but NOT shipped: geckoapp versionCode 163 / 0.3.0-alpha62, 
```
