# CHECKPOINT 3457 — read me first, then TASKS.md

**Written:** 2026-11-27T06:34:29Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/eager-hawking-6z6tcf` · **builds on:** `84d5657e` (this checkpoint is the commit after it)

## Just done
alpha111 DISPATCHED ONCE: run 45952674368 (run number 312, head 84d5657e, app=gecko full_build=true). alpha110 is RELEASED AND RECORDED (Release kestrel-g-v0.3.0-alpha110, run 45949557236 green, BUILDLOG line pushed, ND1c ticked). Gates for 111: handoff self-test 98, :core:test, :app:testDebugUnitTest 515, :geckoapp:compileDebugKotlin

## Do this next
WAIT for run 45952674368 (never dispatch alpha111 again; tag watcher log: scratchpad tagwait111.log). When the tag exists: mcp__github__actions_list list_workflow_jobs resource_id 45952674368 (all of check, test-jvm, test-js, build-gecko must be success), mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha111 shows the APK, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha111 "<the IDENTICAL note: it is in the ship ckpt commit message / scratchpad note111.txt>", tick ND1d/ND1e/ND1f in TASKS.md, answer the owner in plain words (what the diagnostics showed, alpha110 + alpha111 links on their own lines, what was NOT run on the tablet, how to record a profile, open items)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  84d5657e ckpt 3456: gated kestrel-g-v0.3.0-alpha111 (gecko code 212) and pushed it: handoff self
  561bfd0f ckpt 3454: pre-ship: alpha111: two new instruments, so the next review can see what cla
  1f3ac22f ckpt 3448: ND1d/ND1e CODED (not shipped yet): core ProfileRule (+5 tests), geckoapp Pag
  8433cdc4 ckpt 3440: alpha110 DISPATCHED ONCE: run 45949557236 (run number 311, head 91326acd, re
  91326acd ckpt 3439: gated kestrel-g-v0.3.0-alpha110 (gecko code 211) and pushed it: handoff self
  8d2e39d0 ckpt 3432: KR1 keep-the-page-ready DONE in code: core KeepReady (4 tests) + geckoapp Ke
  2bdf5109 ckpt 3420: ND1: Settings lazy sections (SettingsView.section(title){rows}, Chevron, OPE
  2f0ab8fc ckpt 3407: ND1 (owner: 'Use the newest diagnostics and improve the browser as much as p
  7c753c88 ckpt 3381: VPS2a-c DONE: research/VPS_PLAN.md written (P40 facts + ToS limits, evidence
  af9b1f64 ckpt 3378: VPS2 (owner: NoBull P.40, in depth research for every repo + Claude + LinkMe
```
