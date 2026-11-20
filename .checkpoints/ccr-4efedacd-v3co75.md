# CHECKPOINT 2747 — read me first, then TASKS.md

**Written:** 2026-11-20T07:44:38Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-4efedacd-v3co75` · **builds on:** `4c5ff5b3` (this checkpoint is the commit after it)

## Just done
REL74b: alpha74 DISPATCHED ONCE (android.yml app=gecko full_build=true ref ccr-4efedacd-v3co75, note = docs/handoff/ALPHA74_RELEASE_NOTE.txt, gecko code 175). DO NOT DISPATCH AGAIN. Gates passed locally (handoff self-test, versionCode 175 > 174, :core:test :app:testDebugUnitTest compile, push)

## Do this next
Find the run id (mcp__github__actions_list list_workflow_runs android.yml branch ccr-4efedacd-v3co75), wait until all jobs succeed AND the tag kestrel-g-v0.3.0-alpha74 exists (git ls-remote --tags origin) and mcp__github__get_release_by_tag shows the APK; ONLY THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha74 with the IDENTICAL note ($(cat docs/handoff/ALPHA74_RELEASE_NOTE.txt)); then answer the owner with the Release link on its own line, tick RVW3c/RVW3d, delete heartbeat trig_012BwCyPqhi5Dfz8amdBigFo. If the run FAILED: read the failed job (mcp__github__get_job_logs), fix, and ship alpha75 (never re-ship the same tag)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  4c5ff5b3 ckpt 2746: gated kestrel-g-v0.3.0-alpha74 (gecko code 175) and pushed it: handoff self-
  6ab74f37 ckpt 2745: RVW3 fixes CODED + compile green (:geckoapp:compileDebugKotlin EXIT 0): secu
  7496a10a ckpt 2744: RVW3: security lane back and VERIFIED by me against the code (SC1 SC2 SC4 SC
  fe77b529 ckpt 2743: RVW3a launched: three read-only review lanes running as background agents (p
  e4b18cd0 ckpt 2742: RVW3 written into TASKS.md in the owner's words (review + improve speed, vid
  23e83be6 ckpt 2741: Owner decision recorded: keep Firefox's background traffic (P4) as it is; P3
  94c87088 ckpt 2740: alpha73 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha73 (gecko code 174, run
  7f06c310 ckpt 2739: REL73b: alpha73 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  5bf338a5 ckpt 2738: gated kestrel-g-v0.3.0-alpha73 (gecko code 174) and pushed it: handoff self-
  f2a9d669 ckpt 2737: REL73a done: versionCode 174 / 0.3.0-alpha73, ext synced, ALPHA73 release no
```
