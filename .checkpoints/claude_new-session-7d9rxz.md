# CHECKPOINT 2364 — read me first, then TASKS.md

**Written:** 2026-11-15T01:48:52Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `ff8a70db` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha45 (gecko code 146) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/new-session-7d9rxz, inputs {"app":"gecko","full_build":"true","note":"alpha45: an outside check of the ad-layer code in alpha44 found it could switch off a page's own invisible buttons and menus, so that part is gone (now only an invisible frame of ANOTHER site laid over a video is switched off, and it is given back if it turns out to be only fading in); the rule that refuses big links to other sites no longer catches an ordinary news card with a title; the ad skipper for JW Player, Video.js and Plyr no longer mistakes a short film for an ad and puts the film back at once when it returns after an ad; video players that use a blob address (most modern embeds) are matched to the right playlist by length, so the film that really plays comes first in the download sheet, and a clip under a minute next to a film no longer counts; DASH playlists are read early too; words such as trailer, intro or hero only push a file down when it is also much smaller than the biggest one; Diagnostics shows how long each of the extension's web-request checks holds the thread. Tested on the emulator and in test browsers, not on your tablet."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha45), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha45 "alpha45: an outside check of the ad-layer code in alpha44 found it could switch off a page's own invisible buttons and menus, so that part is gone (now only an invisible frame of ANOTHER site laid over a video is switched off, and it is given back if it turns out to be only fading in); the rule that refuses big links to other sites no longer catches an ordinary news card with a title; the ad skipper for JW Player, Video.js and Plyr no longer mistakes a short film for an ad and puts the film back at once when it returns after an ad; video players that use a blob address (most modern embeds) are matched to the right playlist by length, so the film that really plays comes first in the download sheet, and a clip under a minute next to a film no longer counts; DASH playlists are read early too; words such as trailer, intro or hero only push a file down when it is also much smaller than the biggest one; Diagnostics shows how long each of the extension's web-request checks holds the thread. Tested on the emulator and in test browsers, not on your tablet."
NEVER ship kestrel-g-v0.3.0-alpha45 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  c8a968e7 ckpt 2363: RX3-V4 coded + unit-proven (not ticked yet): core MediaList.applyBlob/matchB
  1dea80c7 ckpt 2362: RX3-S2 DONE and ticked (counters only): background.js LST + lstTimed around 
  d90ec3e7 ckpt 2361: alpha44 gated, pushed and dispatched (G4 fake play layers, V3 played element
  02082e19 ckpt 2360: gated kestrel-g-v0.3.0-alpha44 (gecko code 145) and pushed it: handoff self-
  072024e2 ckpt 2359: alpha43 gated, pushed and dispatched to GitHub (V5, D1, S1, B2, G5, G6); wai
  cca67983 ckpt 2358: gated kestrel-g-v0.3.0-alpha43 (gecko code 144) and pushed it: handoff self-
  18b548ee ckpt 2357: pre-ship: alpha43: the glow stops pulsing while a video plays; two more trac
  b7d3f84a ckpt 2356: RX3-S1, B2, G5, G6 DONE and ticked (S1 + G6 emulator-proven, B2 measured wit
  822e687f ckpt 2355: RX3-V5 and RX3-D1 DONE (ticked): AdMarkers/AdRuns/early look and the outage 
  dec2df9d ckpt 2354: alpha42 shipped to GitHub (gated + pushed + android.yml dispatched): G7 + V1
```

(33 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
