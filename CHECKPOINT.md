# CHECKPOINT 2589 — read me first, then TASKS.md

**Written:** 2026-11-17T01:38:10Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `f1cb9443` (this checkpoint is the commit after it)

## Just done
LAB3e: first daily-suite run DISPATCHED ONCE: android.yml run 43997001918 (app=lab lab_suite=daily lab_minutes=45 lab_net=full lab_profile=auto, ref ccr-e89b9dfe-pl3lk5, head f1cb9443): do NOT dispatch again until it ends; check with mcp__github__actions_list list_workflow_jobs resource_id 43997001918 (NOT list_workflow_runs: ~20 KB of release notes per call). Unproven risks: geckoview-x86 resolves, 32-bit x86 Android 10 image boots with KVM, emulator accepts -prop ro.config.low_ram=true (the workflow falls back without it), the ui-nodes of the real app

## Do this next
LAB3d: write tools/emu/buildreport.py against a real older artifact (try run 43784927532 compat or 43971358668 vp: mcp__github__actions_list list_workflow_run_artifacts, then actions_get download_workflow_run_artifact, python3 -I tools/emu/fetch_artifact.py <url> <empty dir under scratchpad>); when run 43997001918 ends download its artifacts emulator-lab-text + build-report and read ui-*.txt / events.jsonl

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f1cb9443 ckpt 2588: LAB3a/b/c code written: emu-lab.yml (profile input: tablet = 32-bit x86 Andr
  2d9c59f4 ckpt 2587: LAB3 (owner message 62: github runner + hardware-matched emulator + daily-us
  fe809368 ckpt 2586: alpha62 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha62 (code 163, ru
  a29d8d50 ckpt 2585: alpha62 build DISPATCHED ONCE: run 43989321170 (app=gecko full_build=true, h
  e7fd1dd9 ckpt 2584: gated kestrel-g-v0.3.0-alpha62 (gecko code 163) and pushed it: handoff self-
  75920d6d ckpt 2583: ANS61a-c ticked in TASKS.md with evidence: lab ho run 43986542855 = 61 check
  eb522196 ckpt 2582: alpha62 prepared but NOT shipped: geckoapp versionCode 163 / 0.3.0-alpha62, 
  241f219d ckpt 2581: lab ho run 43986542855 DISPATCHED (head 977ab6a8, app=lab lab_suite=ho): pro
  977ab6a8 ckpt 2580: ANS61c: lab variants preroll (ad-state class, ad set up before the press) + 
  25e3ced5 ckpt 2579: ANS61: :core:test 1413/0 failed (OwnerRulesTest guard added), :geckoapp:comp
```
