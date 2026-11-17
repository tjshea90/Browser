# CHECKPOINT 2593 — read me first, then TASKS.md

**Written:** 2026-11-17T02:09:13Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `dfe28c38` (this checkpoint is the commit after it)

## Just done
LAB3 docs: research/LAB3_DAILY.md (how to start, what is matched / not, the 15 steps, the probes, how to read and refresh the report), EMU_LAB.md pointer, android.yml lab_suite text mentions daily (an apostrophe in the YAML string broke the parse; the handoff self-test caught it, fixed). Background agent a1df5d3af9c8799c7 is building the already-done / already-refused ledger of speed measures + new ideas (LAB3f); run 43997607118 is running

## Do this next
Wait for (a) the speed-ledger agent's notification, (b) run 43997607118 to end: list_workflow_jobs resource_id 43997607118; then follow the previous Do-this-next (download build-report + emulator-lab-text, read ui-*.txt and events.jsonl, fix daily.py, baseline, LAB3f notes file research/LAB3_IMPROVEMENTS.md, LAB3g answer)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a1a955e8 ckpt 2592: LAB3e: lab run 2 DISPATCHED ONCE: android.yml run 43997607118 (app=lab lab_s
  c59a2c9a ckpt 2591: LAB3 run 1 (43997001918) failed at the Gradle step: Mozilla publishes NO gec
  3b71b73b ckpt 2590: LAB3d done: tools/emu/buildreport.py (build identity from geckoapp/build.gra
  a198ecf8 ckpt 2589: LAB3e: first daily-suite run DISPATCHED ONCE: android.yml run 43997001918 (a
  f1cb9443 ckpt 2588: LAB3a/b/c code written: emu-lab.yml (profile input: tablet = 32-bit x86 Andr
  2d9c59f4 ckpt 2587: LAB3 (owner message 62: github runner + hardware-matched emulator + daily-us
  fe809368 ckpt 2586: alpha62 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha62 (code 163, ru
  a29d8d50 ckpt 2585: alpha62 build DISPATCHED ONCE: run 43989321170 (app=gecko full_build=true, h
  e7fd1dd9 ckpt 2584: gated kestrel-g-v0.3.0-alpha62 (gecko code 163) and pushed it: handoff self-
  75920d6d ckpt 2583: ANS61a-c ticked in TASKS.md with evidence: lab ho run 43986542855 = 61 check
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
