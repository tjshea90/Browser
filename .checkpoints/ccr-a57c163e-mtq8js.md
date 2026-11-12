# CHECKPOINT 628 — read me first, then TASKS.md

**Written:** 2026-11-12T00:38:36Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `c436663` (this checkpoint is the commit after it)

## Just done
owner request 10 written into TASKS.md as X11 (everything still on the list) and X12 (audit app + engine; Proton Mail slow to load and typing lag; tune/modify the Firefox engine for this hardware). alpha14 is shipped and recorded; the pop-up guard code (X6) is in the tree, compiled, not yet run on the emulator.

## Do this next
Order: (1) X6 finish: background.test.js tests for the redirect guard + tap + guard answer; emulator test page popups.html (check Diagnostics Blocker line for 'page guard on', sender.tab presence, MAIN world registration); (2) X8/X10 floating Download chip + long-press entry; (3) X7 DOM scan; (4) X9 TS->MP4 incl. discontinuities; ship alpha15; (5) X2 daily update dialog (app+engine+lists); (6) X5 assessment + CI list snapshot; (7) X3 download manager research+upgrades; (8) X12 audit -> research/AUDIT-2026-11.md + fixes + engine tuning; ship alpha16/17.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  6b28616 ckpt 625: alpha14 SHIPPED and recorded (run 43127174913 green): https://github.com/tjshe
  d1af18c ckpt 615: alpha14 RE-DISPATCHED: run 43127174913 (android.yml, head 0cebcca, 00:25:31Z, 
  0cebcca ckpt 614: gated kestrel-g-v0.3.0-alpha14 (gecko code 113) and pushed it: handoff self-te
  fec7b3d ckpt 609: alpha14 DISPATCHED: run 43126355049 (android.yml, head 3ed1b75, started 00:18:
  3ed1b75 ckpt 608: gated kestrel-g-v0.3.0-alpha14 (gecko code 113) and pushed it: handoff self-te
  b646b31 ckpt 606: X1 DONE in code and tests: updater root cause found (GitHub lists releases by 
  6400e12 ckpt 600: owner request 9 written into TASKS.md as X1-X10 in the owner's words (INBOX ha
  f1f1d48 ckpt 597: alpha13 SHIPPED and recorded (run 43123971432 green, BUILDLOG written): https:
  e860a70 ckpt 595: alpha13 DISPATCHED on GitHub Actions: run 43123971432 (android.yml, head 19e3a
  19e3a6d ckpt 594: gated kestrel-g-v0.2.0-alpha13 (gecko code 112) and pushed it: handoff self-te
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
