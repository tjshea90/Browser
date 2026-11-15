# CHECKPOINT 2371 — read me first, then TASKS.md

**Written:** 2026-11-15T03:17:29Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `c1e9d89b` (this checkpoint is the commit after it)

## Just done
alpha46 (gecko code 147) gated (JVM: core, tools, app incl. MiniNavTest 95, TabGridPrivateBoxTest, LaunchAnim/LaunchLogo tests, OwnerRulesTest, geckoapp compile, lintRelease all green) and dispatched to GitHub (~03:18Z): owner message 41 = panel root cause (zoneAt) + long press above closes + large X, HM1 tab stays on Home, PT1 private box hidden, SP1 launch picture = app symbol + 380 ms glow. Emulator proofs in research/MINI_NAV.md MN1/MN2 and research/UI_OWNER41.md. NOT run: web tests (JS unchanged since alpha45), the tablet.

## Do this next
FINISH alpha46: wait for the run (mcp__github__actions_list list_workflow_runs android.yml, event workflow_dispatch, branch claude/new-session-7d9rxz; tag waiter writes scratchpad tagwait46.out) to be green and git ls-remote --tags origin kestrel-g-v0.3.0-alpha46 to show the tag; mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha46 must list the APK (a courier/timing flake: rerun_failed_jobs, NEVER a new dispatch); THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha46 "$NOTE" with this IDENTICAL note; then answer the owner in plain words with https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha46 on its own line: MN3 answer (no earlier message about the closing X is in INBOX.md; message 41 is the first), the panel root cause in plain words (the spot above the panel counted as the panel), long press above closes, large X, tab on Home stays Home, Private box hidden, launch picture + glow (and Home brand mark), honest limits (emulator only; slow tap unit-proven; the glow only seen in slow motion on a debug build), the YouTube screenshot again (RX4a tried: the sandbox got YouTube's bot-check page, so ask for the Desktop site check), open items BK5, AX1g, tablet feedback alpha40-46, one Diagnostics copy after Proton Mail. note46 text: $NOTE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  c1e9d89b ckpt 2370: gated kestrel-g-v0.3.0-alpha46 (gecko code 147) and pushed it: handoff self-
  95cc5c99 ckpt 2369: pre-ship: alpha46: the floating navigation panel can be closed again: a long
  42026781 ckpt 2368: owner message 41 CODED + unit-proven (emulator proof still to do, rows not t
  ea36cfba ckpt 2367: wrote the owner's message 41 into TASKS.md as boxes (MN1 panel will not clos
  5abf8de1 ckpt 2366: alpha45 (gecko code 146) RELEASED, RECORDED in BUILDLOG.md (run 43664857352 
  ebd2f1ef ckpt 2365: alpha45 gated (JVM + web 373/373), pushed and dispatched to GitHub (workflow
  d636a48a ckpt 2364: gated kestrel-g-v0.3.0-alpha45 (gecko code 146) and pushed it: handoff self-
  c8a968e7 ckpt 2363: RX3-V4 coded + unit-proven (not ticked yet): core MediaList.applyBlob/matchB
  1dea80c7 ckpt 2362: RX3-S2 DONE and ticked (counters only): background.js LST + lstTimed around 
  d90ec3e7 ckpt 2361: alpha44 gated, pushed and dispatched (G4 fake play layers, V3 played element
```
