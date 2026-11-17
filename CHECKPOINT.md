# CHECKPOINT 2638 — read me first, then TASKS.md

**Written:** 2026-11-17T17:27:25Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `d728f0a2` (this checkpoint is the commit after it)

## Just done
LAB7 first lab run 44126148207 READ (research/LAB7_RESULTS.md next): (1) the whole app compiles in 2.2 s on the emulator (sp-all) vs 1.3 s (sp-current); code 12.9 MB vs 3.7 MB; speed-profile also writes an app image base.art (936 KB with the catch-all vs 668 KB); start times and PSS inside the noise (n=2); (2) NO first-launch-after-update penalty: new build launch 1 = -8 pct vs the old build steady state, a warm-up launch gains nothing: a post-update warm-up is NOT built; Gecko keeps its start-up cache across the update (compatibility.ini rewritten, scriptCache grows in the first run). The emulator's system server died in round 2 (cmd: Can't find service). SHIPPING CHANGE COMMITTED: geckoapp/src/main/baseline-prof.txt ends with the catch-all rule HL**;->**(**)**, profile-to-baseline.py keeps it, tools/check-profile-coverage.sh + android.yml gate, OwnerRulesTest.lab7WholeAppIsCompiledAtInstall (passes), lab: aotlab breadth_check with link probes, meminfo kinds, recover(); workflow aot step modes dm|breadth|full

## Do this next
dispatch the confirmation lab run ONCE: android.yml app=lab lab_suite=aot lab_profile=standard lab_net=full lab_minutes=breadth (6 rounds sp-current vs sp-all, links opened in the running app, memory by kind); read it; then write research/LAB7_RESULTS.md, HANDOFF, then alpha67: versionCode 168 / 0.3.0-alpha67 (alpha66 is shipped by the other lane: tag kestrel-g-v0.3.0-alpha66 exists), bash tools/sync-ext-version.sh, docs/handoff/ALPHA67_RELEASE_NOTE.txt, bash ship.sh gecko, ONE dispatch app=gecko full_build=true, verify, record-release

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M .github/workflows/emu-lab.yml

## Last ten checkpoints
```
  5b6a24c8 ckpt 2637: LAB7 CORRECTED lab run DISPATCHED ONCE: android.yml run 44126148207 (run_num
  e40a5b70 ckpt 2636: LAB7 lab run 44125632537 CANCELLED ON PURPOSE and replaced by a corrected on
  3206d5dd ckpt 2635: LAB7 lab run DISPATCHED ONCE (android.yml app=lab lab_suite=aot lab_profile=
  eaa4e46d ckpt 2634: LAB7a+b lab code written and dry-run against a fake adb (aotlab.py breadth_c
  a2313194 ckpt 2634: gated kestrel-g-v0.3.0-alpha66 (gecko code 167) and pushed it: handoff self-
  60f8deca ckpt 2633: alpha65 SHIPPED and RECORDED (run 44120758574 all 7 jobs green, Release kest
  f7d26151 ckpt 2632: alpha65 run is android.yml run 44120758574 (run_number 216, started 16:00:17
  0fa23623 ckpt 2631: alpha65 DISPATCHED ONCE (compile at install = LAB6: release script publishes
  d32b8cf2 ckpt 2630: gated kestrel-g-v0.3.0-alpha65 (gecko code 166) and pushed it: handoff self-
  a6159180 ckpt 2629: OWNER SAID GO on LAB6 (compile at install through the updater). Recorded in 
```

(12 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
