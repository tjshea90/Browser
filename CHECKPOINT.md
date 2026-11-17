# CHECKPOINT 2595 — read me first, then TASKS.md

**Written:** 2026-11-17T02:29:09Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `55f60b95` (this checkpoint is the commit after it)

## Just done
LAB3e: lab run 2 (43997607118) READ. The whole pipeline works on the real emulator (build, boot, 15-step day, report, artifacts). Real results: cold start am 1667 ms / engine ready 2.2 s, a 1178 ms late UI heartbeat at start (tablet 865 ms), film tap-to-picture 0.7 s, 0 UI stalls, memory peak 292 MB with 7 of 8 tabs hibernated, referer trimmed cross-site, GPC header sent, WebRTC masked, third-party cookie not shared, clipboard refused. FOUR lab false positives found by reading the raw logs and FIXED in the lab (not browser faults): bad certificates (the app logs 'page loaded ok=false' for its refused load; Diagnostics says FAILED), pop-up (the app blocks it and shows its chip, window.open returns a stand-in), form typing (taps missed; now uses page-reported coordinates and compares what arrived), per-page console error attribution; plus: low_ram not applied (outcome now recorded), ui-*.txt and device.json now kept in the artifacts, bench calibration uses the fastest run, UI late-beat finding added. Candidate 1 downgraded (data contradicts it), candidate 3 confirmed

## Do this next
Dispatch lab run 3 ONCE (android.yml app=lab lab_suite=daily lab_minutes=45 lab_net=full lab_profile=auto, ref ccr-e89b9dfe-pl3lk5; get its id with list_workflow_runs perPage 1 + workflow_runs_filter branch), then read ui-*.txt for the real control names and fix the typed-address step; then baseline (buildreport --write-baseline research/lab-baseline.json), docs/handoff/LABREPORT_0.3.0-alpha62.md, LAB3g answer, delete heartbeat trig_01K9rvGwTaZNy2NbUQeBeWMg

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/LAB3_IMPROVEMENTS.md

## Last ten checkpoints
```
  e61155d2 ckpt 2594: LAB3f: the speed ledger + 8 NEW ranked candidates are saved in research/LAB3
  a3b36287 ckpt 2593: LAB3 docs: research/LAB3_DAILY.md (how to start, what is matched / not, the 
  a1a955e8 ckpt 2592: LAB3e: lab run 2 DISPATCHED ONCE: android.yml run 43997607118 (app=lab lab_s
  c59a2c9a ckpt 2591: LAB3 run 1 (43997001918) failed at the Gradle step: Mozilla publishes NO gec
  3b71b73b ckpt 2590: LAB3d done: tools/emu/buildreport.py (build identity from geckoapp/build.gra
  a198ecf8 ckpt 2589: LAB3e: first daily-suite run DISPATCHED ONCE: android.yml run 43997001918 (a
  f1cb9443 ckpt 2588: LAB3a/b/c code written: emu-lab.yml (profile input: tablet = 32-bit x86 Andr
  2d9c59f4 ckpt 2587: LAB3 (owner message 62: github runner + hardware-matched emulator + daily-us
  fe809368 ckpt 2586: alpha62 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha62 (code 163, ru
  a29d8d50 ckpt 2585: alpha62 build DISPATCHED ONCE: run 43989321170 (app=gecko full_build=true, h
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
