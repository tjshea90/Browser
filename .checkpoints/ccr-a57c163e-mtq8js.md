# CHECKPOINT 2127 — read me first, then TASKS.md

**Written:** 2026-11-14T07:23:08Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `9b64d5e` (this checkpoint is the commit after it)

## Just done
AW1b+ early scriptlet delivery BUILT: lab proved the late race (calls ran 1005 ms after the first page script, dcl 63 ms); new app message sc (FilterSet.scriptletPayload + ShieldsBridge.scriptletCalls, quick lane, '<gen>:<json>'), background.js per-host contentScripts.register (MAIN, document_start, allFrames; library registered first; first visit never waits, hosts with a script wait <=250 ms for the answer; 40-host LRU; reset on list generation change), library dedups per CALL + reports new calls with an early flag, content.js listens for the report from document start; web-tests 192/192 green (background +11, scriptlets +3, content +1), core ScriptletRulesTest +2 and BridgeLaneTest green; emulator Diagnostics showed 'early error: Permission denied to register a content script for *://adlab.test/*' so patterns are now explicit http://host/* + https://host/* (rebuild + reinstall pending)

## Do this next
rebuild x86_64 debug APK (./gradlew :geckoapp:assembleDebug -PgeckoAbi=x86_64 --offline -q ~1 min), adb install -r -t geckoapp/build/outputs/apk/debug/geckoapp-debug.apk, load http://adlab.test:8001/?r=N three times (lab server: python3 tools/sitelab/adlab.py 8001 <scratchpad>/adlab.log, adb reverse tcp:8001, pref1 localDomains already set) and confirm Diagnostics Blocker line says 'sites set up early' and the lab shows wall hidden / nag not shown / bait did not run / countdown fast / player adPlacements=0 and timing 'x:0:1'; also load control.test for the baseline; then tick AW1b with the evidence (claim.sh AW1b --done) and go on with AW1c (engine.js redirect port, res/ stand-ins, ExtImageTest corpus), AW1d network entity domains, AW1e-h; heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN is disabled

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  26045f2 ckpt 2103: AW1b step 2 DONE: scriptlets-main.js (our own library, 28 scriptlets) + web-t
  476faa3 ckpt 2095: AW1b step 1 DONE (core): ScriptletRules (uBO argument syntax, 23 canonical na
  d580e95 ckpt 2088: AW1a done: blocker mapped and the plan written to research/ADBLOCK_ENGINE.md 
  cdd4e9d ckpt 2086: owner message 37 written into TASKS (AW1 finish the ad blocking: scriptlets, 
  aba038a ckpt 2083: heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN DISABLED (update_trigger enabled=fals
  5375332 ckpt 2081: alpha36 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha36 (gecko code 137), run
  104a302 ckpt 2075: alpha36 DISPATCHED: android.yml run 43563546964 (run_number 87, app=gecko ful
  edcc148 ckpt 2074: gated kestrel-g-v0.3.0-alpha36 (gecko code 137) and pushed it: handoff self-t
  d44b251 ckpt 2073: version bump 137 / 0.3.0-alpha36 for the release of AV1-AV3 (resize/detach, l
  eac3369 ckpt 2061: AV3 EMULATOR-PROVEN (page-field row, live guess mid-glide her|he|hd, held dig
```

(23 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
