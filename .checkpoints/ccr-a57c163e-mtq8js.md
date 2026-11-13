# CHECKPOINT 1348 — read me first, then TASKS.md

**Written:** 2026-11-13T00:39:50Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `e95b292` (this checkpoint is the commit after it)

## Just done
alpha25 (code 124) gated + pushed (ckpt 1347) and DISPATCHED: android.yml run 43338760626 (run #75, started 00:39Z, ~6-7 min). Gates: 825 JVM tests 0 failed, 99 web tests, Gecko compile green. Emulator proofs done: EX-1, EX-2, EX-3, EX-4, EX-10, EX-11. NOT yet recorded in BUILDLOG

## Do this next
WAIT for run 43338760626: background loop until git ls-remote origin refs/tags/kestrel-g-v0.3.0-alpha25 shows the tag, then mcp__github__actions_get get_workflow_run 43338760626 (conclusion success; NEVER poll get_job_logs, 404 until the job ends); then mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha25 (APK asset present); then bash tools/record-release.sh kestrel-g-v0.3.0-alpha25 "$(cat scratchpad/note25.txt)" (the note text is also in the body of ckpt 1347's commit message); then reply to the owner (message 16, plain words, Release link on its own line, honest: emulator/unit/web-tested only, not on the tablet; list the open owner items). Do NOT dispatch alpha25 twice. Heartbeat trigger trig_01LLoVKsjuTUaDn61UGP1pBN exists (every 2 h).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e95b292 ckpt 1347: gated kestrel-g-v0.3.0-alpha25 (gecko code 124) and pushed it: handoff self-t
  bede630 ckpt 1346: EX-10 PROVEN on the emulator (Tap to load box replaced the third-party frame;
  d7c8bb0 ckpt 1341: EX-1 PROVEN on the emulator (WifiService acquireWifiLock lockMode=3 at the st
  ae14f0d ckpt 1334: :style() restyling rules in core (697 of 770 real rules accepted; CosmeticSty
  217004a ckpt 1327: EX-2 PROVEN on the emulator (Gecko's own nsHttp log + host TLS listener: old 
  59c0013 ckpt 1322: Data saver choice re-applies the engine prefs (the look-up-links lab switch f
  49dea49 ckpt 1320: Docs: docs/ARCHITECTURE.md alpha25 section, research/EX_RESULTS.md (what was 
  c560ca9 ckpt 1316: EX ladder: EX-10 embeds-wait-for-a-tap coded (Shields flag EMBEDS=128 + Prefs
  352efcb ckpt 1306: More of the EX ladder coded (compiles; unit tests green: NetProfile 12, Backu
  edaf04d ckpt 1288: EX-2/3/4/8/9/11 code done (compile green, unit tests for EX-9; emulator proof
```
