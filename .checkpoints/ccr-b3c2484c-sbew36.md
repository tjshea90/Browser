# CHECKPOINT 2440 — read me first, then TASKS.md

**Written:** 2026-11-15T22:14:33Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `dae57a1f` (this checkpoint is the commit after it)

## Just done
lab run 43766649065 (fsy with the slow_turn scene) read: ROT2 works on the real engine (turn-settled text 'the page had the new size after ~400 ms', 1167/721 ms on a flap) BUT the lab found a real bug: a page's viewport report was credited to the FIRST tab with the same address (duplicate lab tabs, the hide-mode and slow-turn pages) so those turns used the old rule (no confirmation text); fixed with tabOfReport (full-screen tab, then front tab, then first match) + guard test; lab: busy-loop count verified with ps, report says how many hogs ran

## Do this next
cancel the queued/started compat lab run (it blocks the lab group for 2 h), dispatch lab fsy on this commit, read the slow-turn table (needs 'the page had the new size' text for wait-on turns, hogs>0, wait-off vs wait-on difference), then ship alpha53 (bump 154, note in scratchpad note53.one), re-dispatch compat afterwards

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  9cadf845 ckpt 2439: ROT2 complete in the tree and green: all Kotlin suites EXIT 0, npm test 453 
  359fe757 ckpt 2438: ROT2 coded (owner message 48): TurnSettle PAGE_CAP/NUDGE/HINT + settled(expe
  bd7e42fd ckpt 2437: owner message 48 (two screenshots of a turn: the page keeps the previous ori
  ac2107ef ckpt 2436: alpha52 RELEASED and recorded (run 43763812046 green, APK in the Release, BU
  1e089300 ckpt 2435: alpha52 shipped to GitHub (gates green, pushed): release run 43763812046 (an
  49db2736 ckpt 2434: gated kestrel-g-v0.3.0-alpha52 (gecko code 153) and pushed it: handoff self-
  ab5859df ckpt 2433: pre-ship: alpha52: full-screen video, third step. Found with the emulator la
  80198e75 ckpt 2432: FB4 coded + all green: turn.js vpt report, background relay, ShieldsBridge v
  9780732e ckpt 2431: lab run 43760546128 (fsy, first clean run, alpha51 code) read: Keep mode put
  e9464a7e ckpt 2430: alpha51 gated and queued (FB1 bars kept by default + Settings row, FB2/FB3 t
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
