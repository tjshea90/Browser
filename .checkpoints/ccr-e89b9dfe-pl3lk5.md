# CHECKPOINT 2590 — read me first, then TASKS.md

**Written:** 2026-11-17T01:41:21Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `143839cd` (this checkpoint is the commit after it)

## Just done
LAB3d done: tools/emu/buildreport.py (build identity from geckoapp/build.gradle.kts, verdict per area, hardware-fidelity table from device.json, ranked findings with code hints and grouping, per-step/page/security tables, baseline delta normalised by the /bench CPU page, --notes for the hand-written improvement section, --write-baseline). Tested on REAL data (compat run 43784927532 artifact: 25 raw findings grouped to 6) and on the selftest day. handoff self-test 69/0. Lab run 43997001918 is running

## Do this next
When run 43997001918 ends: mcp__github__actions_list list_workflow_jobs resource_id 43997001918 (small); download artifacts emulator-lab-text + build-report (list_workflow_run_artifacts then actions_get download_workflow_run_artifact, python3 -I tools/emu/fetch_artifact.py <url> <empty dir under scratchpad>); read BUILD-REPORT.md, ui-*.txt, events.jsonl; fix the lab's own faults in tools/emu/daily.py (control names from ui-*.txt), re-dispatch ONLY after reading why it failed

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M tools/emu/buildreport.py

## Last ten checkpoints
```
  a198ecf8 ckpt 2589: LAB3e: first daily-suite run DISPATCHED ONCE: android.yml run 43997001918 (a
  f1cb9443 ckpt 2588: LAB3a/b/c code written: emu-lab.yml (profile input: tablet = 32-bit x86 Andr
  2d9c59f4 ckpt 2587: LAB3 (owner message 62: github runner + hardware-matched emulator + daily-us
  fe809368 ckpt 2586: alpha62 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha62 (code 163, ru
  a29d8d50 ckpt 2585: alpha62 build DISPATCHED ONCE: run 43989321170 (app=gecko full_build=true, h
  e7fd1dd9 ckpt 2584: gated kestrel-g-v0.3.0-alpha62 (gecko code 163) and pushed it: handoff self-
  75920d6d ckpt 2583: ANS61a-c ticked in TASKS.md with evidence: lab ho run 43986542855 = 61 check
  eb522196 ckpt 2582: alpha62 prepared but NOT shipped: geckoapp versionCode 163 / 0.3.0-alpha62, 
  241f219d ckpt 2581: lab ho run 43986542855 DISPATCHED (head 977ab6a8, app=lab lab_suite=ho): pro
  977ab6a8 ckpt 2580: ANS61c: lab variants preroll (ad-state class, ad set up before the press) + 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
