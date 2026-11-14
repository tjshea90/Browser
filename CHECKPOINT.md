# CHECKPOINT 2349 — read me first, then TASKS.md

**Written:** 2026-11-14T22:00:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `ce2e0ec3` (this checkpoint is the commit after it)

## Just done
RX3-Y3 live YouTube check recorded (research/YOUTUBE_ADS.md section 5, tools/sitelab/yt_live.js, YoutubePayloadDump): m.youtube.com payload 10 calls ran 10:0:1, www.youtube.com payload 28 calls ran 28:0:1, player OK and video advanced in Chromium; alpha41 run 43645217162 has check, test-js, test-jvm green and build-gecko running; heartbeat trigger created

## Do this next
HEARTBEAT trig_01KEDB7Qxd8zFeawjGcAkSQZ (every 2 h, read it with get_trigger; delete_trigger when the RX3 job is shipped). (1) WAIT for alpha41 run 43645217162 (check, test-js, test-jvm are GREEN; build-gecko job 132602466692 was building at 22:00Z): git ls-remote --tags origin kestrel-g-v0.3.0-alpha41 shows the tag, mcp__github__actions_list list_workflow_jobs resource_id 43645217162 shows every job success, mcp__github__get_release_by_tag shows the APK. If a job fails: mcp__github__get_job_logs job_id <id> return_content true tail_lines 70; a courier/timing flake -> mcp__github__actions_run_trigger rerun_failed_jobs run_id 43645217162, never a new dispatch. ONLY THEN: bash tools/record-release.sh kestrel-g-v0.3.0-alpha41 "alpha41: Back remembers your place on each page and counts how Back behaves (Back lab); YouTube ad skipping (mutes and fast-forwards ads, presses Skip, closes banners, removes the ad-blocker box; one switch in Settings, on by default) and the lists' full YouTube rules; lists can only rewrite pages if they are the uBlock lists; a page can no longer fake your tap or click inside frames it embeds; the pop-up guard is there from the very first page. Tested on the emulator and in test browsers, not on your tablet." then answer the owner in plain words with https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha41 on its own line: what was proven (emulator: Back kept/reload/pull, guard from the first page, frame assist in the real engine; test browsers: tap forgery, JSON edit, skipper on a mock player built from the live markup, live YouTube check in Chromium) vs NOT run on the tablet, and re-list the open items (BK5 question: may Back be instant for no-store pages on sites you pick; AX1g system keyboard YES/NO; alpha40+alpha41 tablet feedback; the YouTube screenshot was answered). (2) Meanwhile, no build needed: RX3-G7 (generic players, content.js, web test first: web-tests/ new file), then Wave B in order V1 V2 V5 V3 D1 G4 G5 G6 S1 B2; claim each row with bash tools/claim.sh RX3-<id>, ckpt after every step.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d10a5f4a ckpt 2348: alpha41 RE-DISPATCHED (second run; the first failed on the flaky FLK1 test a
  ff61cb61 ckpt 2347: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
  79518152 ckpt 2346: first alpha41 run FAILED on GitHub: core ProxyLinkTest.theRenewalKeepsItOpen
  d6379d16 ckpt 2345: alpha41 (gecko code 142, manifest 1.0.142) GATED, PUSHED and the android.yml
  836d9546 ckpt 2344: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
  14a6a198 ckpt 2343: RX3-G1 DONE (guard+library declared in the manifest, world MAIN; emulator-pr
  e462351b ckpt 2342: RX3-G3 DONE (script side): the cross-frame postMessage assist channel is gon
  279f3b45 ckpt 2341: RX3-BK1/BK2 first emulator proof (static case): page left at y=1800, Back re
  8888c78b ckpt 2340: RX3-BK1/BK2 Kotlin wiring DONE and COMPILES (geckoapp + app debug): ShieldsB
  687414dd ckpt 2339: owner message 40 (a screenshot, no words: YouTube Movies and TV page 'Video 
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
