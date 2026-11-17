# CHECKPOINT 2641 — read me first, then TASKS.md

**Written:** 2026-11-17T18:17:05Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `9193a212` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha67 (gecko code 168) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-e89b9dfe-pl3lk5, inputs {"app":"gecko","full_build":"true","note":"alpha67: the whole app is compiled while it installs, the longer install for a faster browser that you asked for. Until now Android compiled only about 2500 of the app's 15000 methods when an update installed (the ones my start-up profile marked hot) and ran all the rest in its slow interpreter until the tablet was idle and charging. Now one more rule marks every method hot, so Android compiles ALL of it while the update installs. In the test phone that took 1 second longer (1.2 to 2.3 seconds), made 12 MB of compiled code instead of 3 MB, and changed nothing else I could measure: start-up, opening a link in the running browser and memory were the same, so I expect about 10 to 20 seconds more install time on the tablet and a little less lag the first time you use a menu, the tab list or a settings screen (not measured on the tablet). The start-up file the updater sends got smaller (4 KB). I also tested the other half of your idea, whether the first start after an update is slower than later ones (so a warm-up run right after the update could help): it is not, so I did not build one. This release also carries the Swoop keyboard engine fix made after alpha66 (Kestrel's own keyboard behaves as before) and a new Diagnostics line, start oat, with the sizes of the compiled-code files: send it after the update to prove that the whole app was compiled (about 12 MB for base.odex; about 3 MB means the old small profile). IMPORTANT: like alpha65, this only works for updates that come FROM alpha65 or alpha66, because they carry the updater that sends the start-up file: so the update from alpha66 to alpha67 is the first one compiled whole. NOT proven on the tablet: Android Go may ignore the file (then the nightly compile when the tablet is idle and charging does the same job later, now for the whole app). If an update ever fails to install, tell me the exact words Android shows."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha67), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha67 "alpha67: the whole app is compiled while it installs, the longer install for a faster browser that you asked for. Until now Android compiled only about 2500 of the app's 15000 methods when an update installed (the ones my start-up profile marked hot) and ran all the rest in its slow interpreter until the tablet was idle and charging. Now one more rule marks every method hot, so Android compiles ALL of it while the update installs. In the test phone that took 1 second longer (1.2 to 2.3 seconds), made 12 MB of compiled code instead of 3 MB, and changed nothing else I could measure: start-up, opening a link in the running browser and memory were the same, so I expect about 10 to 20 seconds more install time on the tablet and a little less lag the first time you use a menu, the tab list or a settings screen (not measured on the tablet). The start-up file the updater sends got smaller (4 KB). I also tested the other half of your idea, whether the first start after an update is slower than later ones (so a warm-up run right after the update could help): it is not, so I did not build one. This release also carries the Swoop keyboard engine fix made after alpha66 (Kestrel's own keyboard behaves as before) and a new Diagnostics line, start oat, with the sizes of the compiled-code files: send it after the update to prove that the whole app was compiled (about 12 MB for base.odex; about 3 MB means the old small profile). IMPORTANT: like alpha65, this only works for updates that come FROM alpha65 or alpha66, because they carry the updater that sends the start-up file: so the update from alpha66 to alpha67 is the first one compiled whole. NOT proven on the tablet: Android Go may ignore the file (then the nightly compile when the tablet is idle and charging does the same job later, now for the whole app). If an update ever fails to install, tell me the exact words Android shows."
NEVER ship kestrel-g-v0.3.0-alpha67 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  fd0ea0b5 ckpt 2640: Diagnostics line 'start oat' added (BrowserActivity.oatSizes: sizes of oat/<
  63fdb931 ckpt 2639: LAB7 CONFIRMATION lab run DISPATCHED ONCE: android.yml run 44137227924 (run_
  ebd3854a ckpt 2638: LAB7 first lab run 44126148207 READ (research/LAB7_RESULTS.md next): (1) the
  5b6a24c8 ckpt 2637: LAB7 CORRECTED lab run DISPATCHED ONCE: android.yml run 44126148207 (run_num
  e40a5b70 ckpt 2636: LAB7 lab run 44125632537 CANCELLED ON PURPOSE and replaced by a corrected on
  3206d5dd ckpt 2635: LAB7 lab run DISPATCHED ONCE (android.yml app=lab lab_suite=aot lab_profile=
  eaa4e46d ckpt 2634: LAB7a+b lab code written and dry-run against a fake adb (aotlab.py breadth_c
  a2313194 ckpt 2634: gated kestrel-g-v0.3.0-alpha66 (gecko code 167) and pushed it: handoff self-
  60f8deca ckpt 2633: alpha65 SHIPPED and RECORDED (run 44120758574 all 7 jobs green, Release kest
  f7d26151 ckpt 2632: alpha65 run is android.yml run 44120758574 (run_number 216, started 16:00:17
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
