# CHECKPOINT 1246 — read me first, then TASKS.md

**Written:** 2026-11-12T22:36:19Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `d385662` (this checkpoint is the commit after it)

## Just done
alpha24 content finished and tested: keyboard-reveal fix (content.js), D2 lazy generic cosmetics (+toCss fixes), Back up & restore (core codec, BackupManager, BackupFlow, Settings UI, both activities); backup tests now clean up Prefs/Shields singletons so :app:testDebugUnitTest is green again; geckoapp versionCode 123 / 0.3.0-alpha24, extension 1.0.123

## Do this next
SHIP alpha24: 1) bash ship.sh gecko "alpha24: Back up & restore, much deeper ad hiding, and a keyboard fix. ..." (note text is in TASKS.md AA3-7/BUILDLOG draft); 2) dispatch android.yml via mcp__github__actions_run_trigger ref=ccr-a57c163e-mtq8js app=gecko full_build=true note=same; 3) wait for tag kestrel-g-v0.3.0-alpha24 (git ls-remote loop) and a green run; 4) get_release_by_tag, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha24 note; 5) reply to the owner in plain words with the Release link on its own line. Then AA3-3b / AA3-4 / AA3-5 / AA3-6 / AA3-9. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2e3e341 ckpt 1238: AA3-7 Back up/Restore, part 1 done and tested: core BackupCodec (strict untru
  b630671 ckpt 1227: AA3-3 D2 DONE and verified: all ~29,204 generic cosmetic rules now apply (was
  15b71c1 ckpt 1196: AA3-2 typing/keyboard: closed the 'keyboard resizes the page' hypothesis on G
  81f5a25 ckpt 1187: SHIPPED + RECORDED: Kestrel G 0.3.0-alpha23 (code 122), tag kestrel-g-v0.3.0-
  05062dd ckpt 1185: alpha23 CI run 43305054932 failed in test-jvm: BrowserFlowTest.newTabPageWarn
  65bbb1c ckpt 1183: gated kestrel-g-v0.3.0-alpha23 (gecko code 122) and pushed it: handoff self-t
  8e8c9bc ckpt 1176: AB3 code done + unit-tested: NetProfile (core) now has isFastTunnel (10.111.2
  3a65bfe ckpt 1151: AB2 verified on the Android 10 emulator: long press opens the new Save video 
  d9db0b5 ckpt 1139: AB1 done: ONE master switch for the phone helper: PhoneBridge.setMaster/maste
  98cbc10 ckpt 1135: Owner message 15 written into TASKS.md as AB (AB1 one master switch for the p
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
