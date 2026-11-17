# CHECKPOINT 2591 — read me first, then TASKS.md

**Written:** 2026-11-17T01:42:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `3b71b73b` (this checkpoint is the commit after it)

## Just done
LAB3 run 1 (43997001918) failed at the Gradle step: Mozilla publishes NO geckoview-x86 for GeckoView 160, so a 32-bit userland match is impossible; both lab profiles now use the x86_64 image (emu-lab.yml), the fidelity table says 'NOT matched' with the reason; the report step had also failed only because that run's commit predated buildreport.py. TASKS LAB3a updated

## Do this next
Dispatch lab run 2 ONCE (android.yml app=lab lab_suite=daily lab_minutes=45 lab_net=full lab_profile=auto ref ccr-e89b9dfe-pl3lk5), record its id in the next ckpt

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M .github/workflows/emu-lab.yml
     M TASKS.md
     M tools/emu/buildreport.py

## Last ten checkpoints
```
  3b71b73b ckpt 2590: LAB3d done: tools/emu/buildreport.py (build identity from geckoapp/build.gra
  a198ecf8 ckpt 2589: LAB3e: first daily-suite run DISPATCHED ONCE: android.yml run 43997001918 (a
  f1cb9443 ckpt 2588: LAB3a/b/c code written: emu-lab.yml (profile input: tablet = 32-bit x86 Andr
  2d9c59f4 ckpt 2587: LAB3 (owner message 62: github runner + hardware-matched emulator + daily-us
  fe809368 ckpt 2586: alpha62 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha62 (code 163, ru
  a29d8d50 ckpt 2585: alpha62 build DISPATCHED ONCE: run 43989321170 (app=gecko full_build=true, h
  e7fd1dd9 ckpt 2584: gated kestrel-g-v0.3.0-alpha62 (gecko code 163) and pushed it: handoff self-
  75920d6d ckpt 2583: ANS61a-c ticked in TASKS.md with evidence: lab ho run 43986542855 = 61 check
  eb522196 ckpt 2582: alpha62 prepared but NOT shipped: geckoapp versionCode 163 / 0.3.0-alpha62, 
  241f219d ckpt 2581: lab ho run 43986542855 DISPATCHED (head 977ab6a8, app=lab lab_suite=ho): pro
```
