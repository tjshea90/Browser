# CHECKPOINT 2596 — read me first, then TASKS.md

**Written:** 2026-11-17T02:29:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `b52ed4c3` (this checkpoint is the commit after it)

## Just done
LAB3e: lab run 3 DISPATCHED ONCE: android.yml run 44003337190 (daily, head b52ed4c3, started 02:29Z) with the lab fixes (bad-cert / pop-up / form / mixed-content judged by the app's own signals; ui-*.txt + device.json + lowram-attempt.txt kept in the artifacts). The send_later wake at 03:05Z was written for run 2: it should read run 44003337190 instead. Scope decision: the owner asked for a REPORT (findings + ways to improve), so no app code changes and no APK release in this task; LAB3f-2/3 first steps (log readBack duration, time the start-up burst) are offered to the owner in the answer

## Do this next
When run 44003337190 ends (~15 min): list_workflow_jobs 44003337190; download artifacts build-report + emulator-lab-text (list_workflow_run_artifacts, actions_get download_workflow_run_artifact, python3 -I tools/emu/fetch_artifact.py <url> <empty dir under the scratchpad>); read BUILD-REPORT.md + ui-*.txt + lowram-attempt.txt; fix the typed-address step from ui-*.txt if it still cannot find the control; rerun ONLY if a lab fault remains; then: python3 -I tools/emu/buildreport.py --out <dir> --repo-root . --suite daily --profile tablet --run-id N --notes research/LAB3_IMPROVEMENTS.md --write-baseline research/lab-baseline.json, copy BUILD-REPORT.md to docs/handoff/LABREPORT_0.3.0-alpha62.md, tick LAB3 items with evidence (tools/claim.sh style), answer the owner, delete heartbeat trig_01K9rvGwTaZNy2NbUQeBeWMg

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b52ed4c3 ckpt 2595: LAB3e: lab run 2 (43997607118) READ. The whole pipeline works on the real em
  e61155d2 ckpt 2594: LAB3f: the speed ledger + 8 NEW ranked candidates are saved in research/LAB3
  a3b36287 ckpt 2593: LAB3 docs: research/LAB3_DAILY.md (how to start, what is matched / not, the 
  a1a955e8 ckpt 2592: LAB3e: lab run 2 DISPATCHED ONCE: android.yml run 43997607118 (app=lab lab_s
  c59a2c9a ckpt 2591: LAB3 run 1 (43997001918) failed at the Gradle step: Mozilla publishes NO gec
  3b71b73b ckpt 2590: LAB3d done: tools/emu/buildreport.py (build identity from geckoapp/build.gra
  a198ecf8 ckpt 2589: LAB3e: first daily-suite run DISPATCHED ONCE: android.yml run 43997001918 (a
  f1cb9443 ckpt 2588: LAB3a/b/c code written: emu-lab.yml (profile input: tablet = 32-bit x86 Andr
  2d9c59f4 ckpt 2587: LAB3 (owner message 62: github runner + hardware-matched emulator + daily-us
  fe809368 ckpt 2586: alpha62 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha62 (code 163, ru
```
