# CHECKPOINT 595 — read me first, then TASKS.md

**Written:** 2026-11-12T00:00:08Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `19e3a6d` (this checkpoint is the commit after it)

## Just done
alpha13 DISPATCHED on GitHub Actions: run 43123971432 (android.yml, head 19e3a6d, queued 23:59:47Z, expect green about 00:06Z; alpha12 took 5.5 min). DO NOT dispatch again. Gate before it: core+app suites, lint release, tools build, 22 JS tests, real-lists differential 63,378 requests 0 mismatches.

## Do this next
WAIT for run 43123971432 (mcp__github__actions_list list_workflow_jobs 43123971432 is the small poll; all jobs completed + build-gecko success = green). If green: mcp__github__get_release_by_tag kestrel-g-v0.2.0-alpha13 shows the APK, then bash tools/record-release.sh kestrel-g-v0.2.0-alpha13 "$(cat /tmp/claude-0/-home-user-Browser/30a5aa00-41c9-5bd3-87d1-b10052a52d84/scratchpad/note13.txt)" (note also in ckpt 594's message), ckpt, then tell the owner: Release link on its own line, what changed (D1 in-engine blocking, saved list copy, Diagnostics Blocker line + switch, smaller iframe CSS), honest caveats (emulator only, Diagnostics > Update > Copy > send me the Blocker line), and re-list open items. If it FAILED: get the failing job log (web-fetch agent with the signed logs_url), fix, re-gate, re-dispatch (never ship the tag twice). After alpha13: remaining V7 items (rest of P5 cold-start order, P9 onLoadRequest, Gecko pref A/Bs, S3/S5, S7 do not touch blind), V3b download-only process, tab-grid swipe-to-close, Settings restyle.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  19e3a6d ckpt 594: gated kestrel-g-v0.2.0-alpha13 (gecko code 112) and pushed it: handoff self-te
  16223f8 ckpt 592: D1 code complete and verified in unit tests: real-lists differential (63,378 r
  4316802 ckpt 571: D1 steps 1+2 DONE and verified: core/.../filter/ExtImage.kt (KEX1 little-endia
  6e451c7 ckpt 555: alpha12 SHIPPED and recorded (run 43111148506 green, BUILDLOG written): https:
  66aa048 ckpt 551: alpha12 DISPATCHED on GitHub Actions: run 43111148506 (android.yml, head 77eac
  77eac10 ckpt 550: gated kestrel-g-v0.2.0-alpha12 (gecko code 111) and pushed it: handoff self-te
  e82c7f6 ckpt 543: DASH + merge DONE and emulator-verified: core/media/Dash.kt (MPD parser: Segme
  5220430 ckpt 528: TS->MP4 (Remuxer + ADTS->raw AAC frames with an exact AudioClock; verified on 
  bb6a984 ckpt 523: alpha11 SHIPPED and recorded: https://github.com/tjshea90/Browser/releases/tag
  5d55a7b ckpt 513: alpha11 DISPATCHED: run 43101503028 (android.yml, head 6d529ad, 21:15Z). Gate 
```
