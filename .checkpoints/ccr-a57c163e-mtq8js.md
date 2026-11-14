# CHECKPOINT 2177 — read me first, then TASKS.md

**Written:** 2026-11-14T16:05:58Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `75df685` (this checkpoint is the commit after it)

## Just done
AW1d DONE (network $domain=site.* entity domains in Kotlin + engine.js, differential 100% on the mini corpus and 67,655 real-list requests, ENTITY_DOMAIN skips 279 -> 37, emulator proof), AW1e/AW1f/AW1g ticked with evidence; DefaultListsRegressionTest has the new safety guards; full suites green (core all, app 291, lint 0 errors, web-tests 223/223); HANDOFF.md + research 14.3/14.4 written; default.kfs 6,643 KB

## Do this next
AW1h SHIP alpha37: bump geckoapp/build.gradle.kts versionCode 138 / versionName 0.3.0-alpha37, write the ASCII plain-words release note (what the owner sees: pop-up walls and countdowns fixed before the page runs, blocked ad files answered so players start, YouTube-style ad data rewritten, link-shortener links point at the target, entity-domain rules, Quick fixes list; the Settings switch Fix ad and anti-adblock scripts controls it; Diagnostics numbers; NOT run on the tablet or on real sites), bash ship.sh gecko "note" (gates: handoff self-test, ext manifest 1.0.138), dispatch android.yml (mcp__github__actions_run_trigger owner tjshea90 repo Browser ref ccr-a57c163e-mtq8js inputs app=gecko full_build=true note), wait for run + tag, get_release_by_tag, bash tools/record-release.sh <tag> note, claim.sh AW1h --done and AW1i --done, answer the owner in plain words (AW1i repeated message 37; re-list open items: Diagnostics paste for Glide typing + Mini keyboard in page fields, AI1c Doraby/Cloudflare wording + page report, choppy-video link, scroll/Back feel, Proton Mail numbers + keyboard test, scheduled GitHub ad-list build yes/no, private signing key SEC-1, network lab results/LIC-1/UI-3/P2b, AS1 stored libraries yes/no, AS2 per-process memory line); heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN is disabled

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  7e29314 ckpt 2165: AW1e BUILT and EMULATOR-PROVEN (Gecko 160 lab): trusted-replace-fetch-respons
  7cfc54d ckpt 2154: AW1c DONE and EMULATOR-PROVEN (Gecko 160): data: stand-ins for script/image/x
  41441d2 ckpt 2148: AW1c BUILT (not yet emulator-proven): engine.js mirrors FilterSet.check for $
  8085098 ckpt 2130: AW1b DONE and EMULATOR-PROVEN: early delivery works (lab adlab.test vs contro
  3529f3d ckpt 2127: AW1b+ early scriptlet delivery BUILT: lab proved the late race (calls ran 100
  26045f2 ckpt 2103: AW1b step 2 DONE: scriptlets-main.js (our own library, 28 scriptlets) + web-t
  476faa3 ckpt 2095: AW1b step 1 DONE (core): ScriptletRules (uBO argument syntax, 23 canonical na
  d580e95 ckpt 2088: AW1a done: blocker mapped and the plan written to research/ADBLOCK_ENGINE.md 
  cdd4e9d ckpt 2086: owner message 37 written into TASKS (AW1 finish the ad blocking: scriptlets, 
  aba038a ckpt 2083: heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN DISABLED (update_trigger enabled=fals
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
