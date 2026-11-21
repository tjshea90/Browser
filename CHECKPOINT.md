# CHECKPOINT 2827 — read me first, then TASKS.md

**Written:** 2026-11-21T18:12:01Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `b9f6b0b0` (this checkpoint is the commit after it)

## Just done
alpha85 gated (core 1991 incl. FLK1 flake passing alone, app 478, lint 0 errors, web-tests 659/659, test_handoff 83/83) and DISPATCHED ONCE at 18:11Z on 2026-11-21: android.yml app=gecko full_build=true ref claude/peaceful-darwin-4sbmdz. DO NOT DISPATCH AGAIN.

## Do this next
Find the run id (actions_list list_workflow_runs android.yml, branch claude/peaceful-darwin-4sbmdz, event workflow_dispatch), wait for tag kestrel-g-v0.3.0-alpha85 (git ls-remote --tags origin), then list_workflow_jobs all success + get_release_by_tag shows the APK, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha85 with the IDENTICAL note from docs/handoff/ALPHA85_RELEASE_NOTE.txt (cat | tr -d newline); tick LD4a-e in TASKS (LD4d = instrument shipped, no engine change without proof); answer the owner in plain words incl. what is NOT proven and the next steps by verdict; re-list open items (LD3f owner test, LD4g next steps, YTC1/YTC2, ATT1, FLK1, PB8)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b9f6b0b0 ckpt 2826: gated kestrel-g-v0.3.0-alpha85 (gecko code 186) and pushed it: handoff self-
  495ef4db ckpt 2825: pre-ship: alpha85: the live diagnostics now explain WHY a page is slow, whic
  4cf115af ckpt 2824: LD4 gate so far: core 1991 tests (1 failed = FLK1 PhoneBackendTest flake, pa
  a1863e08 ckpt 2823: LD4c done: diag_digest.py LOAD ANALYSIS (per site: loads, load/first paint/f
  0c525795 ckpt 2822: LD4b app side done and compiling: GeckoEngine.networkProfileCode, ShieldsBri
  ca1f0fe2 ckpt 2821: LD4b extension side done: pagetime.js (ES5, top frame, document_start, laten
  029be5ca ckpt 2820: LD4b core done: DiagScrub caps (event 480, snapshot lines 1600: the CPU/Bloc
  0b7be92b ckpt 2819: LD4a done: research/SLOW_LOADS.md written from the real uploads: claude.ai 1
  a6b43548 ckpt 2818: LD4 written into TASKS.md in the owner's words (richer live diagnostics + cl
  db4c9602 ckpt 2817: alpha84 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/
```
