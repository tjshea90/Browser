# CHECKPOINT 2724 — read me first, then TASKS.md

**Written:** 2026-11-20T04:27:18Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-d6f32d19-c3cwzl` · **builds on:** `57b85e1a` (this checkpoint is the commit after it)

## Just done
RVW1b-7: F5 shadow-root video watcher + F11 source labels (JS tests green: finder2/content/videobar 83); F11 Kotlin edits (ShieldsBridge label, MediaList merge + best-first verify, VideoFlow 4) not yet compiled/tested; first full Gradle run still going

## Do this next
Next: after scratchpad full.log EXIT, rerun ./gradlew -q --continue :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest (DownloadController import fixed, F11 Kotlin added) and read failures from XML; then cd web-tests && npm test; bash tools/test_handoff.sh; then answer the owner (RVW1h) and decide the release (alpha72 waits for the owner's go)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/REVIEW_2026-11-20_FINDER.md

## Last ten checkpoints
```
  202b8f0c ckpt 2723: RVW1b-6: F8 JSON-answer reader (Vimeo/JW/Wistia style), P2, P6, P7, P11 done
  6d527b43 ckpt 2722: RVW1b-5 CODED: naming N1-N5 N8, security S1 S3 S6 S7 S8 S9 S10; full core+ap
  e4c77795 ckpt 2721: RVW1b-4: P1 names.js gate, P10 map caps, N6 disposition cut done with web te
  e139e52d ckpt 2720: RVW1b-3 DONE: download engine V1 V2 V6 V7 V10 V12 V13 + S4 S5 fixed with tes
  25206010 ckpt 2719: RVW1b-2 DONE: F7 F10 F14 F15 F18 F20 F22 fixed with tests (core MediaSniff/M
  5364b783 ckpt 2718: RVW1b-1 DONE: finder F1 F2 F3 F4 F9 F12 fixed with tests (finder2.test.js 6/
  aff75506 ckpt 2717: RVW1b: F1 (vid reports name the tab page + per-frame element numbers, 2 web 
  edc76eba ckpt 2716: RVW1a DONE: all four review lanes saved (research/REVIEW_2026-11-20{,_FINDER
  64f64826 ckpt 2715: RVW1a finder lane saved to research/REVIEW_2026-11-20_FINDER.md (F1-F24: F1 
  d9a44bf6 ckpt 2714: RVW1a speed lane saved to research/REVIEW_2026-11-20_SPEED.md (P1-P11); secu
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
