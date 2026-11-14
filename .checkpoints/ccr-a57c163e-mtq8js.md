# CHECKPOINT 2154 — read me first, then TASKS.md

**Written:** 2026-11-14T15:32:20Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `d229be2` (this checkpoint is the commit after it)

## Just done
AW1c DONE and EMULATOR-PROVEN (Gecko 160): data: stand-ins for script/image/xhr/fetch/media/frame/IMA, nothing reached the server on adlab.test; engine.js == Kotlin on 66,761 real-list requests (0 mismatches); default.kfs regenerated 6,575 KB with the redirect words; DefaultListsRegressionTest green; research/ADBLOCK_ENGINE.md 14.2; AW1c ticked

## Do this next
AW1d network entity domains is a long tail (only 266 network rules skipped as ENTITY_DOMAIN in the real lists) so do AW1e FIRST: analyse ../ublockorigin/uassets/filters/quick-fixes.txt (which scriptlets it uses vs the 28 of scriptlets-main.js; trusted-replace-fetch-response / trusted-replace-xhr-response / json-prune on YouTube player responses are the likely gaps), add it to scripts/make-default-lists.sh + FilterSources/FilterUpdater behind a labelled Settings switch ('Extra fixes for video sites: the sites' terms may forbid it, playback may be limited', default ON, said plainly in the answer), implement the missing scriptlets in scriptlets-main.js with Chromium tests, then AW1d, AW1f (false-positive safety), AW1g (Diagnostics + lab proof of the new pieces), AW1h ship alpha37 (versionCode 138, extension manifest 1.0.138) + answer the owner (AW1i: he repeated message 37); the emulator is running (AVD kestrel29x64, lab server tools/sitelab/adlab.py 8001, adb reverse 8001, debug.kestrel.pref1 set; the slow emulator UI sometimes shows 'System UI isn't responding': tap Wait); heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN is disabled

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  41441d2 ckpt 2148: AW1c BUILT (not yet emulator-proven): engine.js mirrors FilterSet.check for $
  8085098 ckpt 2130: AW1b DONE and EMULATOR-PROVEN: early delivery works (lab adlab.test vs contro
  3529f3d ckpt 2127: AW1b+ early scriptlet delivery BUILT: lab proved the late race (calls ran 100
  26045f2 ckpt 2103: AW1b step 2 DONE: scriptlets-main.js (our own library, 28 scriptlets) + web-t
  476faa3 ckpt 2095: AW1b step 1 DONE (core): ScriptletRules (uBO argument syntax, 23 canonical na
  d580e95 ckpt 2088: AW1a done: blocker mapped and the plan written to research/ADBLOCK_ENGINE.md 
  cdd4e9d ckpt 2086: owner message 37 written into TASKS (AW1 finish the ad blocking: scriptlets, 
  aba038a ckpt 2083: heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN DISABLED (update_trigger enabled=fals
  5375332 ckpt 2081: alpha36 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha36 (gecko code 137), run
  104a302 ckpt 2075: alpha36 DISPATCHED: android.yml run 43563546964 (run_number 87, app=gecko ful
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
