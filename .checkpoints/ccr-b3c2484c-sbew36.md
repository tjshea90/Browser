# CHECKPOINT 2504 — read me first, then TASKS.md

**Written:** 2026-11-16T08:36:47Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `559be944` (this checkpoint is the commit after it)

## Just done
lab vp run dispatched: 43832408756 (lab job 133180783158, head 6b32808a = ckpt 2503, started 08:35Z); all unit suites green (core, app, gecko), web suite 566/566; ANS54 design doc has the lab findings

## Do this next
read lab run 43832408756 when job 133180783158 is completed (mcp__github__actions_get get_workflow_job, small): artifact emulator-lab-text -> python3 -I tools/emu/fetch_artifact.py <url> <empty dir>; every section green? (kp incl. 'follow the tablet by default stays upright', np, cp, hls, ap, ho incl. file again + from N s). Fix findings; then bump geckoapp/build.gradle.kts to versionCode 159 / 0.3.0-alpha58 + bash tools/sync-ext-version.sh, docs/handoff/ALPHA58_RELEASE_NOTE.txt (ASCII, no double quotes or backslashes), bash ship.sh gecko with the note, ONE dispatch (app=gecko full_build=true), tag + jobs + release, bash tools/record-release.sh <tag> identical note, tick ANS54a-f in TASKS, answer owner message 54 with the Release link on its own line

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6b32808a ckpt 2503: lab run 43831112200 (ho run 2) read: file now hands over on the real engine 
  57239dd2 ckpt 2502: lab run 43831112200 (suite ho, head bbfc9c3f = ckpt 2501) dispatched; gradle
  bbfc9c3f ckpt 2501: ANS54e: lab run 43826954106 (suite ho) read: overlay/hls/unknown/never ok, f
  00b6ff6b ckpt 2500: lab run 43826954106 dispatched: suite ho (head 97294fcb): web videos play in
  97294fcb ckpt 2499: ANS54e: full web suite 562/562, gradle all green (before the late-hand-over 
  368a9961 ckpt 2498: ANS54e coded, compiles: core WebHandOver + test (10 ok), Prefs web_in_kestre
  a90fb92d ckpt 2497: ANS54 design written (research/ANS54_DESIGN.md) and sub-rows ANS54e.1-7 in T
  519b8caa ckpt 2496: owner message 54 written into TASKS.md as ANS54a-f in his words (a: page pla
  201e537f ckpt 2495: alpha57 SHIPPED and recorded: Release kestrel-g-v0.3.0-alpha57 (code 158, AP
  eb5df864 ckpt 2494: alpha57 RELEASE DISPATCHED (ONE time): android.yml run 43820755532 (app=geck
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
