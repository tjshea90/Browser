# CHECKPOINT 2761 — read me first, then TASKS.md

**Written:** 2026-11-20T20:14:37Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `aae44c8f` (this checkpoint is the commit after it)

## Just done
RVW5 gates all green: :core:test 1916 (OwnerRulesTest guard kept: VideoLevels keeps the owner-rule text lp.screenBrightness = f.coerceIn(0.01f, 1f)), :app:testDebugUnitTest 450, :geckoapp compile EXIT 0 (the module has no unit tests), :app:lintDebug 0 errors, web npm test 649/649. TASKS RVW5a-e ticked with evidence. Bumped geckoapp versionCode 177 / 0.3.0-alpha76, extension version synced, docs/handoff/ALPHA76_RELEASE_NOTE.txt written (ASCII). NOT shipped yet.

## Do this next
REL76: bash ship.sh gecko "$(cat docs/handoff/ALPHA76_RELEASE_NOTE.txt)" (local gates), then ONE dispatch: mcp__github__actions_run_trigger run_workflow owner tjshea90 repo Browser workflow android.yml ref claude/sweet-gates-gr3bj5 inputs app=gecko full_build=true note=<the note>; wait for tag kestrel-g-v0.3.0-alpha76 (git ls-remote --tags) + all jobs green + Release APK, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha76 with the IDENTICAL note; answer the owner with the Release link on its own line (RVW5g). Never dispatch twice.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/build.gradle.kts
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  8665429e ckpt 2760: RVW5: coded + tested in core (media tests 524 of 524 green): NA1 title clean
  58b7c173 ckpt 2759: RVW5 written into TASKS.md in the owner's words (new levers: speed, playback
  80ae116d ckpt 2758: RVW4 DONE AND SHIPPED: alpha75 RELEASED AND RECORDED = kestrel-g-v0.3.0-alph
  a0ad95ec ckpt 2757: REL75b: alpha75 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  eebefd5f ckpt 2756: gated kestrel-g-v0.3.0-alpha75 (gecko code 176) and pushed it: handoff self-
  9e99464e ckpt 2755: RVW4: whole JVM suite GREEN (:core:test 1907 tests 0 failures 12 skipped as 
  b1ec2fd4 ckpt 2754: NL1 PROVEN on the real engine: lab run 44787922316 (daily, 62 scenes, 12 min
  ae4e6a11 ckpt 2753: RVW4: NL2 done: realfinder.js (real pages through the real background.js + c
  d637614b ckpt 2752: NL1: licence row (docs/FILTER-LISTS.md) + HANDOFF engine step written; emula
  85af50a4 ckpt 2751: RVW4-NL1 code done and compiling: Gradle prepareWebcompat (AAR from Mozilla 
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
