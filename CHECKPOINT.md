# CHECKPOINT 1257 — read me first, then TASKS.md

**Written:** 2026-11-12T22:41:34Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `27d5452` (this checkpoint is the commit after it)

## Just done
AA3-3b step 1 (after dispatching alpha24 run 43321730068): engine-image chunks now have their own worker thread kestrel-image (BridgeLane in core picks quick/cosmetic/image lane from the first bytes of the native message; 5 core tests; web-test pins that the extension writes the kind first); geckoapp compiles; NOT yet run on the emulator

## Do this next
1) alpha24: wait for tag kestrel-g-v0.3.0-alpha24 + green run 43321730068, get_release_by_tag, record-release.sh (note is in the ckpt 1247 commit message), reply to owner with the Release link. 2) AA3-3b next pieces in this order: $removeparam (core: parse option, strip in URL layer + tests), redirect= neutering (stub resources), procedural cosmetics (:has-text, :upward) in content.js, false-positive corpus for generic class rules. The kestrel-image change goes into alpha25.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  efdc3a2 ckpt 1248: alpha24 gated and pushed (ckpt 1247), workflow DISPATCHED: run 43321730068 (#
  3f19341 ckpt 1247: gated kestrel-g-v0.3.0-alpha24 (gecko code 123) and pushed it: handoff self-t
  6b43626 ckpt 1246: alpha24 content finished and tested: keyboard-reveal fix (content.js), D2 laz
  2e3e341 ckpt 1238: AA3-7 Back up/Restore, part 1 done and tested: core BackupCodec (strict untru
  b630671 ckpt 1227: AA3-3 D2 DONE and verified: all ~29,204 generic cosmetic rules now apply (was
  15b71c1 ckpt 1196: AA3-2 typing/keyboard: closed the 'keyboard resizes the page' hypothesis on G
  81f5a25 ckpt 1187: SHIPPED + RECORDED: Kestrel G 0.3.0-alpha23 (code 122), tag kestrel-g-v0.3.0-
  05062dd ckpt 1185: alpha23 CI run 43305054932 failed in test-jvm: BrowserFlowTest.newTabPageWarn
  65bbb1c ckpt 1183: gated kestrel-g-v0.3.0-alpha23 (gecko code 122) and pushed it: handoff self-t
  8e8c9bc ckpt 1176: AB3 code done + unit-tested: NetProfile (core) now has isFastTunnel (10.111.2
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
