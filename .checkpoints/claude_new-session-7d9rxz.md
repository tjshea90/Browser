# CHECKPOINT 2354 — read me first, then TASKS.md

**Written:** 2026-11-14T23:16:26Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `652f583c` (this checkpoint is the commit after it)

## Just done
alpha42 shipped to GitHub (gated + pushed + android.yml dispatched): G7 + V1 + V2; waiting for the run

## Do this next
HEARTBEAT trig_01KEDB7Qxd8zFeawjGcAkSQZ (every 2 h; get_trigger to read; delete_trigger when the RX3 job is shipped). alpha42 (gecko code 143, G7 + V1 + V2) was GATED, PUSHED and the android.yml run was DISPATCHED at about 23:18Z (app=gecko full_build=true ref claude/new-session-7d9rxz). WAIT: git ls-remote --tags origin kestrel-g-v0.3.0-alpha42 shows the tag; mcp__github__actions_list list_workflow_runs (owner tjshea90 repo Browser resource_id android.yml, workflow_runs_filter {event workflow_dispatch, branch claude/new-session-7d9rxz}) newest run conclusion success, then list_workflow_jobs for that run id (all success); mcp__github__get_release_by_tag shows the APK. A courier/timing flake (ServerTest, ProxyLinkTest, a web test under load) -> mcp__github__actions_run_trigger rerun_failed_jobs run_id <id>, never a new dispatch. ONLY THEN: bash tools/record-release.sh kestrel-g-v0.3.0-alpha42 "alpha42: ad skipping now also works in other video players (JW Player, Video.js, Plyr and the Google ad players), with the same Settings switch and its own line in Diagnostics; the download list no longer counts hover previews, silent looping banner videos, thumbnail-sized videos and ads as videos, and a link to a video file only counts once Kestrel has asked the site how big it is. Tested on the emulator and in test browsers, not on your tablet." then answer the owner in plain words (Release link https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha42 on its own line; what was proven on the emulator/test browsers vs NOT on the tablet: G7 five scenarios in the real engine, V1 four elements roles + counted 1 of 4, V2 asked 2 counted 1; things to look at on the tablet: Diagnostics lines 'Other players ads', 'YouTube ads', 'video info', 'media verify'; open items: BK5 question, AX1g, tablet feedback alpha40/41/42). MEANWHILE (no build needed): Wave B next = V5 (core/media Hls.kt parse CUE-OUT/CUE-IN/SCTE35/DATERANGE into Seg flags, new core/media/AdRuns.kt, StreamEngine pre-seeds job.skipped, VideoFlow early playlist look for the first 3 playlists after the page settled; tests AdRunsTest + StreamEngineTest; lab /hls-ad-run), then V3, D1, G4, G5, G6, S1, B2 (research/REVIEW-EXTERNAL-3.md sections 5-9); claim each (bash tools/claim.sh RX3-<id>), failing test first, ckpt after every step; the next release is alpha43 (versionCode 144).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  652f583c ckpt 2353: gated kestrel-g-v0.3.0-alpha42 (gecko code 143) and pushed it: handoff self-
  feda2b32 ckpt 2352: RX3-V1 DONE (ticked) and RX3-V2 coded + proven on the emulator (teaser-link 
  21cda298 ckpt 2351: RX3-G7 DONE and ticked (full gates green: JVM + web 331/331; emulator real-e
  265fcf6e ckpt 2350: RX3-G7 other-players ad skipper coded and tested (15 mock tests + manifest/r
  69fcfcf1 ckpt 2349: RX3-Y3 live YouTube check recorded (research/YOUTUBE_ADS.md section 5, tools
  d10a5f4a ckpt 2348: alpha41 RE-DISPATCHED (second run; the first failed on the flaky FLK1 test a
  ff61cb61 ckpt 2347: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
  79518152 ckpt 2346: first alpha41 run FAILED on GitHub: core ProxyLinkTest.theRenewalKeepsItOpen
  d6379d16 ckpt 2345: alpha41 (gecko code 142, manifest 1.0.142) GATED, PUSHED and the android.yml
  836d9546 ckpt 2344: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
```
