# CHECKPOINT 2200 — read me first, then TASKS.md

**Written:** 2026-11-14T16:34:39Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `b72f9b2` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha38 (gecko code 139) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha38 (includes alpha37): Fixes the app closing on some bank pages. Pages such as the Ally Invest help pages (www.ally.com/help/invest/dividend-reinvestment/) ask the browser whether passkeys (signing in with a fingerprint or the screen lock) are available. Firefox's passkey code needs Google services, which this tablet does not have, so the whole app closed. Passkeys are now switched off inside the engine: the page simply does not see them and offers the password. I made the app close on the emulator the same way and checked that it no longer closes there; not tried on your tablet or on the real Ally page. Also new: if the app ever closes by itself again, the next Menu > Diagnostics starts with a line EARLIER CRASH that says what it was (and lists the engine's own crash reports if it left any): please paste it."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha38), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha38 "alpha38 (includes alpha37): Fixes the app closing on some bank pages. Pages such as the Ally Invest help pages (www.ally.com/help/invest/dividend-reinvestment/) ask the browser whether passkeys (signing in with a fingerprint or the screen lock) are available. Firefox's passkey code needs Google services, which this tablet does not have, so the whole app closed. Passkeys are now switched off inside the engine: the page simply does not see them and offers the password. I made the app close on the emulator the same way and checked that it no longer closes there; not tried on your tablet or on the real Ally page. Also new: if the app ever closes by itself again, the next Menu > Diagnostics starts with a line EARLIER CRASH that says what it was (and lists the engine's own crash reports if it left any): please paste it."
NEVER ship kestrel-g-v0.3.0-alpha38 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  b72f9b2 ckpt 2199: pre-ship: alpha38 (includes alpha37): Fixes the app closing on some bank page
  3495b5b ckpt 2198: AX2 root cause found, reproduced and fixed: GeckoView's Android WebAuthn need
  ac84c28 ckpt 2190: owner message 38 written into TASKS (AX1 keyboard learns sloppy glide pattern
  3d38f35 ckpt 2185: alpha37 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha37 (gecko code 138), and
  aef75f4 ckpt 2180: alpha37 DISPATCHED: android.yml run 43613953896 (run_number 88, app=gecko ful
  551bd7e ckpt 2179: gated kestrel-g-v0.3.0-alpha37 (gecko code 138) and pushed it: handoff self-t
  ec2ea4b ckpt 2178: pre-ship: alpha37 (includes alpha36): Better ad blocking. 1) A site now gets 
  4bd8519 ckpt 2177: AW1d DONE (network $domain=site.* entity domains in Kotlin + engine.js, diffe
  7e29314 ckpt 2165: AW1e BUILT and EMULATOR-PROVEN (Gecko 160 lab): trusted-replace-fetch-respons
  7cfc54d ckpt 2154: AW1c DONE and EMULATOR-PROVEN (Gecko 160): data: stand-ins for script/image/x
```
