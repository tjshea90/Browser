# CHECKPOINT 1877 — read me first, then TASKS.md

**Written:** 2026-11-14T00:43:36Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `f11808c` (this checkpoint is the commit after it)

## Just done
alpha33 (gecko code 134) DISPATCHED: android.yml run 43536066565 (run #84, started 2026-11-14T00:43:13Z) on ccr-a57c163e-mtq8js, full_build=true. Local gates all green first: core 819 tests, app 223, lintDebug, geckoapp compile, web-tests 150/150. AO1 ticked.

## Do this next
WAIT for run 43536066565 (about 7-9 min like alpha32): git ls-remote --tags origin kestrel-g-v0.3.0-alpha33 shows the tag when the publish step runs; then mcp__github__actions_get get_workflow_run 43536066565 must say conclusion success; then mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha33 shows the APK; THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha33 with the note in scratchpad/alpha33_note.txt (use: bash tools/record-release.sh kestrel-g-v0.3.0-alpha33 "$(cat that file)"). NEVER dispatch alpha33 twice. After that: answer the owner in plain words (everything in messages 29-33), re-list open items, ckpt, delete heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN only after 24 h of owner silence (stand down on wakes).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f11808c ckpt 1876: gated kestrel-g-v0.3.0-alpha33 (gecko code 134) and pushed it: handoff self-t
  cc028b28 ckpt 1875: AO1 ticked in TASKS (emulator Diagnostics screen shows the CPU by process, S
  9689afe ckpt 1860: AQ1, AR1 and AR2 ticked in TASKS with evidence: Home restore proven on the em
  2874e84 ckpt 1844: AQ1: found on the emulator that restore's show() cleared the Home flag (the p
  ebe2fa5 ckpt 1829: Owner message 32 written into TASKS (AR1 blocked-page icon bottom middle 1 s,
  39cf669 ckpt 1819: Owner message 31 written into TASKS (AQ1 Home restored as last page, AQ2 cont
  fa60b0e ckpt 1812: AO1 code + research written: core/diag/CpuUse.kt (ProcStat/CpuSeries, CpuUseT
  33ceee7 ckpt 1804: AO2 DONE and ticked: SizeEstimate + VideoFlow calibration + engine running es
  da2a562 ckpt 1790: AO4 DONE and ticked: emulator proof with planted files (engine start pruned t
  6b3800f ckpt 1788: AO4 code written and green: core ApkFiles (+5 tests), app diag/StorageTidy (+
```
