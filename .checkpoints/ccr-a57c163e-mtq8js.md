# CHECKPOINT 2103 — read me first, then TASKS.md

**Written:** 2026-11-14T06:45:59Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `a7f1504` (this checkpoint is the commit after it)

## Just done
AW1b step 2 DONE: scriptlets-main.js (our own library, 28 scriptlets) + web-tests/scriptlets.test.js 22 tests in Chromium + content.js hand-off (cuts /*ks..*/ out of the css, event + READY, REPORT relay) with 4 content tests, background.js registers it (world MAIN) and counts, Prefs.shieldScripts + Settings switch + Flavor.hasScriptlets, ShieldsBridge sends the calls (works with element hiding off), default.kfs regenerated 6.5 MB with 12,069 scriptlet rules + entity rules; lab: tools/sitelab/adlab.py + adlab-rules.txt

## Do this next
emulator proof of AW1b: build x86_64 debug APK (-PgeckoAbi=x86_64), compile lab .kfs (tools:run --out lab.kfs abp:ads:tools/sitelab/adlab-rules.txt), adb push to files/filters/current.kfs via run-as, setprop debug.kestrel.pref1 network.dns.localDomains=adlab.test,control.test,ads.adlab.test, adb reverse tcp:8001, python3 tools/sitelab/adlab.py 8001 log, open http://adlab.test:8001/ and http://control.test:8001/ and compare the check lines; then AW1c redirect stand-ins, AW1d entity network rules

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    A  tools/sitelab/adlab-rules.txt

## Last ten checkpoints
```
  476faa3 ckpt 2095: AW1b step 1 DONE (core): ScriptletRules (uBO argument syntax, 23 canonical na
  d580e95 ckpt 2088: AW1a done: blocker mapped and the plan written to research/ADBLOCK_ENGINE.md 
  cdd4e9d ckpt 2086: owner message 37 written into TASKS (AW1 finish the ad blocking: scriptlets, 
  aba038a ckpt 2083: heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN DISABLED (update_trigger enabled=fals
  5375332 ckpt 2081: alpha36 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha36 (gecko code 137), run
  104a302 ckpt 2075: alpha36 DISPATCHED: android.yml run 43563546964 (run_number 87, app=gecko ful
  edcc148 ckpt 2074: gated kestrel-g-v0.3.0-alpha36 (gecko code 137) and pushed it: handoff self-t
  d44b251 ckpt 2073: version bump 137 / 0.3.0-alpha36 for the release of AV1-AV3 (resize/detach, l
  eac3369 ckpt 2061: AV3 EMULATOR-PROVEN (page-field row, live guess mid-glide her|he|hd, held dig
  c5c3d98 ckpt 2057: AV2b done: repeated-letter words ('awwww') never learned (PersonalModel.stret
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
