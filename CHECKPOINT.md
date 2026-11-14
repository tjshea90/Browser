# CHECKPOINT 2198 — read me first, then TASKS.md

**Written:** 2026-11-14T16:32:20Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `0da6df3` (this checkpoint is the commit after it)

## Just done
AX2 root cause found, reproduced and fixed: GeckoView's Android WebAuthn needs Google Play services (NoClassDefFoundError WebAuthnTokenManager.<clinit>) so a secure page that only ASKS about passkeys (a bank login widget) closed the whole app; security.webauth.webauthn=false in gecko-prefs.txt (config v8, check-gecko-prefs passes, Diagnostics read-back), emulator before/after on http://localhost:8001/wa; CrashNote.kt (uncaught-exception note + engine crash report lines in Diagnostics, 5 tests incl. a pin that the pref stays false); research/CRASH_PASSKEYS.md; not yet released

## Do this next
AX2b: run the full suites + lint, then fold AX2 into the alpha38 release together with AX1 (keyboard): either ship AX2 alone first (small, urgent: the owner's app closes) then AX1 - DECISION: ship AX2 alone as alpha38 right after AX1a's research is written? No: ship AX2 first as alpha38 (versionCode 139) as soon as the suites pass, AX1 follows as alpha39; for AX1 start with AX1a (read GlideDecoder / PersonalModel / MiniNav learning, build the sloppy for/fir case in the simulator, measure corrections needed, research/GLIDE_TYPING.md section 11); heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN is disabled

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ac84c28 ckpt 2190: owner message 38 written into TASKS (AX1 keyboard learns sloppy glide pattern
  3d38f35 ckpt 2185: alpha37 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha37 (gecko code 138), and
  aef75f4 ckpt 2180: alpha37 DISPATCHED: android.yml run 43613953896 (run_number 88, app=gecko ful
  551bd7e ckpt 2179: gated kestrel-g-v0.3.0-alpha37 (gecko code 138) and pushed it: handoff self-t
  ec2ea4b ckpt 2178: pre-ship: alpha37 (includes alpha36): Better ad blocking. 1) A site now gets 
  4bd8519 ckpt 2177: AW1d DONE (network $domain=site.* entity domains in Kotlin + engine.js, diffe
  7e29314 ckpt 2165: AW1e BUILT and EMULATOR-PROVEN (Gecko 160 lab): trusted-replace-fetch-respons
  7cfc54d ckpt 2154: AW1c DONE and EMULATOR-PROVEN (Gecko 160): data: stand-ins for script/image/x
  41441d2 ckpt 2148: AW1c BUILT (not yet emulator-proven): engine.js mirrors FilterSet.check for $
  8085098 ckpt 2130: AW1b DONE and EMULATOR-PROVEN: early delivery works (lab adlab.test vs contro
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
