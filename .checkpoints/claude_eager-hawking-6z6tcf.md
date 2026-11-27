# CHECKPOINT 3461 — read me first, then TASKS.md

**Written:** 2026-11-27T06:47:26Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/eager-hawking-6z6tcf` · **builds on:** `240256a0` (this checkpoint is the commit after it)

## Just done
alpha111 RE-DISPATCHED ONCE after fixes: run 45954070324 (run number 313, head 240256a0). The first alpha111 run 45952674368 was CANCELLED before publish (no tag): two fixes went in first: a private tab that appears while a recording runs throws the recording away before anything is written or sent; KeepReadyService and keepReadyOnLeave can no longer throw out of the service start / onStop (promoteSafely + try/catch). alpha110 is RELEASED AND RECORDED. OwnerRulesTest + core suite + app 515 + geckoapp compile green

## Do this next
WAIT for the tag kestrel-g-v0.3.0-alpha111 (watcher: scratchpad tagwait111b.log; build-gecko needs test-jvm and test-js so a published Release with the APK proves green). Then mcp__github__get_release_by_tag tjshea90 Browser kestrel-g-v0.3.0-alpha111, bash tools/record-release.sh kestrel-g-v0.3.0-alpha111 "<the IDENTICAL note, in this commit's ship ckpt message>", tick ND1d/ND1e/ND1f in TASKS.md, then answer the owner in plain words with both Release links on their own lines. Optional tidy while waiting: profile_digest.py should also accept samples.timeDeltas (processed format) - edit only AFTER the run has checked out (it has) and rerun python3 -B -I tools/test_profile_digest.py

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  240256a0 ckpt 3460: gated kestrel-g-v0.3.0-alpha111 (gecko code 212) and pushed it: handoff self
  3382c4f8 ckpt 3457: alpha111 DISPATCHED ONCE: run 45952674368 (run number 312, head 84d5657e, ap
  84d5657e ckpt 3456: gated kestrel-g-v0.3.0-alpha111 (gecko code 212) and pushed it: handoff self
  561bfd0f ckpt 3454: pre-ship: alpha111: two new instruments, so the next review can see what cla
  1f3ac22f ckpt 3448: ND1d/ND1e CODED (not shipped yet): core ProfileRule (+5 tests), geckoapp Pag
  8433cdc4 ckpt 3440: alpha110 DISPATCHED ONCE: run 45949557236 (run number 311, head 91326acd, re
  91326acd ckpt 3439: gated kestrel-g-v0.3.0-alpha110 (gecko code 211) and pushed it: handoff self
  8d2e39d0 ckpt 3432: KR1 keep-the-page-ready DONE in code: core KeepReady (4 tests) + geckoapp Ke
  2bdf5109 ckpt 3420: ND1: Settings lazy sections (SettingsView.section(title){rows}, Chevron, OPE
  2f0ab8fc ckpt 3407: ND1 (owner: 'Use the newest diagnostics and improve the browser as much as p
```
