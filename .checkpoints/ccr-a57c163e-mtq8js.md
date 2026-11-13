# CHECKPOINT 1404 — read me first, then TASKS.md

**Written:** 2026-11-13T02:38:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `33c7277` (this checkpoint is the commit after it)

## Just done
alpha26 (code 125) gated + pushed (ckpt 1403) and DISPATCHED: android.yml run 43353549684 (run #76, 02:38Z, ~7 min). Gates: 857 JVM tests 0 failed, 114 web tests, Gecko compile green

## Do this next
WAIT for run 43353549684: background loop until git ls-remote origin refs/tags/kestrel-g-v0.3.0-alpha26 shows the tag, then the run conclusion (mcp__github__actions_list list_workflow_runs perPage 1 status completed; NEVER poll get_job_logs), then mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha26 (APK present), then bash tools/record-release.sh kestrel-g-v0.3.0-alpha26 "$(cat scratchpad/note26.txt)" (note also in ckpt 1403's commit message), then reply to owner message 17 (plain words, Release link on its own line, honest about what was only emulator/unit/web-tested, re-list open owner items). Do NOT dispatch alpha26 twice. While waiting: AD1b-5 Gradle dependency verification metadata (dry run), optional Wi-Fi sleep test (EX-7).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  33c7277 ckpt 1403: gated kestrel-g-v0.3.0-alpha26 (gecko code 125) and pushed it: handoff self-t
  bc3596c ckpt 1401: AD1b-3/4 + AD1a-3 DONE: Settings cards + launch screen + Downloads queue UI v
  af1938b ckpt 1388: AD1b-3 UI polish: Settings sections are chamfered cards with dividers (like t
  7cb96e0 ckpt 1384: AD1b-2 mp4 always DONE in core: Hevc.kt (hvc1/hvcC), AudioFrames.kt (AC-3/E-A
  7919d0f ckpt 1373: AD1b-1 download manager DONE in core + UI wiring: Start now / Move up / Move 
  7b80ff7 ckpt 1365: AD1a-1 DONE: link warm-up triggers (finger down + top three search results) i
  ac0f553 ckpt 1353: Owner message 17 written into TASKS as AD1 (AD1a review rows, AD1b earlier AA
  16fc889 ckpt 1350: alpha25 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha25 (gecko code 124), run 
  62b643d ckpt 1348: alpha25 (code 124) gated + pushed (ckpt 1347) and DISPATCHED: android.yml run
  e95b292 ckpt 1347: gated kestrel-g-v0.3.0-alpha25 (gecko code 124) and pushed it: handoff self-t
```
