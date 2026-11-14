# CHECKPOINT 2346 — read me first, then TASKS.md

**Written:** 2026-11-14T21:53:33Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `2d54bbb9` (this checkpoint is the commit after it)

## Just done
first alpha41 run FAILED on GitHub: core ProxyLinkTest.theRenewalKeepsItOpenPastTheFirstArming (the known flaky FLK1: two courier clients with one key, the phone's replay guard rejects an identical signature in the same ms) - fixed in the TEST (one shared client, 8 clean runs); nothing was published (no tag). RX3-Y2 skipper DONE on the mechanics: youtube.js (+ manifest entry, background guard answer carries skip, yts relay), Settings row 'Skip ads inside video players' default on, page-config flag 8, Diagnostics line 'YouTube ads', core AdSkipStats (7 tests), web-tests/youtube.test.js 14 pass on a mock player built from the LIVE markup (player and video verified on the real mobile and desktop site; ad class names / skip selectors NOT verified: the sandbox gets no ads). Local gates equal to CI pass: :core:test :tools:build :app:testDebugUnitTest :app:lintRelease, web suite 313/313

## Do this next
RE-SHIP alpha41 (same version, nothing was published): bash ship.sh gecko "<note incl. YouTube skipper>" (scratchpad note41b.txt), then dispatch android.yml (app=gecko full_build=true ref claude/new-session-7d9rxz) with the same note, WAIT for the tag kestrel-g-v0.3.0-alpha41 + conclusion success + get_release_by_tag APK, ONLY THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha41 "<note>"; answer the owner in plain words with the Release link. After that: RX3-Y3 (tools/sitelab/yt_live.js live check + selector table in research/YOUTUBE_ADS.md), RX3-G7 generic players (content.js: observe a player container class only after a video fired play; ad-state words jw-flag-ads vjs-ad-playing vjs-ad-loading ima-ad-container plyr--ad; never on the YouTube hosts), then Wave B: V1, V2, V5, D1, G4, G5, G6, S1, B2

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d6379d16 ckpt 2345: alpha41 (gecko code 142, manifest 1.0.142) GATED, PUSHED and the android.yml
  836d9546 ckpt 2344: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
  14a6a198 ckpt 2343: RX3-G1 DONE (guard+library declared in the manifest, world MAIN; emulator-pr
  e462351b ckpt 2342: RX3-G3 DONE (script side): the cross-frame postMessage assist channel is gon
  279f3b45 ckpt 2341: RX3-BK1/BK2 first emulator proof (static case): page left at y=1800, Back re
  8888c78b ckpt 2340: RX3-BK1/BK2 Kotlin wiring DONE and COMPILES (geckoapp + app debug): ShieldsB
  687414dd ckpt 2339: owner message 40 (a screenshot, no words: YouTube Movies and TV page 'Video 
  2d3b6a3a ckpt 2338: RX3-BK1/BK2 core logic DONE and green: core/session/BackScroll.kt (ScrollMem
  13adf44e ckpt 2334: saved research/REVIEW-EXTERNAL-3.md (v2, byte-identical copy of the owner's 
  094aed9b ckpt 2337: alpha40 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha40 (gecko code 141, run
```

(10 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
