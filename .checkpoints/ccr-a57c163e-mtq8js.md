# CHECKPOINT 2005 — read me first, then TASKS.md

**Written:** 2026-11-14T04:23:16Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `072c85d` (this checkpoint is the commit after it)

## Just done
alpha35 dispatched: android.yml queued for ref ccr-a57c163e-mtq8js (app=gecko full_build=true) at 04:23Z; versionCode 136

## Do this next
wait for the run (about 5 min): mcp__github__actions_list list_workflow_runs resource_id android.yml workflow_runs_filter event=workflow_dispatch perPage 1 -> when completed+success: mcp__github__get_release_by_tag tag kestrel-g-v0.3.0-alpha35 (APK attached?) -> bash tools/record-release.sh kestrel-g-v0.3.0-alpha35 "$(cat scratchpad/alpha35_note.txt)" -> answer the owner (messages 34-35) with the Release link on its own line; heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays until 24 h of owner silence

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  072c85d ckpt 2004: gated kestrel-g-v0.3.0-alpha35 (gecko code 136) and pushed it: handoff self-t
  90d1fd1 ckpt 2002: version bump 136 / 0.3.0-alpha35 for the release of AU1-AU3
  21e5616 ckpt 1998: AU1+AU2+AU3 verified on the emulator (corner long press, page-field typing: l
  95be9ed ckpt 1994: AU3 verified on the JVM side: core 876/877 (the one failure is a download tim
  85e6e7a ckpt 1990: AU3 code: TextEdit (core, 7 tests), PageField interface + MiniNav field mode 
  4ad5eb4 ckpt 1979: AU3 research written (research/MINI_NAV.md): GeckoView 160 input path read fr
  543c7ef ckpt 1978: AU2 learning keyboard: PersonalModel tests (11) + MiniKeyboard typedPairs tes
  a2b40ea ckpt 1965: alpha34 SHIPPED AND RECORDED (run 43549663530 green, Release kestrel-g-v0.3.0
  937563e ckpt 1958: Owner message 35 written into TASKS (AU1 invisible long-press corner instead 
  7898087 ckpt 1954: alpha34 (gecko code 135) DISPATCHED: android.yml run 43549663530 (run #85, st
```
