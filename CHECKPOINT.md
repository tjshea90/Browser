# CHECKPOINT 2148 — read me first, then TASKS.md

**Written:** 2026-11-14T15:14:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `8dbad0c` (this checkpoint is the commit after it)

## Just done
AW1c BUILT (not yet emulator-proven): engine.js mirrors FilterSet.check for $redirect= / $redirect-rule= (cRedirect/cRedirectOnly, verdict bits 8-12; differential corpus mini.corpus.tsv now has an exhaustive stand-in block over every request type, 100% equal, fixtures regenerated incl. mini.redirects.tsv), host-rule category kept as before AW1c (last wins, engine.js identical), redirects.js = our own 22 stand-ins answered as data: URLs (no extension address leaks; IMA stub answers VAST_EMPTY_RESPONSE, GPT/GTM/GA/adsbygoogle/fingerprint2/nobab/apstag/chartbeat/prebid shims, 1x1 gif, png, mp3, mp4), manifest background scripts engine+redirects+background, background.js standIn() (type-compatible only, flag 4 of the cfg answer = Shields up AND Settings 'Fix ad and anti-adblock scripts'; Diagnostics counts them), ShieldsBridge.pageConfig flag 4; web-tests 214/214 (redirects.test.js 18 in real Chromium, background +4), core suite green except an unrelated timing-flaky ProxyLinkTest (passes alone); RuleParserTest stale expectation fixed; owner repeated message 37 at 15:04Z (TASKS AW1i)

## Do this next
1) bash scripts/make-default-lists.sh (uAssets checkout is at ../ublockorigin/uassets) to regenerate app/src/main/assets/filters/default.kfs WITH the redirect words, then ./gradlew :core:test --tests '*DefaultListsRegressionTest*' and RealListsExtImageTest + web-tests/real-diff.js (engine.js vs Kotlin on the real lists, every verdict equal); 2) emulator: start AVD kestrel29x64 (emulator -avd kestrel29x64 -no-window -no-audio -no-snapshot -gpu swiftshader_indirect -no-accel -memory 2048 &, wait for boot), build -PgeckoAbi=x86_64, install, extend tools/sitelab/adlab.py + adlab-rules.txt with redirect checks (script noopjs onload fires, image 1x1 naturalWidth 1, xhr noopjson status 200, media noopmp4 loadedmetadata, frame noopframe load, IMA flow -> adError 1009) and prove data: redirects work in Gecko 160 for script/image/xhr/media/frame (if a type fails, cancel is the fallback: restrict that type in redirects.js); 3) then AW1d network entity domains ($domain=site.* in Kotlin FilterSet + engine.js + differential corpus), AW1e video-ad pack, AW1f safety, AW1g Diagnostics, AW1h ship; heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN is disabled

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  8085098 ckpt 2130: AW1b DONE and EMULATOR-PROVEN: early delivery works (lab adlab.test vs contro
  3529f3d ckpt 2127: AW1b+ early scriptlet delivery BUILT: lab proved the late race (calls ran 100
  26045f2 ckpt 2103: AW1b step 2 DONE: scriptlets-main.js (our own library, 28 scriptlets) + web-t
  476faa3 ckpt 2095: AW1b step 1 DONE (core): ScriptletRules (uBO argument syntax, 23 canonical na
  d580e95 ckpt 2088: AW1a done: blocker mapped and the plan written to research/ADBLOCK_ENGINE.md 
  cdd4e9d ckpt 2086: owner message 37 written into TASKS (AW1 finish the ad blocking: scriptlets, 
  aba038a ckpt 2083: heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN DISABLED (update_trigger enabled=fals
  5375332 ckpt 2081: alpha36 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha36 (gecko code 137), run
  104a302 ckpt 2075: alpha36 DISPATCHED: android.yml run 43563546964 (run_number 87, app=gecko ful
  edcc148 ckpt 2074: gated kestrel-g-v0.3.0-alpha36 (gecko code 137) and pushed it: handoff self-t
```

(17 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
