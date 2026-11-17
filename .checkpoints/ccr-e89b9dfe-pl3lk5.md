# CHECKPOINT 2643 — read me first, then TASKS.md

**Written:** 2026-11-17T18:28:29Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `894cf0a1` (this checkpoint is the commit after it)

## Just done
alpha67 SHIPPED and RECORDED (Release kestrel-g-v0.3.0-alpha67 code 168, run 44146222603 all jobs green incl. the new coverage gate, APK 95.9 MB + sha256 + dm 4,132 bytes, BUILDLOG commit recorded). TASKS LAB7 ticked, HANDOFF alpha67 entry, research/LAB7_RESULTS.md + LAB7_INSTALL_COMPILE.md hold the evidence. DO NOT DISPATCH ALPHA67 AGAIN.

## Do this next
Nothing is running. Wait for the owner: update the tablet from alpha66 to alpha67 and send Diagnostics lines 'start oat' (base.odex about 12 MB = whole app compiled at install) and 'start art'; tablet test of alpha66 Swoop keyboard link (other lane); owner decisions still open: Neon idle pulse, alpha62 site. If more speed work is wanted: candidates not done = nothing with evidence; ideas: lab-only Health mirror on a release build to run the daily suite against compile modes, GPU first-frame stall (HardwareRenderer.nSetStopped 0.9 s at each cold start on emulator AND tablet) is platform GL init

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M HANDOFF.md
     M TASKS.md

## Last ten checkpoints
```
  765d1c8a ckpt 2642: alpha67 DISPATCHED ONCE (android.yml app=gecko full_build=true, ref ccr-e89b
  d8340975 ckpt 2641: gated kestrel-g-v0.3.0-alpha67 (gecko code 168) and pushed it: handoff self-
  fd0ea0b5 ckpt 2640: Diagnostics line 'start oat' added (BrowserActivity.oatSizes: sizes of oat/<
  63fdb931 ckpt 2639: LAB7 CONFIRMATION lab run DISPATCHED ONCE: android.yml run 44137227924 (run_
  ebd3854a ckpt 2638: LAB7 first lab run 44126148207 READ (research/LAB7_RESULTS.md next): (1) the
  5b6a24c8 ckpt 2637: LAB7 CORRECTED lab run DISPATCHED ONCE: android.yml run 44126148207 (run_num
  e40a5b70 ckpt 2636: LAB7 lab run 44125632537 CANCELLED ON PURPOSE and replaced by a corrected on
  3206d5dd ckpt 2635: LAB7 lab run DISPATCHED ONCE (android.yml app=lab lab_suite=aot lab_profile=
  eaa4e46d ckpt 2634: LAB7a+b lab code written and dry-run against a fake adb (aotlab.py breadth_c
  a2313194 ckpt 2634: gated kestrel-g-v0.3.0-alpha66 (gecko code 167) and pushed it: handoff self-
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
