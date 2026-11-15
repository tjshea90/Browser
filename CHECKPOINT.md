# CHECKPOINT 2361 — read me first, then TASKS.md

**Written:** 2026-11-15T00:55:13Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `02082e19` (this checkpoint is the commit after it)

## Just done
alpha44 gated, pushed and dispatched (G4 fake play layers, V3 played element and pre-roll rule, S3 observer budget); web 362/362 and JVM gates green; waiting for the run

## Do this next
HEARTBEAT trig_01KEDB7Qxd8zFeawjGcAkSQZ (every 2 h; get_trigger to read; delete_trigger when the RX3 job is shipped = after alpha44 is recorded and answered, unless more rows are queued). alpha43 is RELEASED + RECORDED + ANSWERED. alpha44 (gecko code 145: G4, V3, S3) was GATED, PUSHED and the android.yml run DISPATCHED at about 00:55Z (app=gecko full_build=true ref claude/new-session-7d9rxz). WAIT: git ls-remote --tags origin kestrel-g-v0.3.0-alpha44 shows the tag; mcp__github__actions_list list_workflow_runs (owner tjshea90 repo Browser resource_id android.yml, workflow_runs_filter {event workflow_dispatch, branch claude/new-session-7d9rxz}) newest run conclusion success; mcp__github__get_release_by_tag shows the APK; a courier/timing flake -> rerun_failed_jobs run_id <id>, NEVER a new dispatch. ONLY THEN: bash tools/record-release.sh kestrel-g-v0.3.0-alpha44 "<the note: starts 'alpha44: one tap now plays a video when an invisible advertising layer lies over it' - the full text is in the scratchpad file note44.txt of the dead session and in the ship output / android.yml run input 'note' of this run>" then answer the owner in plain words (Release link https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha44 on its own line; proven on the emulator: G4 one tap reaches the video through an invisible layer (lab /fake-layer: video-click and no layer-click), V3 pre-roll recognised (lab /preroll-main: counted 1 of 2); S3 test-browser only; NOT run on the tablet: everything; re-list open items BK5 question, AX1g, tablet feedback alpha40-44, plus: Diagnostics lines to look at). THEN the remaining RX3 rows are Wave C and each needs the owner's tablet data (BK3, BK4, M1, H1, S2 counters then diet, S4, S5, S6, V4, V6, V7, D2, D3, B3, Y4, Y5, BK5 after the owner answers): do the cheap instrumentation-first ones if the owner has not answered yet (S2 listener counters in background.js 'st' stats; Y4 refresh the lists when YouTube opens), otherwise STOP and delete the heartbeat; do not burn usage waiting.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  02082e19 ckpt 2360: gated kestrel-g-v0.3.0-alpha44 (gecko code 145) and pushed it: handoff self-
  072024e2 ckpt 2359: alpha43 gated, pushed and dispatched to GitHub (V5, D1, S1, B2, G5, G6); wai
  cca67983 ckpt 2358: gated kestrel-g-v0.3.0-alpha43 (gecko code 144) and pushed it: handoff self-
  18b548ee ckpt 2357: pre-ship: alpha43: the glow stops pulsing while a video plays; two more trac
  b7d3f84a ckpt 2356: RX3-S1, B2, G5, G6 DONE and ticked (S1 + G6 emulator-proven, B2 measured wit
  822e687f ckpt 2355: RX3-V5 and RX3-D1 DONE (ticked): AdMarkers/AdRuns/early look and the outage 
  dec2df9d ckpt 2354: alpha42 shipped to GitHub (gated + pushed + android.yml dispatched): G7 + V1
  652f583c ckpt 2353: gated kestrel-g-v0.3.0-alpha42 (gecko code 143) and pushed it: handoff self-
  feda2b32 ckpt 2352: RX3-V1 DONE (ticked) and RX3-V2 coded + proven on the emulator (teaser-link 
  21cda298 ckpt 2351: RX3-G7 DONE and ticked (full gates green: JVM + web 331/331; emulator real-e
```
