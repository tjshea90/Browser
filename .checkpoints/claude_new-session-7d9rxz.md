# CHECKPOINT 2365 — read me first, then TASKS.md

**Written:** 2026-11-15T01:49:32Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `d636a48a` (this checkpoint is the commit after it)

## Just done
alpha45 gated (JVM + web 373/373), pushed and dispatched to GitHub (workflow android.yml, ref claude/new-session-7d9rxz, dispatched ~01:49Z): independent-review fixes (G4 v2 frames only, overlay-link rule, players.js film-length/hold/skip buttons), RX3-V4 blob players + clip rule + DASH look, V6 soft words, V7 corpus, S2 counters; V4 V6 V7 Y5 REV S2 ticked; observer budget test now uses a fake clock (it was load-sensitive).

## Do this next
FINISH alpha45 (heartbeat trig_01KEDB7Qxd8zFeawjGcAkSQZ stays until this is answered): wait for the run (mcp__github__actions_list list_workflow_runs android.yml filter event workflow_dispatch branch claude/new-session-7d9rxz; tag waiter writes scratchpad tagwait45.out) to be green and git ls-remote --tags origin kestrel-g-v0.3.0-alpha45 to show the tag; mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha45 must list the APK (a courier/timing flake: rerun_failed_jobs on that run, NEVER a new dispatch); THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha45 "$NOTE" with this IDENTICAL note; then answer the owner in plain words with https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha45 on its own line: say HONESTLY that an independent review of alpha44 found its ad-layer code could disable a page's own invisible controls and that alpha45 removes that part (so the first tap relies on the assist as in alpha40-43); what is proven (test browsers: fakelayer 8, players 19, guard +3, blob 4; JVM MediaListBlobTest 14 + FinderCorpusTest; emulator /blob-lab: blob player matched to the 300 s playlist, 45 s clip not counted) and NOT run on the tablet/real sites; the Y5 sentence (YouTube may warn or limit playback, more likely signed in, one switch Skip ads inside video players turns it off); re-list open items: BK5 question, AX1g, tablet feedback alpha40-45, one Diagnostics copy after a Proton Mail session (S2 numbers: Blocker line 'Extension listeners'). After that: delete the heartbeat (delete_trigger trig_01KEDB7Qxd8zFeawjGcAkSQZ) unless S4 (blocker histogram, counters only) is started: remaining RX3 rows are tablet/owner-gated (BK3 BK4 M1 H1 S4 S5 S6 V4->done V6->done V7->done D2 D3 B3 Y4 BK5). note45 text: alpha45: an outside check of the ad-layer code in alpha44 found it could switch off a page's own invisible buttons and menus, so that part is gone (now only an invisible frame of ANOTHER site laid over a video is switched off, and it is given back if it turns out to be only fading in); the rule that refuses big links to other sites no longer catches an ordinary news card with a title; the ad skipper for JW Player, Video.js and Plyr no longer mistakes a short film for an ad and puts the film back at once when it returns after an ad; video players that use a blob address (most modern embeds) are matched to the right playlist by length, so the film that really plays comes first in the download sheet, and a clip under a minute next to a film no longer counts; DASH playlists are read early too; words such as trailer, intro or hero only push a file down when it is also much smaller than the biggest one; Diagnostics shows how long each of the extension's web-request checks holds the thread. Tested on the emulator and in test browsers, not on your tablet.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d636a48a ckpt 2364: gated kestrel-g-v0.3.0-alpha45 (gecko code 146) and pushed it: handoff self-
  c8a968e7 ckpt 2363: RX3-V4 coded + unit-proven (not ticked yet): core MediaList.applyBlob/matchB
  1dea80c7 ckpt 2362: RX3-S2 DONE and ticked (counters only): background.js LST + lstTimed around 
  d90ec3e7 ckpt 2361: alpha44 gated, pushed and dispatched (G4 fake play layers, V3 played element
  02082e19 ckpt 2360: gated kestrel-g-v0.3.0-alpha44 (gecko code 145) and pushed it: handoff self-
  072024e2 ckpt 2359: alpha43 gated, pushed and dispatched to GitHub (V5, D1, S1, B2, G5, G6); wai
  cca67983 ckpt 2358: gated kestrel-g-v0.3.0-alpha43 (gecko code 144) and pushed it: handoff self-
  18b548ee ckpt 2357: pre-ship: alpha43: the glow stops pulsing while a video plays; two more trac
  b7d3f84a ckpt 2356: RX3-S1, B2, G5, G6 DONE and ticked (S1 + G6 emulator-proven, B2 measured wit
  822e687f ckpt 2355: RX3-V5 and RX3-D1 DONE (ticked): AdMarkers/AdRuns/early look and the outage 
```
