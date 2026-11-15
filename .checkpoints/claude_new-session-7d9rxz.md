# CHECKPOINT 2360 — read me first, then TASKS.md

**Written:** 2026-11-15T00:54:32Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `ce08e51f` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha44 (gecko code 145) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/new-session-7d9rxz, inputs {"app":"gecko","full_build":"true","note":"alpha44: one tap now plays a video when an invisible advertising layer lies over it (the layer is switched off the moment your finger touches the player, and an ad frame hidden over a player is found too); a clip that plays on a page and is then replaced by a much longer film on the same player (a pre-roll) is recognised as an ad and no longer counts for the download list, and the file that really plays comes first in the download sheet; the watcher that helps hide ad boxes now stops after a short time budget, so a busy page cannot slow itself down. Tested on the emulator and in test browsers, not on your tablet."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha44), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha44 "alpha44: one tap now plays a video when an invisible advertising layer lies over it (the layer is switched off the moment your finger touches the player, and an ad frame hidden over a player is found too); a clip that plays on a page and is then replaced by a much longer film on the same player (a pre-roll) is recognised as an ad and no longer counts for the download list, and the file that really plays comes first in the download sheet; the watcher that helps hide ad boxes now stops after a short time budget, so a busy page cannot slow itself down. Tested on the emulator and in test browsers, not on your tablet."
NEVER ship kestrel-g-v0.3.0-alpha44 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  072024e2 ckpt 2359: alpha43 gated, pushed and dispatched to GitHub (V5, D1, S1, B2, G5, G6); wai
  cca67983 ckpt 2358: gated kestrel-g-v0.3.0-alpha43 (gecko code 144) and pushed it: handoff self-
  18b548ee ckpt 2357: pre-ship: alpha43: the glow stops pulsing while a video plays; two more trac
  b7d3f84a ckpt 2356: RX3-S1, B2, G5, G6 DONE and ticked (S1 + G6 emulator-proven, B2 measured wit
  822e687f ckpt 2355: RX3-V5 and RX3-D1 DONE (ticked): AdMarkers/AdRuns/early look and the outage 
  dec2df9d ckpt 2354: alpha42 shipped to GitHub (gated + pushed + android.yml dispatched): G7 + V1
  652f583c ckpt 2353: gated kestrel-g-v0.3.0-alpha42 (gecko code 143) and pushed it: handoff self-
  feda2b32 ckpt 2352: RX3-V1 DONE (ticked) and RX3-V2 coded + proven on the emulator (teaser-link 
  21cda298 ckpt 2351: RX3-G7 DONE and ticked (full gates green: JVM + web 331/331; emulator real-e
  265fcf6e ckpt 2350: RX3-G7 other-players ad skipper coded and tested (15 mock tests + manifest/r
```

(33 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
