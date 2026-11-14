# CHECKPOINT 2190 — read me first, then TASKS.md

**Written:** 2026-11-14T16:18:59Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `61de143` (this checkpoint is the commit after it)

## Just done
owner message 38 written into TASKS (AX1 keyboard learns sloppy glide patterns + corrections + exportable file for a future system keyboard; AX2 the app closes while loading www.ally.com/help/invest/dividend-reinvestment/); alpha37 was answered; nothing else in flight

## Do this next
AX2a first (a crash closes the whole app): try to fetch the page (WebFetch / web-fetch agent) to see what it loads; read TabDelegates.onCrash/onKill + BrowserActivity.2053 (content process death) and look for an uncaught Throwable path from the Shields bridge (ShieldsBridge.onMessage catches Exception only: OutOfMemoryError/StackOverflowError escape), the alpha37 additions (sc message, per-host contentScripts, data: redirects) and the Health log; add a crash note written by Thread.setDefaultUncaughtExceptionHandler into files/ + Diagnostics line; then AX1a-f (research/GLIDE_TYPING.md section 11, GlideDecoder, PersonalModel, MiniNav, export format docs/KEYBOARD-EXPORT.md); heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN is disabled

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  3d38f35 ckpt 2185: alpha37 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha37 (gecko code 138), and
  aef75f4 ckpt 2180: alpha37 DISPATCHED: android.yml run 43613953896 (run_number 88, app=gecko ful
  551bd7e ckpt 2179: gated kestrel-g-v0.3.0-alpha37 (gecko code 138) and pushed it: handoff self-t
  ec2ea4b ckpt 2178: pre-ship: alpha37 (includes alpha36): Better ad blocking. 1) A site now gets 
  4bd8519 ckpt 2177: AW1d DONE (network $domain=site.* entity domains in Kotlin + engine.js, diffe
  7e29314 ckpt 2165: AW1e BUILT and EMULATOR-PROVEN (Gecko 160 lab): trusted-replace-fetch-respons
  7cfc54d ckpt 2154: AW1c DONE and EMULATOR-PROVEN (Gecko 160): data: stand-ins for script/image/x
  41441d2 ckpt 2148: AW1c BUILT (not yet emulator-proven): engine.js mirrors FilterSet.check for $
  8085098 ckpt 2130: AW1b DONE and EMULATOR-PROVEN: early delivery works (lab adlab.test vs contro
  3529f3d ckpt 2127: AW1b+ early scriptlet delivery BUILT: lab proved the late race (calls ran 100
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
