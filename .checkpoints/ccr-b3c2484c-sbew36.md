# CHECKPOINT 2426 — read me first, then TASKS.md

**Written:** 2026-11-15T20:47:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `16fea396` (this checkpoint is the commit after it)

## Just done
owner message 47 written into TASKS.md (alpha50 changed nothing on the tablet: controls still under the navigation bar, no controls in full screen, the video was not paused when turning, wrong size for seconds): FB1 keep the system bars and the page between them by default (no immersive request) + Settings row, FB2 page-level pause for every video during a turn, FB3 a resize nudge after the turn settled

## Do this next
NEXT: code FB1 (Prefs.fullscreenBars, SettingsView row, BrowserActivity.applyImmersive only for Hide, lab hook 'bars'), FB2+FB3 (kestrel-shields/turn.js + manifest + background vbk words + onVideoBar), tests (web-tests/turn.test.js, OwnerRulesTest guard), lab suite fsy modes keep/hide; the fsy re-run dispatched 20:44Z is still running (read it when it ends: pictures yahoofs-* show the owner's stale-size symptom); then ship alpha51 and answer the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  d941af5c ckpt 2425: alpha50 shipped and recorded (Release kestrel-g-v0.3.0-alpha50, run 43758859
  4415fbaf ckpt 2424: lab run 43753283814 (fsy) read: it says NOTHING about the full-screen code: 
  6689210f ckpt 2423: gated kestrel-g-v0.3.0-alpha50 (gecko code 151) and pushed it: handoff self-
  ca8bc3fb ckpt 2422: pre-ship: alpha50: fixes from an independent check of alpha49 (everything el
  342c62e0 ckpt 2421: compat run 1 (43747605584) read: only 4 sites measured, the emulated phone's
  b0aa0482 ckpt 2420: lab driver: Yahoo Full-button tap x 556 (inside the button for film widths 5
  a88bd7cb ckpt 2419: alpha49 review (a1b60178ab815123f) fixed: RA1 media state forgotten with the
  2de3f20b ckpt 2418: alpha49 shipped and recorded: Release kestrel-g-v0.3.0-alpha49 (run 43751318
  7e7d3319 ckpt 2417: lab scene states fixed (the app turns a wide video sideways by itself at ful
  0a81c2b8 ckpt 2416: gated kestrel-g-v0.3.0-alpha49 (gecko code 150) and pushed it: handoff self-
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
