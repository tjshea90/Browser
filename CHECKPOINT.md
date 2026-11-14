# CHECKPOINT 2165 — read me first, then TASKS.md

**Written:** 2026-11-14T15:47:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `efc02b9` (this checkpoint is the commit after it)

## Just done
AW1e BUILT and EMULATOR-PROVEN (Gecko 160 lab): trusted-replace-fetch-response / trusted-replace-xhr-response (text rewritten in the answer: adPlacements -> no_ads, adSlots -> no_slots, all matching rules apply in turn), nobab/nofab (BlockAdBlock/FuckAdBlock stub accessors, callback fired), popads-dummy, norafif, href-sanitizer (?url, -base64, text; bug found on the emulator: a finished link was re-read without its parameter and pointed at the page itself -> empty target rejected, test strengthened); /api/graphql-style needles are addresses not /regex/flags (flags only gimsuy); quick-fixes list added to make-default-lists.sh + FilterUpdater (quick-fixes.min.txt) + docs; default.kfs 6,617 KB with 12,330 scriptlet rules (SCRIPTLET_UNSUPPORTED 594 -> 500); Settings text of the switch says what it does now (also stand-ins, video-player data, terms may forbid); web-tests scriptlets 34/34, Kotlin ScriptletRulesTest +2

## Do this next
run the FULL suites with cleanTest (./gradlew :core:cleanTest :core:test :app:testDebugUnitTest :app:lintDebug :geckoapp:compileDebugKotlin --offline, web-tests npm test, real-diff.js on a fresh KESTREL_REAL_DIFF dir) and fix what the new lists/scriptlets break; then AW1f (false-positive safety: DefaultListsRegressionTest corpus of mainstream hosts + payload sanity), AW1g (Diagnostics wording), research/ADBLOCK_ENGINE.md 14.3 for AW1e, AW1d (network entity domains, 279 rules, optional), AW1h ship alpha37: bump geckoapp versionCode 138 / 0.3.0-alpha37 (extension manifest 1.0.138 via ship.sh), bash ship.sh gecko note, dispatch android.yml, wait, record-release, answer the owner (AW1i: repeated message 37); the emulator is running (lab server 8001 + adb reverse + debug.kestrel.pref1 set; 'System UI isn't responding' dialog: tap Wait); heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN is disabled

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M app/src/main/kotlin/app/kestrel/browser/ui/SettingsView.kt

## Last ten checkpoints
```
  7cfc54d ckpt 2154: AW1c DONE and EMULATOR-PROVEN (Gecko 160): data: stand-ins for script/image/x
  41441d2 ckpt 2148: AW1c BUILT (not yet emulator-proven): engine.js mirrors FilterSet.check for $
  8085098 ckpt 2130: AW1b DONE and EMULATOR-PROVEN: early delivery works (lab adlab.test vs contro
  3529f3d ckpt 2127: AW1b+ early scriptlet delivery BUILT: lab proved the late race (calls ran 100
  26045f2 ckpt 2103: AW1b step 2 DONE: scriptlets-main.js (our own library, 28 scriptlets) + web-t
  476faa3 ckpt 2095: AW1b step 1 DONE (core): ScriptletRules (uBO argument syntax, 23 canonical na
  d580e95 ckpt 2088: AW1a done: blocker mapped and the plan written to research/ADBLOCK_ENGINE.md 
  cdd4e9d ckpt 2086: owner message 37 written into TASKS (AW1 finish the ad blocking: scriptlets, 
  aba038a ckpt 2083: heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN DISABLED (update_trigger enabled=fals
  5375332 ckpt 2081: alpha36 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha36 (gecko code 137), run
```

(10 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
