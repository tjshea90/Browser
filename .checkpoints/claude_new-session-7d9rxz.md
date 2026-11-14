# CHECKPOINT 2352 — read me first, then TASKS.md

**Written:** 2026-11-14T23:12:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `d565b1e3` (this checkpoint is the commit after it)

## Just done
RX3-V1 DONE (ticked) and RX3-V2 coded + proven on the emulator (teaser-link lab: asked=2 counted=0 -> verified 683 KB counted=1, 29 KB not counted); JVM gates for V2 running

## Do this next
HEARTBEAT trig_01KEDB7Qxd8zFeawjGcAkSQZ. alpha42 (versionCode 143) NOT shipped yet. V1 ticked. V2 (claimed [~]): code done (MediaCandidate.fromMarkup/verified, MediaList.setLength/unverifiedToCheck/usableCount, MediaRank, ShieldsBridge sets fromMarkup for source dom, VideoFlow.verifySoon/verifyNow one-byte Range check) and emulator-proven (research/FINDER_LAB.md 2.1). NEXT: read the JVM gate result in the scratchpad file gates2-gradle.log (EXIT 0 expected; else run ./gradlew --offline -q :core:test :tools:build :app:testDebugUnitTest :geckoapp:testDebugUnitTest :app:lintRelease in the background and read the log), then bash tools/claim.sh RX3-V2 --done "<proof: core MediaListTest +5, MediaRankTest +2, emulator teaser-link, gates>"; THEN Wave B: V5 (core/media/AdRuns.kt + Hls CUE-OUT/CUE-IN/SCTE35/DATERANGE parsing + StreamEngine pre-seeded skipped + early playlist look; tests AdRunsTest StreamEngineTest; lab /hls-ad-run), V3 (content.js playing event + vid ev played after 3 s; app primary sort), D1 (HLS outage wait in StreamEngine/Engine), G4 G5 G6 (guard), S1 (glow pause during video), B2 (extra tracker lists with Settings switch). Claim each with bash tools/claim.sh RX3-<id>, failing test first, ckpt after each step. When V5 or the next row is done and the gates are green, SHIP alpha42: bash ship.sh gecko "<note>" then mcp__github__actions_run_trigger run_workflow android.yml ref claude/new-session-7d9rxz inputs app=gecko full_build=true note=<same note>, wait for tag kestrel-g-v0.3.0-alpha42, record with tools/record-release.sh, answer the owner in plain words with the Release link.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M research/FINDER_LAB.md

## Last ten checkpoints
```
  21cda298 ckpt 2351: RX3-G7 DONE and ticked (full gates green: JVM + web 331/331; emulator real-e
  265fcf6e ckpt 2350: RX3-G7 other-players ad skipper coded and tested (15 mock tests + manifest/r
  69fcfcf1 ckpt 2349: RX3-Y3 live YouTube check recorded (research/YOUTUBE_ADS.md section 5, tools
  d10a5f4a ckpt 2348: alpha41 RE-DISPATCHED (second run; the first failed on the flaky FLK1 test a
  ff61cb61 ckpt 2347: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
  79518152 ckpt 2346: first alpha41 run FAILED on GitHub: core ProxyLinkTest.theRenewalKeepsItOpen
  d6379d16 ckpt 2345: alpha41 (gecko code 142, manifest 1.0.142) GATED, PUSHED and the android.yml
  836d9546 ckpt 2344: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
  14a6a198 ckpt 2343: RX3-G1 DONE (guard+library declared in the manifest, world MAIN; emulator-pr
  e462351b ckpt 2342: RX3-G3 DONE (script side): the cross-frame postMessage assist channel is gon
```

(14 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
