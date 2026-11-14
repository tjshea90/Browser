# CHECKPOINT 2353 — read me first, then TASKS.md

**Written:** 2026-11-14T23:15:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `85f9974f` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha42 (gecko code 143) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/new-session-7d9rxz, inputs {"app":"gecko","full_build":"true","note":"alpha42: ad skipping now also works in other video players (JW Player, Video.js, Plyr and the Google ad players), with the same Settings switch and its own line in Diagnostics; the download list no longer counts hover previews, silent looping banner videos, thumbnail-sized videos and ads as videos, and a link to a video file only counts once Kestrel has asked the site how big it is. Tested on the emulator and in test browsers, not on your tablet."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha42), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha42 "alpha42: ad skipping now also works in other video players (JW Player, Video.js, Plyr and the Google ad players), with the same Settings switch and its own line in Diagnostics; the download list no longer counts hover previews, silent looping banner videos, thumbnail-sized videos and ads as videos, and a link to a video file only counts once Kestrel has asked the site how big it is. Tested on the emulator and in test browsers, not on your tablet."
NEVER ship kestrel-g-v0.3.0-alpha42 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  feda2b32 ckpt 2352: RX3-V1 DONE (ticked) and RX3-V2 coded + proven on the emulator (teaser-link 
  21cda298 ckpt 2351: RX3-G7 DONE and ticked (full gates green: JVM + web 331/331; emulator real-e
  265fcf6e ckpt 2350: RX3-G7 other-players ad skipper coded and tested (15 mock tests + manifest/r
  69fcfcf1 ckpt 2349: RX3-Y3 live YouTube check recorded (research/YOUTUBE_ADS.md section 5, tools
  d10a5f4a ckpt 2348: alpha41 RE-DISPATCHED (second run; the first failed on the flaky FLK1 test a
  ff61cb61 ckpt 2347: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
  79518152 ckpt 2346: first alpha41 run FAILED on GitHub: core ProxyLinkTest.theRenewalKeepsItOpen
  d6379d16 ckpt 2345: alpha41 (gecko code 142, manifest 1.0.142) GATED, PUSHED and the android.yml
  836d9546 ckpt 2344: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
  14a6a198 ckpt 2343: RX3-G1 DONE (guard+library declared in the manifest, world MAIN; emulator-pr
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
