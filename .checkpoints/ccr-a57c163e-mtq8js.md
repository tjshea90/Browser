# CHECKPOINT 2088 — read me first, then TASKS.md

**Written:** 2026-11-14T06:29:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `301b123` (this checkpoint is the commit after it)

## Just done
AW1a done: blocker mapped and the plan written to research/ADBLOCK_ENGINE.md section 14 (12,971 real scriptlet rules, 9,508 ad-related; storage in the cosmetic site groups, no format change; delivery in the css answer as /*ks[json]*/ + a page-world library scriptlets-main.js; decisions 1-9)

## Do this next
AW1b step 1: core/filter/ScriptletRules.kt (parse +js(name, args) with the uBO argument syntax: quotes, escaped commas, aliases, limits; stored form PREFIX 0x05 name 0x06 args), RuleParser.parseCosmetic accepts it for domain rules + #@#+js exceptions, CosmeticIndex.scriptletsFor(host)/cssFor adds the /*ks*/ marker+payload, CosmeticSelectors.toCss skips it; tests ScriptletRulesTest + RealScriptletRulesTest on /home/user/ublockorigin/uassets; then scriptlets-main.js + web-tests/scriptlets.test.js

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    M  research/ADBLOCK_ENGINE.md

## Last ten checkpoints
```
  cdd4e9d ckpt 2086: owner message 37 written into TASKS (AW1 finish the ad blocking: scriptlets, 
  aba038a ckpt 2083: heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN DISABLED (update_trigger enabled=fals
  5375332 ckpt 2081: alpha36 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha36 (gecko code 137), run
  104a302 ckpt 2075: alpha36 DISPATCHED: android.yml run 43563546964 (run_number 87, app=gecko ful
  edcc148 ckpt 2074: gated kestrel-g-v0.3.0-alpha36 (gecko code 137) and pushed it: handoff self-t
  d44b251 ckpt 2073: version bump 137 / 0.3.0-alpha36 for the release of AV1-AV3 (resize/detach, l
  eac3369 ckpt 2061: AV3 EMULATOR-PROVEN (page-field row, live guess mid-glide her|he|hd, held dig
  c5c3d98 ckpt 2057: AV2b done: repeated-letter words ('awwww') never learned (PersonalModel.stret
  39b545b ckpt 2047: AV2a research REPORT saved to research/GLIDE_RESEARCH.md (Gboard/Samsung/Swif
  b097785 ckpt 2044: AV3b: caps lock (double tap Shift, accent-filled key), page-field bottom row 
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
