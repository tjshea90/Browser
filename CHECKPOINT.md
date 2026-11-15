# CHECKPOINT 2358 — read me first, then TASKS.md

**Written:** 2026-11-15T00:12:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `18b548ee` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha43 (gecko code 144) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/new-session-7d9rxz, inputs {"app":"gecko","full_build":"true","note":"alpha43: the glow stops pulsing while a video plays; two more tracker block lists (AdGuard Tracking Protection and HaGeZi Light, new switch Settings > Filter lists > Extra tracker lists, on by default; the first update downloads about 3 MB and rebuilds the lists); a page jump to an advertising site is now stopped even long after your last tap, and a new window opened by a button instead of a link needs one tap on the small blocked-window icon (sign-in windows are not affected); downloads wait out a lost connection instead of failing; video downloads leave out advertising breaks that the stream's own list gives away, and live, protected and clip-sized streams no longer count for the download badge. Tested on the emulator and in test browsers, not on your tablet."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha43), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha43 "alpha43: the glow stops pulsing while a video plays; two more tracker block lists (AdGuard Tracking Protection and HaGeZi Light, new switch Settings > Filter lists > Extra tracker lists, on by default; the first update downloads about 3 MB and rebuilds the lists); a page jump to an advertising site is now stopped even long after your last tap, and a new window opened by a button instead of a link needs one tap on the small blocked-window icon (sign-in windows are not affected); downloads wait out a lost connection instead of failing; video downloads leave out advertising breaks that the stream's own list gives away, and live, protected and clip-sized streams no longer count for the download badge. Tested on the emulator and in test browsers, not on your tablet."
NEVER ship kestrel-g-v0.3.0-alpha43 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  18b548ee ckpt 2357: pre-ship: alpha43: the glow stops pulsing while a video plays; two more trac
  b7d3f84a ckpt 2356: RX3-S1, B2, G5, G6 DONE and ticked (S1 + G6 emulator-proven, B2 measured wit
  822e687f ckpt 2355: RX3-V5 and RX3-D1 DONE (ticked): AdMarkers/AdRuns/early look and the outage 
  dec2df9d ckpt 2354: alpha42 shipped to GitHub (gated + pushed + android.yml dispatched): G7 + V1
  652f583c ckpt 2353: gated kestrel-g-v0.3.0-alpha42 (gecko code 143) and pushed it: handoff self-
  feda2b32 ckpt 2352: RX3-V1 DONE (ticked) and RX3-V2 coded + proven on the emulator (teaser-link 
  21cda298 ckpt 2351: RX3-G7 DONE and ticked (full gates green: JVM + web 331/331; emulator real-e
  265fcf6e ckpt 2350: RX3-G7 other-players ad skipper coded and tested (15 mock tests + manifest/r
  69fcfcf1 ckpt 2349: RX3-Y3 live YouTube check recorded (research/YOUTUBE_ADS.md section 5, tools
  d10a5f4a ckpt 2348: alpha41 RE-DISPATCHED (second run; the first failed on the flaky FLK1 test a
```
