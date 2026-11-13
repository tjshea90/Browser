# CHECKPOINT 1515 — read me first, then TASKS.md

**Written:** 2026-11-13T07:10:07Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `78c9506` (this checkpoint is the commit after it)

## Just done
alpha29 DISPATCHED: android.yml run 43383744581 (run_number 79) started 07:09:46Z for kestrel-g-v0.3.0-alpha29 (gecko code 128); gates were green locally

## Do this next
wait for tag kestrel-g-v0.3.0-alpha29 (git ls-remote origin refs/tags/kestrel-g-v0.3.0-alpha29) and run 43383744581 completed/success (mcp__github__actions_get get_workflow_run), then mcp__github__get_release_by_tag shows the APK + .sha256, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha29 with the note from scratchpad note29.txt, then answer the owner (storage first: 1.62 GB free vs about 3.1 GB needed; install the NEWEST Release = alpha29; re-list open items); never dispatch alpha29 twice

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  78c9506 ckpt 1514: gated kestrel-g-v0.3.0-alpha29 (gecko code 128) and pushed it: handoff self-t
  f2230eb ckpt 1511: AH1b DONE and emulator-proven with the real engine (heads-up before a refused
  11126ad ckpt 1495: AH1c done and emulator-proven end to end (Shields master switch off -> Firefo
  939a120 ckpt 1487: AH1d done and emulator-proven: the Video line stayed on 'decoders unknown' be
  7d3044a ckpt 1482: Owner message 21 (their Diagnostics copy from the tablet, alpha27) written in
  29bfbb4 ckpt 1478: handoff saved (.handoff/handoff.md + archive 2026-11-13T05-01-56Z; CLAUDE.md/
  2e2935d ckpt 1476: alpha28 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha28 (gecko code 127), run 
  ec67869 ckpt 1474: gated kestrel-g-v0.3.0-alpha28 (gecko code 127) and pushed it: handoff self-t
  a562cab ckpt 1473: alpha28 gates green: JVM (core + app + gecko compile), 123 web tests, pref na
  a3a8e82 ckpt 1470: AF1 cause found + fixed + proven in real Gecko on the emulator: Kestrel asked
```
