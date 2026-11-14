# CHECKPOINT 2201 — read me first, then TASKS.md

**Written:** 2026-11-14T16:35:05Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `da5124b` (this checkpoint is the commit after it)

## Just done
alpha38 DISPATCHED (AX2 fix: passkey API off + crash note): android.yml run_workflow queued at 16:34Z, app=gecko full_build=true, ref ccr-a57c163e-mtq8js; find the run id with mcp__github__actions_list list_workflow_runs android.yml branch ccr-a57c163e-mtq8js event workflow_dispatch perPage 1 (the answer is long: it echoes the commit message); builds, signs, creates tag kestrel-g-v0.3.0-alpha38 (about 9 minutes)

## Do this next
WAIT: git ls-remote --tags origin until kestrel-g-v0.3.0-alpha38 appears, then list_workflow_jobs for the run until build-gecko is success, mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha38 (APK present), then bash tools/record-release.sh kestrel-g-v0.3.0-alpha38 with the SAME note (see ship2.log / the CHECKPOINT of ckpt 2199+: 'alpha38 (includes alpha37): Fixes the app closing on some bank pages...'), claim.sh AX2b --done, answer the owner (AX2 plain words + say AX1 keyboard learning is next); meanwhile AX1a research (GlideDecoder / PersonalModel / MiniNav, research/GLIDE_TYPING.md section 11); NEVER ship alpha38 twice; heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN is disabled

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  da5124b ckpt 2200: gated kestrel-g-v0.3.0-alpha38 (gecko code 139) and pushed it: handoff self-t
  b72f9b2 ckpt 2199: pre-ship: alpha38 (includes alpha37): Fixes the app closing on some bank page
  3495b5b ckpt 2198: AX2 root cause found, reproduced and fixed: GeckoView's Android WebAuthn need
  ac84c28 ckpt 2190: owner message 38 written into TASKS (AX1 keyboard learns sloppy glide pattern
  3d38f35 ckpt 2185: alpha37 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha37 (gecko code 138), and
  aef75f4 ckpt 2180: alpha37 DISPATCHED: android.yml run 43613953896 (run_number 88, app=gecko ful
  551bd7e ckpt 2179: gated kestrel-g-v0.3.0-alpha37 (gecko code 138) and pushed it: handoff self-t
  ec2ea4b ckpt 2178: pre-ship: alpha37 (includes alpha36): Better ad blocking. 1) A site now gets 
  4bd8519 ckpt 2177: AW1d DONE (network $domain=site.* entity domains in Kotlin + engine.js, diffe
  7e29314 ckpt 2165: AW1e BUILT and EMULATOR-PROVEN (Gecko 160 lab): trusted-replace-fetch-respons
```
