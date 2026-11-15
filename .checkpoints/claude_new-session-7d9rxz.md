# CHECKPOINT 2359 — read me first, then TASKS.md

**Written:** 2026-11-15T00:13:28Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `cca67983` (this checkpoint is the commit after it)

## Just done
alpha43 gated, pushed and dispatched to GitHub (V5, D1, S1, B2, G5, G6); waiting for the run

## Do this next
HEARTBEAT trig_01KEDB7Qxd8zFeawjGcAkSQZ (every 2 h; get_trigger to read; delete_trigger when the RX3 job is shipped). alpha43 (gecko code 144: V5, D1, S1, B2, G5, G6) was GATED, PUSHED and the android.yml run DISPATCHED at about 00:14Z (app=gecko full_build=true ref claude/new-session-7d9rxz). WAIT: git ls-remote --tags origin kestrel-g-v0.3.0-alpha43 shows the tag; mcp__github__actions_list list_workflow_runs (owner tjshea90 repo Browser resource_id android.yml, workflow_runs_filter {event workflow_dispatch, branch claude/new-session-7d9rxz}) newest run conclusion success; mcp__github__get_release_by_tag shows the APK; a courier/timing flake -> mcp__github__actions_run_trigger rerun_failed_jobs run_id <id>, NEVER a new dispatch. ONLY THEN: bash tools/record-release.sh kestrel-g-v0.3.0-alpha43 "<the note in the ship command: starts 'alpha43: the glow stops pulsing while a video plays; two more tracker block lists...' - copy it from the git log of this checkpoint's parent commit or from BUILDLOG/ship output>" then answer the owner in plain words (Release link https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha43 on its own line; proven on the emulator: S1 glow follows the video's play and pause, G6 jump to a listed ad host stopped with no touch for hours, V5 early look (live and 12 s clip not counted, film 12 s advertising found), B2 measured with the real lists; proven only by JVM tests: D1 outage wait, G5 new-window wall, V5 engine pre-skip; NOT run on the tablet: everything; WARN that the first list update after install downloads about 3 MB and compiles 210k rules on the tablet, if lists stop updating switch Settings > Filter lists > Extra tracker lists OFF and tell me; CHANGED BEHAVIOUR: a window opened by a button to another site needs one tap on the blocked icon; re-list open items BK5 question, AX1g, tablet feedback alpha40-43). MEANWHILE (no build): G4 fake play layers (content.js or guard-main.js: on touchstart/pointerdown inside a player rectangle neutralize a fake layer with pointer-events:none; web test first in a NEW file web-tests/fakelayer.test.js), then V3, then Wave C. Check a research/*.md does not exist before writing it.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  cca67983 ckpt 2358: gated kestrel-g-v0.3.0-alpha43 (gecko code 144) and pushed it: handoff self-
  18b548ee ckpt 2357: pre-ship: alpha43: the glow stops pulsing while a video plays; two more trac
  b7d3f84a ckpt 2356: RX3-S1, B2, G5, G6 DONE and ticked (S1 + G6 emulator-proven, B2 measured wit
  822e687f ckpt 2355: RX3-V5 and RX3-D1 DONE (ticked): AdMarkers/AdRuns/early look and the outage 
  dec2df9d ckpt 2354: alpha42 shipped to GitHub (gated + pushed + android.yml dispatched): G7 + V1
  652f583c ckpt 2353: gated kestrel-g-v0.3.0-alpha42 (gecko code 143) and pushed it: handoff self-
  feda2b32 ckpt 2352: RX3-V1 DONE (ticked) and RX3-V2 coded + proven on the emulator (teaser-link 
  21cda298 ckpt 2351: RX3-G7 DONE and ticked (full gates green: JVM + web 331/331; emulator real-e
  265fcf6e ckpt 2350: RX3-G7 other-players ad skipper coded and tested (15 mock tests + manifest/r
  69fcfcf1 ckpt 2349: RX3-Y3 live YouTube check recorded (research/YOUTUBE_ADS.md section 5, tools
```
