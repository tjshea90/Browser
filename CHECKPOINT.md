# CHECKPOINT 2180 — read me first, then TASKS.md

**Written:** 2026-11-14T16:08:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `551bd7e` (this checkpoint is the commit after it)

## Just done
alpha37 DISPATCHED: android.yml run 43613953896 (run_number 88, app=gecko full_build=true, ref ccr-a57c163e-mtq8js, head 551bd7e): builds, signs, verifies the certificate, creates tag kestrel-g-v0.3.0-alpha37 and the Release (about 9 minutes like run 87)

## Do this next
WAIT for the run (background loop: git ls-remote --tags origin until kestrel-g-v0.3.0-alpha37 appears, then mcp__github__actions_get get_workflow_run 43613953896 until completed/success; get_job_logs answers 404 until the job ends), then mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha37 (APK asset present), then bash tools/record-release.sh kestrel-g-v0.3.0-alpha37 with the SAME note as in the ship output / CHECKPOINT (the alpha37 note text starting 'alpha37 (includes alpha36): Better ad blocking.'), then bash tools/claim.sh AW1h --done and AW1i --done with the run id, then answer the owner in plain words and list the open items; NEVER ship alpha37 twice; heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN is disabled

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  551bd7e ckpt 2179: gated kestrel-g-v0.3.0-alpha37 (gecko code 138) and pushed it: handoff self-t
  ec2ea4b ckpt 2178: pre-ship: alpha37 (includes alpha36): Better ad blocking. 1) A site now gets 
  4bd8519 ckpt 2177: AW1d DONE (network $domain=site.* entity domains in Kotlin + engine.js, diffe
  7e29314 ckpt 2165: AW1e BUILT and EMULATOR-PROVEN (Gecko 160 lab): trusted-replace-fetch-respons
  7cfc54d ckpt 2154: AW1c DONE and EMULATOR-PROVEN (Gecko 160): data: stand-ins for script/image/x
  41441d2 ckpt 2148: AW1c BUILT (not yet emulator-proven): engine.js mirrors FilterSet.check for $
  8085098 ckpt 2130: AW1b DONE and EMULATOR-PROVEN: early delivery works (lab adlab.test vs contro
  3529f3d ckpt 2127: AW1b+ early scriptlet delivery BUILT: lab proved the late race (calls ran 100
  26045f2 ckpt 2103: AW1b step 2 DONE: scriptlets-main.js (our own library, 28 scriptlets) + web-t
  476faa3 ckpt 2095: AW1b step 1 DONE (core): ScriptletRules (uBO argument syntax, 23 canonical na
```
