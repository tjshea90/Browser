# CHECKPOINT 2438 — read me first, then TASKS.md

**Written:** 2026-11-15T21:50:46Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `d6516ea1` (this checkpoint is the commit after it)

## Just done
ROT2 coded (owner message 48): TurnSettle PAGE_CAP/NUDGE/HINT + settled(expectsPage, pageConfirmed), TurnSettleMachine pageReported/nudge/hint/lag stats, TurnSettler host + BrowserActivity (vpOk evidence per tab, cover FrameLayout with progress ring, onPageViewport feeds the turn), turn.js one report 1 s after the page is up (f=0) + debounce 200; tests: core media tests, OwnerRulesTest guard, turn.test.js 10/10; Kotlin compile green; full npm test + all Kotlin suites NOT yet run

## Do this next
1) run all Kotlin suites + npm test; 2) lab scene slow_turn in tools/emu/soak.py (CPU hogs on the emulator during turns, red strip + black-cover sampling at +0.6..6.5 s) and a verdict (never a stale layout visible: either the cover or the right layout); 3) fix lab verdict false positive for landscape (emulator puts the nav bar at the SIDE in landscape: red strip legitimately ends at y=599); 4) dispatch lab fsy, read, ship alpha53 (versionCode 154), answer ANS48; the second fsy run (Yahoo pictures) should already be done: read its artifact too

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  bd7e42fd ckpt 2437: owner message 48 (two screenshots of a turn: the page keeps the previous ori
  ac2107ef ckpt 2436: alpha52 RELEASED and recorded (run 43763812046 green, APK in the Release, BU
  1e089300 ckpt 2435: alpha52 shipped to GitHub (gates green, pushed): release run 43763812046 (an
  49db2736 ckpt 2434: gated kestrel-g-v0.3.0-alpha52 (gecko code 153) and pushed it: handoff self-
  ab5859df ckpt 2433: pre-ship: alpha52: full-screen video, third step. Found with the emulator la
  80198e75 ckpt 2432: FB4 coded + all green: turn.js vpt report, background relay, ShieldsBridge v
  9780732e ckpt 2431: lab run 43760546128 (fsy, first clean run, alpha51 code) read: Keep mode put
  e9464a7e ckpt 2430: alpha51 gated and queued (FB1 bars kept by default + Settings row, FB2/FB3 t
  38a628d2 ckpt 2429: gated kestrel-g-v0.3.0-alpha51 (gecko code 152) and pushed it: handoff self-
  7c27ad91 ckpt 2428: pre-ship: alpha51: full-screen video, second attempt after alpha50 changed n
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
