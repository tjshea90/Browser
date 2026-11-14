# CHECKPOINT 2178 — read me first, then TASKS.md

**Written:** 2026-11-14T16:06:37Z · **tests:** 1 RED: test_handoff.sh (0 green)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `4bd8519` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha37 (includes alpha36): Better ad blocking. 1) A site now gets its own anti-ad fixes BEFORE the page's own scripts run, so 'turn off your ad blocker' walls, countdown timers, pop-up openers and ad scripts are stopped at the start instead of after the page has already shown them. These are the fixes the blocking lists have for each site. Settings > Fix ad and anti-adblock scripts switches points 1 to 3 off. 2) When an ad file is blocked, the page gets a harmless empty stand-in of the same kind where the lists say so (an empty script, a 1x1 picture, a silent clip, an ad library that answers 'no ad available'), so a video player that waits for its ad code starts the video instead of hanging. 3) For some video players and news feeds the ad part of the data they download is cut out before the page reads it (the way the lists handle YouTube-style players), links on link-shortener pages point at the real target, and the common 'ad blocker detected' scripts are told there is no blocker. 4) Blocking rules for whole families of sites (one name with any ending, like yts.*) now work, and the lists' Quick fixes list is added (it refreshes daily with the others). 5) Menu > Diagnostics > Blocker line now says how many pages ran fixes, how many of them before the page's own scripts, and how many blocked files got a stand-in. Not tried on the real tablet or on real sites yet, only on a test page on the emulator: if a site breaks, switch Shields off for that site or turn the Settings switch off.

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/build.gradle.kts

## Last ten checkpoints
```
  4bd8519 ckpt 2177: AW1d DONE (network $domain=site.* entity domains in Kotlin + engine.js, diffe
  7e29314 ckpt 2165: AW1e BUILT and EMULATOR-PROVEN (Gecko 160 lab): trusted-replace-fetch-respons
  7cfc54d ckpt 2154: AW1c DONE and EMULATOR-PROVEN (Gecko 160): data: stand-ins for script/image/x
  41441d2 ckpt 2148: AW1c BUILT (not yet emulator-proven): engine.js mirrors FilterSet.check for $
  8085098 ckpt 2130: AW1b DONE and EMULATOR-PROVEN: early delivery works (lab adlab.test vs contro
  3529f3d ckpt 2127: AW1b+ early scriptlet delivery BUILT: lab proved the late race (calls ran 100
  26045f2 ckpt 2103: AW1b step 2 DONE: scriptlets-main.js (our own library, 28 scriptlets) + web-t
  476faa3 ckpt 2095: AW1b step 1 DONE (core): ScriptletRules (uBO argument syntax, 23 canonical na
  d580e95 ckpt 2088: AW1a done: blocker mapped and the plan written to research/ADBLOCK_ENGINE.md 
  cdd4e9d ckpt 2086: owner message 37 written into TASKS (AW1 finish the ad blocking: scriptlets, 
```
