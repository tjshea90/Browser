# CHECKPOINT 2130 — read me first, then TASKS.md

**Written:** 2026-11-14T07:35:15Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `3e3f9e5` (this checkpoint is the commit after it)

## Just done
AW1b DONE and EMULATOR-PROVEN: early delivery works (lab adlab.test vs control.test: wall hidden/SHOWN, nag not shown/SHOWN, countdown 407 ms/8036 ms, bait did not run/RAN, player adPlacements 0/2, pop-under listener never ran/RAN after taps, entity applied/no, banner hidden/visible; Diagnostics 'scriptlets on, 4 pages ran 36 calls, 3 before the page first script, 1 sites set up early'); fix found on the emulator: explicit http://host/* + https://host/* patterns (the * scheme was refused); lab needle fixed (nagBox); research/ADBLOCK_ENGINE.md 14.1 has the table + limits; AW1a and AW1b ticked in TASKS

## Do this next
AW1c: port the redirect logic to geckoapp/src/main/assets/kestrel-shields/engine.js (cRedirect / cRedirectOnly, verdict | (id << 8), mirror FilterSet.check exactly), add redirect rules to the ExtImageTest mini set and regenerate the fixtures in web-tests/fixtures (mini.kex, mini.corpus.tsv, mini.redirects.tsv) then run engine.test.js + real-diff.js + RealListsExtImageTest; then redirects.js id table (core RedirectResources.FILES ids 1-22) + res/ stand-in files (own stubs; ffmpeg exists for mp3/mp4) + manifest web_accessible_resources + background.js decide() returning redirectUrl moz-extension://.../res/<file> with a request-type compatibility check; regenerate default.kfs (scripts/make-default-lists.sh needs the uAssets checkout next to the repo) ; heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN is disabled

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  3529f3d ckpt 2127: AW1b+ early scriptlet delivery BUILT: lab proved the late race (calls ran 100
  26045f2 ckpt 2103: AW1b step 2 DONE: scriptlets-main.js (our own library, 28 scriptlets) + web-t
  476faa3 ckpt 2095: AW1b step 1 DONE (core): ScriptletRules (uBO argument syntax, 23 canonical na
  d580e95 ckpt 2088: AW1a done: blocker mapped and the plan written to research/ADBLOCK_ENGINE.md 
  cdd4e9d ckpt 2086: owner message 37 written into TASKS (AW1 finish the ad blocking: scriptlets, 
  aba038a ckpt 2083: heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN DISABLED (update_trigger enabled=fals
  5375332 ckpt 2081: alpha36 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha36 (gecko code 137), run
  104a302 ckpt 2075: alpha36 DISPATCHED: android.yml run 43563546964 (run_number 87, app=gecko ful
  edcc148 ckpt 2074: gated kestrel-g-v0.3.0-alpha36 (gecko code 137) and pushed it: handoff self-t
  d44b251 ckpt 2073: version bump 137 / 0.3.0-alpha36 for the release of AV1-AV3 (resize/detach, l
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
