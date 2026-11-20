# CHECKPOINT 2758 — read me first, then TASKS.md

**Written:** 2026-11-20T18:56:10Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/adoring-hamilton-58trdj` · **builds on:** `4f2a143a` (this checkpoint is the commit after it)

## Just done
RVW4 DONE AND SHIPPED: alpha75 RELEASED AND RECORDED = kestrel-g-v0.3.0-alpha75 (gecko code 176, run 44794938085 all jobs green incl. certificate pin + APK audit + gecko pref check, APK 96.6 MB + sha256 + .dm in the Release, BUILDLOG.md line pushed). Contents: NL1 Mozilla site fixes (webcompat 160.0.1, proven starting on the real engine in lab run 44787922316), NL2 real-page finder tool + HTML probe guard. Suites: core 1907 / app 450 / web 649 / lint 0 failures.

## Do this next
NOTHING is running. Waiting for the owner's tablet test of alpha75 (Diagnostics must show 'Site fixes: Mozilla's compatibility fixes 160.4.0 running'; watch memory) and still alpha74's (video retry needs a real dropout; downloads yield to a film). Open ideas, none started: PB2 own player through the phone proxy (needs emulator + Courier), PB4 keep the inline player when going full screen, PB9 buffer bytes by bitrate, PB10 release a backgrounded paused player, a dclk_video_ads / gvt1 ad-video address rule (seen on ted.com: a 15 s ad mp4 listed next to the film), real-page probes for Reddit/X/TikTok need a login the sandbox lacks. Re-run tools/sitelab/realfinder.js (NODE_PATH=$(npm root -g) node tools/sitelab/realfinder.js url ...; at most 3 urls per call) whenever the finder changes

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  a0ad95ec ckpt 2757: REL75b: alpha75 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  eebefd5f ckpt 2756: gated kestrel-g-v0.3.0-alpha75 (gecko code 176) and pushed it: handoff self-
  9e99464e ckpt 2755: RVW4: whole JVM suite GREEN (:core:test 1907 tests 0 failures 12 skipped as 
  b1ec2fd4 ckpt 2754: NL1 PROVEN on the real engine: lab run 44787922316 (daily, 62 scenes, 12 min
  ae4e6a11 ckpt 2753: RVW4: NL2 done: realfinder.js (real pages through the real background.js + c
  d637614b ckpt 2752: NL1: licence row (docs/FILTER-LISTS.md) + HANDOFF engine step written; emula
  85af50a4 ckpt 2751: RVW4-NL1 code done and compiling: Gradle prepareWebcompat (AAR from Mozilla 
  934b8dad ckpt 2750: RVW4a: researched. Engine prefs / downloads / finder are saturated (all chea
  8d40d448 ckpt 2749: RVW4 written into TASKS.md in the owner's words (new levers for speed, compa
  aeef6973 ckpt 2748: RVW3 DONE AND SHIPPED: alpha74 RELEASED AND RECORDED = kestrel-g-v0.3.0-alph
```
