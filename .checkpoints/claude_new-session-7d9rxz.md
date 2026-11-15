# CHECKPOINT 2357 — read me first, then TASKS.md

**Written:** 2026-11-15T00:12:12Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `b7d3f84a` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha43: the glow stops pulsing while a video plays; two more tracker block lists (AdGuard Tracking Protection and HaGeZi Light, new switch Settings > Filter lists > Extra tracker lists, on by default; the first update downloads about 3 MB and rebuilds the lists); a page jump to an advertising site is now stopped even long after your last tap, and a new window opened by a button instead of a link needs one tap on the small blocked-window icon (sign-in windows are not affected); downloads wait out a lost connection instead of failing; video downloads leave out advertising breaks that the stream's own list gives away, and live, protected and clip-sized streams no longer count for the download badge. Tested on the emulator and in test browsers, not on your tablet.

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/build.gradle.kts
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  b7d3f84a ckpt 2356: RX3-S1, B2, G5, G6 DONE and ticked (S1 + G6 emulator-proven, B2 measured wit
  822e687f ckpt 2355: RX3-V5 and RX3-D1 DONE (ticked): AdMarkers/AdRuns/early look and the outage 
  dec2df9d ckpt 2354: alpha42 shipped to GitHub (gated + pushed + android.yml dispatched): G7 + V1
  652f583c ckpt 2353: gated kestrel-g-v0.3.0-alpha42 (gecko code 143) and pushed it: handoff self-
  feda2b32 ckpt 2352: RX3-V1 DONE (ticked) and RX3-V2 coded + proven on the emulator (teaser-link 
  21cda298 ckpt 2351: RX3-G7 DONE and ticked (full gates green: JVM + web 331/331; emulator real-e
  265fcf6e ckpt 2350: RX3-G7 other-players ad skipper coded and tested (15 mock tests + manifest/r
  69fcfcf1 ckpt 2349: RX3-Y3 live YouTube check recorded (research/YOUTUBE_ADS.md section 5, tools
  d10a5f4a ckpt 2348: alpha41 RE-DISPATCHED (second run; the first failed on the flaky FLK1 test a
  ff61cb61 ckpt 2347: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
```
