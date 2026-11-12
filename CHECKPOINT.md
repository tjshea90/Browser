# CHECKPOINT 1248 — read me first, then TASKS.md

**Written:** 2026-11-12T22:38:43Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `3f19341` (this checkpoint is the commit after it)

## Just done
alpha24 gated and pushed (ckpt 1247), workflow DISPATCHED: run 43321730068 (#74, in progress, started 22:38Z, head 3f19341)

## Do this next
WAIT for tag kestrel-g-v0.3.0-alpha24 (git ls-remote origin refs/tags/kestrel-g-v0.3.0-alpha24, 10 s loop; build takes ~12-18 min for gecko), then mcp__github__actions_get get_workflow_run 43321730068 must say conclusion success; mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha24 must list the APK; ONLY THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha24 '<alpha24 note in the ckpt 1247 commit message>'; then reply to the owner in plain words with the Release link on its own line (what is new: Back up & restore at Menu > Settings > Back up & restore, deeper ad hiding, keyboard fix; emulator-only honesty). If the run is red: mcp__github__get_job_logs on the failed job, fix, bump to code 124/alpha25 only if a tag was already created, else re-dispatch. Never ship the tag twice. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  3f19341 ckpt 1247: gated kestrel-g-v0.3.0-alpha24 (gecko code 123) and pushed it: handoff self-t
  6b43626 ckpt 1246: alpha24 content finished and tested: keyboard-reveal fix (content.js), D2 laz
  2e3e341 ckpt 1238: AA3-7 Back up/Restore, part 1 done and tested: core BackupCodec (strict untru
  b630671 ckpt 1227: AA3-3 D2 DONE and verified: all ~29,204 generic cosmetic rules now apply (was
  15b71c1 ckpt 1196: AA3-2 typing/keyboard: closed the 'keyboard resizes the page' hypothesis on G
  81f5a25 ckpt 1187: SHIPPED + RECORDED: Kestrel G 0.3.0-alpha23 (code 122), tag kestrel-g-v0.3.0-
  05062dd ckpt 1185: alpha23 CI run 43305054932 failed in test-jvm: BrowserFlowTest.newTabPageWarn
  65bbb1c ckpt 1183: gated kestrel-g-v0.3.0-alpha23 (gecko code 122) and pushed it: handoff self-t
  8e8c9bc ckpt 1176: AB3 code done + unit-tested: NetProfile (core) now has isFastTunnel (10.111.2
  3a65bfe ckpt 1151: AB2 verified on the Android 10 emulator: long press opens the new Save video 
```
