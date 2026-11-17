# CHECKPOINT 2642 — read me first, then TASKS.md

**Written:** 2026-11-17T18:17:46Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `d8340975` (this checkpoint is the commit after it)

## Just done
alpha67 DISPATCHED ONCE (android.yml app=gecko full_build=true, ref ccr-e89b9dfe-pl3lk5, at the gated commit d8340975 / ckpt 2641, note = docs/handoff/ALPHA67_RELEASE_NOTE.txt). DO NOT DISPATCH ALPHA67 AGAIN. Whole-app compile = the catch-all profile rule; lab proof research/LAB7_RESULTS.md (run 44126148207 + confirmation run 44137227924: compile +1 s, no start/memory change, no first-launch penalty so no warm-up). The release workflow now also runs tools/check-profile-coverage.sh right after the release build.

## Do this next
1) find the run id (list_workflow_runs event workflow_dispatch branch ccr-e89b9dfe-pl3lk5 perPage 1) and poll git ls-remote --tags origin for kestrel-g-v0.3.0-alpha67 (background loop); 2) when the tag exists: list_workflow_jobs (all green incl. the new gate step 'Gate - the baseline profile makes Android compile the whole app at install'), get_release_by_tag (APK + .apk.sha256 + kestrel-g-0.3.0-alpha67.dm, the .dm should be about 4.3 KB); 3) bash tools/record-release.sh kestrel-g-v0.3.0-alpha67 with the IDENTICAL note (tr -d newline from docs/handoff/ALPHA67_RELEASE_NOTE.txt); 4) tick LAB7 in TASKS.md (LAB7a/b/c with evidence), HANDOFF.md alpha67 entry, ckpt; 5) answer the owner in plain words (what changed, numbers, what is NOT proven on the tablet, what to try: update from alpha66, then Diagnostics lines start oat and start art; open items: Neon pulse decision, alpha62 site, tablet test of alpha66 keyboard link)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d8340975 ckpt 2641: gated kestrel-g-v0.3.0-alpha67 (gecko code 168) and pushed it: handoff self-
  fd0ea0b5 ckpt 2640: Diagnostics line 'start oat' added (BrowserActivity.oatSizes: sizes of oat/<
  63fdb931 ckpt 2639: LAB7 CONFIRMATION lab run DISPATCHED ONCE: android.yml run 44137227924 (run_
  ebd3854a ckpt 2638: LAB7 first lab run 44126148207 READ (research/LAB7_RESULTS.md next): (1) the
  5b6a24c8 ckpt 2637: LAB7 CORRECTED lab run DISPATCHED ONCE: android.yml run 44126148207 (run_num
  e40a5b70 ckpt 2636: LAB7 lab run 44125632537 CANCELLED ON PURPOSE and replaced by a corrected on
  3206d5dd ckpt 2635: LAB7 lab run DISPATCHED ONCE (android.yml app=lab lab_suite=aot lab_profile=
  eaa4e46d ckpt 2634: LAB7a+b lab code written and dry-run against a fake adb (aotlab.py breadth_c
  a2313194 ckpt 2634: gated kestrel-g-v0.3.0-alpha66 (gecko code 167) and pushed it: handoff self-
  60f8deca ckpt 2633: alpha65 SHIPPED and RECORDED (run 44120758574 all 7 jobs green, Release kest
```
