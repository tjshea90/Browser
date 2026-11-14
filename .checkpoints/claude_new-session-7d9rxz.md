# CHECKPOINT 2351 — read me first, then TASKS.md

**Written:** 2026-11-14T22:51:24Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `e170a13c` (this checkpoint is the commit after it)

## Just done
RX3-G7 DONE and ticked (full gates green: JVM + web 331/331; emulator real-engine proof; two defects found and fixed); RX3-V1 core logic done and green (VideoRole, MediaList/MediaRank decoy rules, 12 new tests); JS facts + Kotlin plumbing still to do

## Do this next
HEARTBEAT trig_01KEDB7Qxd8zFeawjGcAkSQZ (every 2 h; get_trigger to read; delete_trigger when the RX3 job is shipped). alpha41 is released+recorded+answered. RX3-G7 is DONE and ticked (emulator-proven, see research/YOUTUBE_ADS.md 6.4/6.5); the app is now at versionCode 143 / 0.3.0-alpha42 (manifest 1.0.143), NOT yet shipped. NOW: RX3-V1 (claimed [~], design in research/FINDER_LAB.md section 1): core DONE and green (VideoRole.kt VideoFacts/VideoRoleRules, MediaCandidate.role/interest/decoy, MediaList.applyInfo(..., role) + usableCount excludes decoys + value() evicts decoys first + usableUnsorted no exemption for decoys, MediaRank decoys -> other and excluded from ref/biggest; tests VideoRoleTest, MediaListTest +7, MediaRankTest +4 pass). STILL TO DO for V1: (1) content.js report() sends the facts: f bits (muted 1 loop 2 autoplay 4 controls 8 visible 16 ad 32; controls = native attribute OR a known player root ancestor: jwplayer video-js plyr mejs__container flowplayer shaka-video-container html5-video-player), rw rh (getBoundingClientRect rounded), vp (percent of the box inside the viewport), ad = an ancestor within 8 levels (never body) whose class or id split at non-alphanumerics holds ad ads advert advertisement adslot or an ad-state class (jw-flag-ads vjs-ad-playing ima-ad-container plyr__ads ad-showing ad-interrupting); put the web test in a NEW file web-tests/videofacts.test.js (content.test.js is near the 60 s limit); (2) background.js relays f rw rh vp (clamped ints) in the vid message; (3) Kotlin: ShieldsBridge.onVid builds VideoFacts.fromBits when m.has("f"), mediaInfo callback + VideoFlow.info(page,url,w,h,d,label,facts) call applyInfo(..., VideoRoleRules.decide(facts, durationMs)); BrowserActivity.kt line ~385 lambda; (4) a lab page set /hover-previews /hero-loop /preroll-main in tools/sitelab/vidlab_srv.py (EY-21) and an emulator check if cheap; (5) tick with bash tools/claim.sh --done RX3-V1 "<proof>". THEN Wave B in order: V2 V5 V3 D1 G4 G5 G6 S1 B2 (research/REVIEW-EXTERNAL-3.md sections 5-8), claim each, failing test first, ckpt after every step. alpha42 ships (bash ship.sh gecko "<note>", dispatch android.yml app=gecko full_build=true ref claude/new-session-7d9rxz, wait, tools/record-release.sh with the identical note) when Wave A is done and V1/V2 are in; the Health line 'ads report-in' in BrowserActivity may stay (it holds no host).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  265fcf6e ckpt 2350: RX3-G7 other-players ad skipper coded and tested (15 mock tests + manifest/r
  69fcfcf1 ckpt 2349: RX3-Y3 live YouTube check recorded (research/YOUTUBE_ADS.md section 5, tools
  d10a5f4a ckpt 2348: alpha41 RE-DISPATCHED (second run; the first failed on the flaky FLK1 test a
  ff61cb61 ckpt 2347: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
  79518152 ckpt 2346: first alpha41 run FAILED on GitHub: core ProxyLinkTest.theRenewalKeepsItOpen
  d6379d16 ckpt 2345: alpha41 (gecko code 142, manifest 1.0.142) GATED, PUSHED and the android.yml
  836d9546 ckpt 2344: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
  14a6a198 ckpt 2343: RX3-G1 DONE (guard+library declared in the manifest, world MAIN; emulator-pr
  e462351b ckpt 2342: RX3-G3 DONE (script side): the cross-frame postMessage assist channel is gon
  279f3b45 ckpt 2341: RX3-BK1/BK2 first emulator proof (static case): page left at y=1800, Back re
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
