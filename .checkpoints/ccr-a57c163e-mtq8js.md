# CHECKPOINT 2095 — read me first, then TASKS.md

**Written:** 2026-11-14T06:35:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `4cfc42e` (this checkpoint is the commit after it)

## Just done
AW1b step 1 DONE (core): ScriptletRules (uBO argument syntax, 23 canonical names + aliases, refuse calls that would act on everything, limits), RuleParser accepts +js() for domain rules, #@#+js() / #@#+js(), ~exclusions as exceptions and ENTITY domains (site.*) for ALL cosmetic kinds, CosmeticIndex.hostKeys adds entity keys via Psl.siteOf, cssFor(scriptlets=true) puts /*ks[json]*/ after /*kg*//*kp*/, FilterSet.scriptletsFor; real lists: 12,128 of 12,859 scriptlet lines accepted (1,639 entity), snapshot build 12,095 scriptlet rules; ScriptletRulesTest 12 + RealScriptletRulesTest 2 + whole filter package green

## Do this next
AW1b step 2: geckoapp/src/main/assets/kestrel-shields/scriptlets-main.js = the page-world library (own code: set, acs, aopr, aopw, aost, aeld, nostif, nosiif, nano-stb/sib, nowoif, noeval(-if), no-fetch-if, no-xhr-if, json-prune(+fetch/xhr response), ra, rc, rmnt, rpnt, nowebrtc, set-cookie, remove-cookie, set-local-storage-item, set-session-storage-item, trusted-set-cookie, trusted-set-local-storage-item) listening for the __kst_sl_v1 event, + web-tests/scriptlets.test.js in headless Chromium; then content.js (cut /*ks..*/ out of the css, dispatch the event, READY handshake), background.js (contentScripts.register MAIN), ShieldsBridge (scriptlets=Prefs flag), Settings switch, Diagnostics counters

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d580e95 ckpt 2088: AW1a done: blocker mapped and the plan written to research/ADBLOCK_ENGINE.md 
  cdd4e9d ckpt 2086: owner message 37 written into TASKS (AW1 finish the ad blocking: scriptlets, 
  aba038a ckpt 2083: heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN DISABLED (update_trigger enabled=fals
  5375332 ckpt 2081: alpha36 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha36 (gecko code 137), run
  104a302 ckpt 2075: alpha36 DISPATCHED: android.yml run 43563546964 (run_number 87, app=gecko ful
  edcc148 ckpt 2074: gated kestrel-g-v0.3.0-alpha36 (gecko code 137) and pushed it: handoff self-t
  d44b251 ckpt 2073: version bump 137 / 0.3.0-alpha36 for the release of AV1-AV3 (resize/detach, l
  eac3369 ckpt 2061: AV3 EMULATOR-PROVEN (page-field row, live guess mid-glide her|he|hd, held dig
  c5c3d98 ckpt 2057: AV2b done: repeated-letter words ('awwww') never learned (PersonalModel.stret
  39b545b ckpt 2047: AV2a research REPORT saved to research/GLIDE_RESEARCH.md (Gboard/Samsung/Swif
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
