# CHECKPOINT 2347 — read me first, then TASKS.md

**Written:** 2026-11-14T21:53:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `79518152` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/new-session-7d9rxz, inputs {"app":"gecko","full_build":"true","note":"alpha41: Back remembers your place on each page and counts how Back behaves (Back lab); YouTube ad skipping (mutes and fast-forwards ads, presses Skip, closes banners, removes the ad-blocker box; one switch in Settings, on by default) and the lists' full YouTube rules; lists can only rewrite pages if they are the uBlock lists; a page can no longer fake your tap or click inside frames it embeds; the pop-up guard is there from the very first page. Tested on the emulator and in test browsers, not on your tablet."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha41), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha41 "alpha41: Back remembers your place on each page and counts how Back behaves (Back lab); YouTube ad skipping (mutes and fast-forwards ads, presses Skip, closes banners, removes the ad-blocker box; one switch in Settings, on by default) and the lists' full YouTube rules; lists can only rewrite pages if they are the uBlock lists; a page can no longer fake your tap or click inside frames it embeds; the pop-up guard is there from the very first page. Tested on the emulator and in test browsers, not on your tablet."
NEVER ship kestrel-g-v0.3.0-alpha41 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  79518152 ckpt 2346: first alpha41 run FAILED on GitHub: core ProxyLinkTest.theRenewalKeepsItOpen
  d6379d16 ckpt 2345: alpha41 (gecko code 142, manifest 1.0.142) GATED, PUSHED and the android.yml
  836d9546 ckpt 2344: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
  14a6a198 ckpt 2343: RX3-G1 DONE (guard+library declared in the manifest, world MAIN; emulator-pr
  e462351b ckpt 2342: RX3-G3 DONE (script side): the cross-frame postMessage assist channel is gon
  279f3b45 ckpt 2341: RX3-BK1/BK2 first emulator proof (static case): page left at y=1800, Back re
  8888c78b ckpt 2340: RX3-BK1/BK2 Kotlin wiring DONE and COMPILES (geckoapp + app debug): ShieldsB
  687414dd ckpt 2339: owner message 40 (a screenshot, no words: YouTube Movies and TV page 'Video 
  2d3b6a3a ckpt 2338: RX3-BK1/BK2 core logic DONE and green: core/session/BackScroll.kt (ScrollMem
  13adf44e ckpt 2334: saved research/REVIEW-EXTERNAL-3.md (v2, byte-identical copy of the owner's 
```
