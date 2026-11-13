# CHECKPOINT 1536 — read me first, then TASKS.md

**Written:** 2026-11-13T09:15:42Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `ab7b158` (this checkpoint is the commit after it)

## Just done
alpha30 DISPATCHED: android.yml run 43399774749 (run_number 80) queued 09:15:23Z for kestrel-g-v0.3.0-alpha30 (gecko code 129); items AI1a/AI1b emulator-proven, AI1c analysed + regression test; gates green

## Do this next
wait for tag kestrel-g-v0.3.0-alpha30 (git ls-remote origin refs/tags/kestrel-g-v0.3.0-alpha30) and run 43399774749 success (mcp__github__actions_get get_workflow_run), get_release_by_tag shows the APK + sha256, bash tools/record-release.sh kestrel-g-v0.3.0-alpha30 with scratchpad note30.txt, then answer the owner: all three items in plain words, the AI1d question (which earlier prompts did you mean), ask the version in Diagnostics App: line and the exact words of the 'incompatible browser' site, re-list open items; then save the handoff WITH the unanswered-prompt list (TASKS AI1d) and stop; never dispatch alpha30 twice

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ab7b158 ckpt 1535: gated kestrel-g-v0.3.0-alpha30 (gecko code 129) and pushed it: handoff self-t
  ac7766e ckpt 1534: pre-ship: alpha30 (includes alpha29 and alpha28): your three requests. 1) Set
  613fb9e ckpt 1531: AI1a + AI1b EMULATOR-PROVEN (Settings in the private-window menu; long-press 
  53ae8c1 ckpt 1525: new context window (session restart): the owner re-sent items 1-3 of message 
  5930d95 ckpt 1519: handoff saved (.handoff/handoff.md; CLAUDE.md/AGENTS.md Session Learnings upd
  5c37df1 ckpt 1518: alpha29 SHIPPED and RECORDED (run 43383744581 green, BUILDLOG line written, R
  01ea47c ckpt 1515: alpha29 DISPATCHED: android.yml run 43383744581 (run_number 79) started 07:09
  78c9506 ckpt 1514: gated kestrel-g-v0.3.0-alpha29 (gecko code 128) and pushed it: handoff self-t
  f2230eb ckpt 1511: AH1b DONE and emulator-proven with the real engine (heads-up before a refused
  11126ad ckpt 1495: AH1c done and emulator-proven end to end (Shields master switch off -> Firefo
```
