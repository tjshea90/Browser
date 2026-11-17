# CHECKPOINT 2594 — read me first, then TASKS.md

**Written:** 2026-11-17T02:23:19Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `a3b36287` (this checkpoint is the commit after it)

## Just done
LAB3f: the speed ledger + 8 NEW ranked candidates are saved in research/LAB3_IMPROVEMENTS.md (this file is the report's section 6, pass --notes); candidates 1 (cold-start blocking waits for the saved engine) and 2 (synchronous capturePixels read-back inside the navigation decision) verified against the code by reading; TASKS LAB3f-1..4 added. Run 43997607118 still running

## Do this next
At the wake-up (send_later fires 03:05Z) or earlier: list_workflow_jobs 43997607118; read the run; fix daily.py from ui-*.txt; then decide which of LAB3f-1/2 to implement (1 is JS-only with a web test; 2 first logs readBack duration). Do NOT ship an APK unless the owner asks (the request was a report for a build)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
    ?? research/LAB3_IMPROVEMENTS.md

## Last ten checkpoints
```
  a3b36287 ckpt 2593: LAB3 docs: research/LAB3_DAILY.md (how to start, what is matched / not, the 
  a1a955e8 ckpt 2592: LAB3e: lab run 2 DISPATCHED ONCE: android.yml run 43997607118 (app=lab lab_s
  c59a2c9a ckpt 2591: LAB3 run 1 (43997001918) failed at the Gradle step: Mozilla publishes NO gec
  3b71b73b ckpt 2590: LAB3d done: tools/emu/buildreport.py (build identity from geckoapp/build.gra
  a198ecf8 ckpt 2589: LAB3e: first daily-suite run DISPATCHED ONCE: android.yml run 43997001918 (a
  f1cb9443 ckpt 2588: LAB3a/b/c code written: emu-lab.yml (profile input: tablet = 32-bit x86 Andr
  2d9c59f4 ckpt 2587: LAB3 (owner message 62: github runner + hardware-matched emulator + daily-us
  fe809368 ckpt 2586: alpha62 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha62 (code 163, ru
  a29d8d50 ckpt 2585: alpha62 build DISPATCHED ONCE: run 43989321170 (app=gecko full_build=true, h
  e7fd1dd9 ckpt 2584: gated kestrel-g-v0.3.0-alpha62 (gecko code 163) and pushed it: handoff self-
```
