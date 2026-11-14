# CHECKPOINT 2260 — read me first, then TASKS.md

**Written:** 2026-11-14T17:51:26Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `db0854d` (this checkpoint is the commit after it)

## Just done
alpha39 DISPATCHED (AX1: glide shapes + export/import): android.yml run_workflow queued at 17:51Z, app=gecko full_build=true, ref ccr-a57c163e-mtq8js, gates passed locally (handoff self-test, :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin, lint earlier); builds, signs, creates tag kestrel-g-v0.3.0-alpha39 (about 9 minutes). Emulator proof done (research/GLIDE_TYPING.md 11.7)

## Do this next
WAIT: git ls-remote --tags origin until kestrel-g-v0.3.0-alpha39 appears, then mcp__github__actions_list list_workflow_runs (android.yml, branch ccr-a57c163e-mtq8js, event workflow_dispatch, perPage 1) and list_workflow_jobs until build-gecko is success, mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha39 (APK present), then bash tools/record-release.sh kestrel-g-v0.3.0-alpha39 with the SAME note as in the CHECKPOINT of ckpt before this one / ship3 log ('alpha39 (includes alpha38): The keyboard now learns the SHAPE of your glides ...'), claim.sh AX1f --done, then ANSWER THE OWNER (message 38 part 1, plain words: what it learns, the for/fir reproduction and result, export/import where and how, a system keyboard needs a separate small app - ask yes/no, what was NOT run on the tablet; Release link on its own line; re-list open items). NEVER ship alpha39 twice

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  db0854d ckpt 2259: gated kestrel-g-v0.3.0-alpha39 (gecko code 140) and pushed it: handoff self-t
  4c766a1 ckpt 2258: pre-ship: alpha39 (includes alpha38): The keyboard now learns the SHAPE of yo
  899e135 ckpt 2244: AX1: remembered-word injection added (the lab found that for words seen 6+ ti
  54435a1 ckpt 2232: AX1 core built and tested: Fingerprint + PatternMemory (core/input/glide/Patt
  2383c78 ckpt 2206: alpha38 RELEASED AND RECORDED (run 43616372537 green, tag kestrel-g-v0.3.0-al
  2d5a35b ckpt 2201: alpha38 DISPATCHED (AX2 fix: passkey API off + crash note): android.yml run_w
  da5124b ckpt 2200: gated kestrel-g-v0.3.0-alpha38 (gecko code 139) and pushed it: handoff self-t
  b72f9b2 ckpt 2199: pre-ship: alpha38 (includes alpha37): Fixes the app closing on some bank page
  3495b5b ckpt 2198: AX2 root cause found, reproduced and fixed: GeckoView's Android WebAuthn need
  ac84c28 ckpt 2190: owner message 38 written into TASKS (AX1 keyboard learns sloppy glide pattern
```
