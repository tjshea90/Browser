# CHECKPOINT 2727 — read me first, then TASKS.md

**Written:** 2026-11-20T04:59:11Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-d6f32d19-c3cwzl` · **builds on:** `10fe1a32` (this checkpoint is the commit after it)

## Just done
REL72a done: geckoapp versionCode 173 / 0.3.0-alpha72, ext version synced, docs/handoff/ALPHA72_RELEASE_NOTE.txt written

## Do this next
bash ship.sh gecko with the note from docs/handoff/ALPHA72_RELEASE_NOTE.txt (it runs the local gates; run in background, long), then ONE dispatch app=gecko full_build=true on ref ccr-d6f32d19-c3cwzl, verify tag via git ls-remote --tags, record-release with the identical note

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M geckoapp/build.gradle.kts
     M geckoapp/src/main/assets/kestrel-shields/manifest.json
    ?? docs/handoff/ALPHA72_RELEASE_NOTE.txt

## Last ten checkpoints
```
  10fe1a32 ckpt 2726: REL72 written into TASKS (owner said: Release the next version and tell me w
  bbad6ad4 ckpt 2725: RVW1 whole suite GREEN: :core:test :app:testDebugUnitTest :geckoapp:testDebu
  49214eac ckpt 2724: RVW1b-7: F5 shadow-root video watcher + F11 source labels (JS tests green: f
  202b8f0c ckpt 2723: RVW1b-6: F8 JSON-answer reader (Vimeo/JW/Wistia style), P2, P6, P7, P11 done
  6d527b43 ckpt 2722: RVW1b-5 CODED: naming N1-N5 N8, security S1 S3 S6 S7 S8 S9 S10; full core+ap
  e4c77795 ckpt 2721: RVW1b-4: P1 names.js gate, P10 map caps, N6 disposition cut done with web te
  e139e52d ckpt 2720: RVW1b-3 DONE: download engine V1 V2 V6 V7 V10 V12 V13 + S4 S5 fixed with tes
  25206010 ckpt 2719: RVW1b-2 DONE: F7 F10 F14 F15 F18 F20 F22 fixed with tests (core MediaSniff/M
  5364b783 ckpt 2718: RVW1b-1 DONE: finder F1 F2 F3 F4 F9 F12 fixed with tests (finder2.test.js 6/
  aff75506 ckpt 2717: RVW1b: F1 (vid reports name the tab page + per-frame element numbers, 2 web 
```
