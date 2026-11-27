# CHECKPOINT 3465 — read me first, then TASKS.md

**Written:** 2026-11-27T07:05:35Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/eager-hawking-6z6tcf` · **builds on:** `0e36b09e` (this checkpoint is the commit after it)

## Just done
alpha110 AND alpha111 RELEASED AND RECORDED (BUILDLOG lines 211 and 212). ND1 finished: ND1a-f ticked, ND1g (owner test of alpha111) open. Also in the tree after alpha111: tools/profile_digest.py learned timeDeltas + the CPU unit (test_profile_digest.py green)

## Do this next
NOTHING is mid-change. A new session: (1) read research/DIAG_2026-11-27.md; (2) when the owner has sent diagnostics from alpha111: python3 -B -I tools/diag_digest.py (look at: memory by process, tasks running at busy moments, KILLS with keep-ready held=true, SELF-CHECKS must be empty) and, if a profile was recorded, python3 -B -I tools/profile_digest.py (--shape first when a number looks wrong; the reader was written before a real file existed: fix it, the raw file stays in the private repo profiles/); (3) write what the page spends its time on into research/ and only THEN change settings or code for claude.ai; (4) the open items: ND1g owner test, PERF25a (which device ran Brave), OPT3e, PERF17c/d, PERF19 cheaper blocker, PERF23, PERF14e Back place missed 8 of 13, PERF9, VPS2d (owner has to pick); possible next data-backed work: the keyboard resize wait (gecko layout took 100-667 ms, median 134, 83 events: the engine view resize waits on the compositor), claude.ai NET_PARTIAL_TRANSFER (14 cut xhr streams; Fast Wi-Fi tunnel suspected)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  9a47bf75 ckpt 3461: alpha111 RE-DISPATCHED ONCE after fixes: run 45954070324 (run number 313, he
  240256a0 ckpt 3460: gated kestrel-g-v0.3.0-alpha111 (gecko code 212) and pushed it: handoff self
  3382c4f8 ckpt 3457: alpha111 DISPATCHED ONCE: run 45952674368 (run number 312, head 84d5657e, ap
  84d5657e ckpt 3456: gated kestrel-g-v0.3.0-alpha111 (gecko code 212) and pushed it: handoff self
  561bfd0f ckpt 3454: pre-ship: alpha111: two new instruments, so the next review can see what cla
  1f3ac22f ckpt 3448: ND1d/ND1e CODED (not shipped yet): core ProfileRule (+5 tests), geckoapp Pag
  8433cdc4 ckpt 3440: alpha110 DISPATCHED ONCE: run 45949557236 (run number 311, head 91326acd, re
  91326acd ckpt 3439: gated kestrel-g-v0.3.0-alpha110 (gecko code 211) and pushed it: handoff self
  8d2e39d0 ckpt 3432: KR1 keep-the-page-ready DONE in code: core KeepReady (4 tests) + geckoapp Ke
  2bdf5109 ckpt 3420: ND1: Settings lazy sections (SettingsView.section(title){rows}, Chevron, OPE
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
