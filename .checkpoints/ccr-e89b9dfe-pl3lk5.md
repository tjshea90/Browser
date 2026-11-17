# CHECKPOINT 2620 — read me first, then TASKS.md

**Written:** 2026-11-17T09:59:26Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `d31dee56` (this checkpoint is the commit after it)

## Just done
LAB5a digests written (research/LAB5_PACKAGING, _STARTUP, _EXTENSION, _PREFSWEEP.md). PACK RUN READ (run 44053244976, 4 builds of the same code): omni.ja STORED = warm start -11 %, engine ready -12 %, page loaded -11 %, memory after the page set -28 MB (590 -> 562); libs in APK = no start/memory gain, +103 MB on x86 (+65 MB on ARM32) so NOT done. CHANGES (not yet built/tested end to end): omniStored is DEFAULT ON in geckoapp/build.gradle.kts (+ verify-apk.sh asserts omni.ja is Stored; emu-lab pack builds pin -PomniStored=false for A and C); engine prefs config v10 = captchadetection.actor.enabled=false, signon.rememberSignons=false, browser.formfill.enable=false (49 names verified by tools/check-gecko-prefs.sh, read back in Diagnostics); StorageInfo.purgeDoomedCache no longer walks cache2/entries (+StorageInfoPurgeTest, Health line storage purge-doomed); extension: ONE runtime.onMessage dispatcher instead of 24 listeners (background.js onMsg(), every handler still runs, first defined answer wins, a throwing handler is logged and skipped), players.js entry had TWO exclude_matches keys (JSON kept the YouTube list only, so it ran in captcha frames): merged + a duplicate-key test of the manifest, token scan skips repeated raw class strings and runs the load pass at idle; background.test.js 99 pass. DAILY RUN 44054301212 DELIVERED A REAL ART PROFILE (the debug build saves one): tools/profile-to-baseline.py turned it into geckoapp/src/main/baseline-prof.txt (14,100 rules, 11,173 methods, 2,927 classes); local assembleRelease running (log in the scratchpad gradle-release.log) to confirm assets/dexopt/baseline.prof is packed. SWEEP RUN 44059232336 in flight (variants base base2 nowarm nursery16 gcpar gcsmall canvas64, 5 rounds).

## Do this next
1) read sweep run 44059232336 (SWEEP-REPORT.md: base vs base2 = noise floor, nowarm = the start-up effect of GeckoEngine.prewarmConfig, GC prefs) and fill research/LAB5_PREFSWEEP.md; 2) read the local release build log + unzip -v of the APK (omni.ja Stored? assets/dexopt/baseline.prof size?) and add the baseline.prof assert to tools/verify-apk.sh; 3) packlab compile-mode A/B (cmd package compile verify vs speed-profile vs speed); 4) LAB5c warm-thread part 2 + defer start chores (research/LAB5_STARTUP.md rows 1,4,5,7); 5) full tests: ./gradlew :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest and cd web-tests && NODE_PATH=$(npm root -g) npm test; 6) after-run daily + alpha64 release (code 165) by the recipe

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a55d8865 ckpt 2619: LAB5c-1 (first real app change, measured by a same-APK A/B): GeckoEngine.pre
  da746ada ckpt 2618: LAB5b: more lab tooling committed (no run yet): suite 'sweep' = tools/emu/pr
  96e097e3 ckpt 2617: LAB5b: pref-sweep tooling started: GeckoPrefs.writeConfig reads debug.kestre
  97f31512 ckpt 2616: LAB5b: packaging A/B lab built and wired: geckoapp/build.gradle.kts has two 
  1cf2f4fd ckpt 2615: LAB5 (owner message 2026-11-17T06:42:30Z: improve the browser as much as pos
  cc1424d3 ckpt 2614: alpha63 SHIPPED + recorded (run 44046401335 green, release kestrel-g-v0.3.0-
  12cd7f08 ckpt 2613: alpha63 dispatched ONCE (run_workflow app=gecko full_build=true ref ccr-a10e
  d59a6ddb ckpt 2612: gated kestrel-g-v0.3.0-alpha63 (gecko code 164) and pushed it: handoff self-
  3cdec9fe ckpt 2611: run 9 read: degraded run (emulator restarted, no network, Google A/B all 60 
  ede3c889 ckpt 2610: OWNER RESCHEDULE (usage running out, 2026-11-17 04:58Z): ALL work moved to O
```

(20 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
