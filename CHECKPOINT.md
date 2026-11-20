# CHECKPOINT 2756 — read me first, then TASKS.md

**Written:** 2026-11-20T18:39:50Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/adoring-hamilton-58trdj` · **builds on:** `f2afe894` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha75 (gecko code 176) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/adoring-hamilton-58trdj, inputs {"app":"gecko","full_build":"true","note":"alpha75: better page compatibility and a cleaner video list. PAGE COMPATIBILITY: Kestrel G now carries Mozilla's own list of fixes for sites that break in other browsers, the same list stock Firefox for Android has and this app was missing. It covers hundreds of sites (321 cases for phones in this version): pages that say your browser is unsupported, phone pages that open as desktop pages, broken sign-in scripts, and sites that only work when they see a different browser name (for example LinkedIn, CNN, Office 365 and Outlook, TikTok comments, several banks, Nintendo). It starts 12 seconds after the first page of a session has loaded, so opening the app is not slower, and it adds no network traffic of its own: the list that comes with the build is used and is renewed when the engine is updated. Settings, Network lab, Site fixes from Mozilla turns it off at once; Diagnostics shows a Site fixes line. VIDEO FINDING: the video finder was run against real players from the build computer for the first time (hls.js, video.js, Plyr, JW Player, Vimeo, Streamable, archive.org, MDN, Wikimedia): every stream and file that was playing was found with its right size. One fix came out of it: a web page whose address merely ends like a video file (Wikimedia File pages are one) could be counted as a video of the size of the page; a web page reply is no longer taken as a file size. WHAT I COULD NOT PROVE ON THE TABLET: on the emulator with the real Firefox engine the site fixes start in 0.2 seconds, cause no script errors and no extra memory (peak 298 MB against 303 MB before), but no emulator page was one that Mozilla has a fix for, so the fixes themselves are not seen working, and nothing was timed on the tablet."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha75), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha75 "alpha75: better page compatibility and a cleaner video list. PAGE COMPATIBILITY: Kestrel G now carries Mozilla's own list of fixes for sites that break in other browsers, the same list stock Firefox for Android has and this app was missing. It covers hundreds of sites (321 cases for phones in this version): pages that say your browser is unsupported, phone pages that open as desktop pages, broken sign-in scripts, and sites that only work when they see a different browser name (for example LinkedIn, CNN, Office 365 and Outlook, TikTok comments, several banks, Nintendo). It starts 12 seconds after the first page of a session has loaded, so opening the app is not slower, and it adds no network traffic of its own: the list that comes with the build is used and is renewed when the engine is updated. Settings, Network lab, Site fixes from Mozilla turns it off at once; Diagnostics shows a Site fixes line. VIDEO FINDING: the video finder was run against real players from the build computer for the first time (hls.js, video.js, Plyr, JW Player, Vimeo, Streamable, archive.org, MDN, Wikimedia): every stream and file that was playing was found with its right size. One fix came out of it: a web page whose address merely ends like a video file (Wikimedia File pages are one) could be counted as a video of the size of the page; a web page reply is no longer taken as a file size. WHAT I COULD NOT PROVE ON THE TABLET: on the emulator with the real Firefox engine the site fixes start in 0.2 seconds, cause no script errors and no extra memory (peak 298 MB against 303 MB before), but no emulator page was one that Mozilla has a fix for, so the fixes themselves are not seen working, and nothing was timed on the tablet."
NEVER ship kestrel-g-v0.3.0-alpha75 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  9e99464e ckpt 2755: RVW4: whole JVM suite GREEN (:core:test 1907 tests 0 failures 12 skipped as 
  b1ec2fd4 ckpt 2754: NL1 PROVEN on the real engine: lab run 44787922316 (daily, 62 scenes, 12 min
  ae4e6a11 ckpt 2753: RVW4: NL2 done: realfinder.js (real pages through the real background.js + c
  d637614b ckpt 2752: NL1: licence row (docs/FILTER-LISTS.md) + HANDOFF engine step written; emula
  85af50a4 ckpt 2751: RVW4-NL1 code done and compiling: Gradle prepareWebcompat (AAR from Mozilla 
  934b8dad ckpt 2750: RVW4a: researched. Engine prefs / downloads / finder are saturated (all chea
  8d40d448 ckpt 2749: RVW4 written into TASKS.md in the owner's words (new levers for speed, compa
  aeef6973 ckpt 2748: RVW3 DONE AND SHIPPED: alpha74 RELEASED AND RECORDED = kestrel-g-v0.3.0-alph
  a1da4068 ckpt 2747: REL74b: alpha74 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  4c5ff5b3 ckpt 2746: gated kestrel-g-v0.3.0-alpha74 (gecko code 175) and pushed it: handoff self-
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
