# CHECKPOINT 2624 — read me first, then TASKS.md

**Written:** 2026-11-17T11:25:28Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `c19b6392` (this checkpoint is the commit after it)

## Just done
RELEASE PREP (not shipped): versionCode 165 / versionName 0.3.0-alpha64 in geckoapp/build.gradle.kts, extension manifest 1.0.165 (tools/sync-ext-version.sh), docs/handoff/ALPHA64_RELEASE_NOTE.txt drafted (ASCII, no quotes/backslashes; add the aot numbers before dispatching). VP RUN READ (44066133643): 11 of 12 scenes ok incl. web-videos-in-kestrels-player (hand-over chain through the single extension listener), VLC gestures, quality inside the page; film-inside-the-page FAILED because alpha62's loop breaker (WebHandOver.playedThrough, 10 min window) declined the clip the ho scene had just played: Diagnostics says 'last reason: Kestrel's player played this clip to its end a moment ago'; not a LAB5 effect; lab fix = debug-only command handreset (BrowserActivity) called at the start of ip_scene (compiles). lab_suite=ip queued on that commit; aot run (compile at install, release build profile) running behind/with it.

## Do this next
1) read aot run (AOT-REPORT.md: debug verify vs speed-profile vs speed; release build: embedded profile via INSTALL_PROFILE, am start -W TotalTime; dm install check) and the ip run (all 21 checks of film-inside-the-page must pass = the inline player works after the extension changes); 2) put the aot sentence into docs/handoff/ALPHA64_RELEASE_NOTE.txt; 3) git ls-remote --tags origin (no alpha64), bash ship.sh gecko with the note text, then ONE dispatch: android.yml ref ccr-e89b9dfe-pl3lk5 inputs app=gecko full_build=true note; verify tag + jobs + release asset; bash tools/record-release.sh kestrel-g-v0.3.0-alpha64 with the IDENTICAL note; 4) dispatch lab_suite=daily on the released commit and write research/lab-baseline.json from it (buildreport.py --write-baseline); 5) answer the owner in plain words; tick LAB3/LAB4/LAB5 rows; delete send_later trigger trig_01N9rqESvUntvENiV7VxVhfi

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  1c468757 ckpt 2623: SWEEP READ (run 44059232336, cancelled after the emulator went offline: 17 g
  bd981349 ckpt 2622: LAB5: OwnerRulesTest.lab5MeasuredPackagingProfileAndPrefsStay (33 tests in t
  c83d9f96 ckpt 2621: LAB5c batch 2 proven by tests: JVM suite 1972 tests / 0 failures (core + app
  41f2a308 ckpt 2620: LAB5a digests written (research/LAB5_PACKAGING, _STARTUP, _EXTENSION, _PREFS
  a55d8865 ckpt 2619: LAB5c-1 (first real app change, measured by a same-APK A/B): GeckoEngine.pre
  da746ada ckpt 2618: LAB5b: more lab tooling committed (no run yet): suite 'sweep' = tools/emu/pr
  96e097e3 ckpt 2617: LAB5b: pref-sweep tooling started: GeckoPrefs.writeConfig reads debug.kestre
  97f31512 ckpt 2616: LAB5b: packaging A/B lab built and wired: geckoapp/build.gradle.kts has two 
  1cf2f4fd ckpt 2615: LAB5 (owner message 2026-11-17T06:42:30Z: improve the browser as much as pos
  cc1424d3 ckpt 2614: alpha63 SHIPPED + recorded (run 44046401335 green, release kestrel-g-v0.3.0-
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
