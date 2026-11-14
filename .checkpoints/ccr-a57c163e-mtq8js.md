# CHECKPOINT 2199 — read me first, then TASKS.md

**Written:** 2026-11-14T16:33:23Z · **tests:** 1 RED: test_handoff.sh (0 green)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `3495b5b` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha38 (includes alpha37): Fixes the app closing on some bank pages. Pages such as the Ally Invest help pages (www.ally.com/help/invest/dividend-reinvestment/) ask the browser whether passkeys (signing in with a fingerprint or the screen lock) are available. Firefox's passkey code needs Google services, which this tablet does not have, so the whole app closed. Passkeys are now switched off inside the engine: the page simply does not see them and offers the password. I made the app close on the emulator the same way and checked that it no longer closes there; not tried on your tablet or on the real Ally page. Also new: if the app ever closes by itself again, the next Menu > Diagnostics starts with a line EARLIER CRASH that says what it was (and lists the engine's own crash reports if it left any): please paste it.

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/build.gradle.kts

## Last ten checkpoints
```
  3495b5b ckpt 2198: AX2 root cause found, reproduced and fixed: GeckoView's Android WebAuthn need
  ac84c28 ckpt 2190: owner message 38 written into TASKS (AX1 keyboard learns sloppy glide pattern
  3d38f35 ckpt 2185: alpha37 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha37 (gecko code 138), and
  aef75f4 ckpt 2180: alpha37 DISPATCHED: android.yml run 43613953896 (run_number 88, app=gecko ful
  551bd7e ckpt 2179: gated kestrel-g-v0.3.0-alpha37 (gecko code 138) and pushed it: handoff self-t
  ec2ea4b ckpt 2178: pre-ship: alpha37 (includes alpha36): Better ad blocking. 1) A site now gets 
  4bd8519 ckpt 2177: AW1d DONE (network $domain=site.* entity domains in Kotlin + engine.js, diffe
  7e29314 ckpt 2165: AW1e BUILT and EMULATOR-PROVEN (Gecko 160 lab): trusted-replace-fetch-respons
  7cfc54d ckpt 2154: AW1c DONE and EMULATOR-PROVEN (Gecko 160): data: stand-ins for script/image/x
  41441d2 ckpt 2148: AW1c BUILT (not yet emulator-proven): engine.js mirrors FilterSet.check for $
```
