# CHECKPOINT 2350 — read me first, then TASKS.md

**Written:** 2026-11-14T22:41:16Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `f8644993` (this checkpoint is the commit after it)

## Just done
RX3-G7 other-players ad skipper coded and tested (15 mock tests + manifest/relay tests), real-engine emulator run proved 5 of 5 scenarios; found and fixed the 4-video cap and the dropped-reports bug (page unknown on the first page after a cold start); bumped to alpha42 / versionCode 143

## Do this next
HEARTBEAT trig_01KEDB7Qxd8zFeawjGcAkSQZ (every 2 h; get_trigger to read; delete_trigger when the RX3 job is shipped). alpha41 is RELEASED and RECORDED (https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha41) and the owner has the answer. NOW: RX3-G7 (claimed [~]) = other-players ad skipper, CODE DONE: geckoapp/src/main/assets/kestrel-shields/players.js + manifest entry (exclude_matches the YouTube hosts) + background.js (yts relay w + page fallback, skipText counters) + BrowserActivity (second AdSkipStats 'Other players ads', 'ads report-in' Health line with page host - REMOVE the tabs=... part of that line if noisy) + core AdSkipStats(label); web-tests/players.test.js 15 pass, background.test.js +3 RX3-G7 tests; class names READ from the players' public source (research/YOUTUBE_ADS.md section 6, plyr--ad does NOT exist). versionCode bumped to 143 / alpha42 (manifest 1.0.143 via tools/sync-ext-version.sh). EMULATOR (AVD up, lab server python3 -B -I tools/sitelab/guardlab.py 8090 <log> page /playerlab, driver python3 -u -I tools/emu/playerlab_emu.py --port 8090 --log <log>): run 3 showed all five scenarios work in the REAL engine (ad 12 s -> ended in 0.8-1.25 s at speed 16; vjs-ad-loading alone untouched; hosted IMA ad sped, film beside it untouched; skip button pressed only once skippable with pointerdown mousedown pointerup mouseup click). FOUND + FIXED on the way: cap of 4 followed videos (now 12 with eviction) and the yts relay had no page on the first page after a cold start so the app dropped EVERY report as private (alpha41 YouTube skipper line was affected too) - fix = relay falls back to sender.url of the top frame. TO DO NEXT: (1) after the extension finished updating (first start of 1.0.143 takes ~2 min) rerun the driver and confirm the Health line says private=false and an 'ads report' line / the Diagnostics line 'Other players ads: N seen' appears; then paste the lab numbers into research/YOUTUBE_ADS.md section 6.4, tick RX3-G7 with bash tools/claim.sh --done "<proof>" (RX3-G7), run the full web suite (cd web-tests && npm test) and the gradle gate (./gradlew --offline -q :core:test :tools:build :app:testDebugUnitTest :app:lintRelease in background, poll log for EXIT), ckpt. (2) Wave B in order V1 V2 V5 V3 D1 G4 G5 G6 S1 B2 (research/REVIEW-EXTERNAL-3.md sections 6 and 5): claim each with bash tools/claim.sh RX3-<id>, failing test first, ckpt after every step. (3) alpha42 ships when Wave A is done (Y3 emulator leftovers may wait): bash ship.sh gecko "<note>" then dispatch android.yml (app=gecko full_build=true ref claude/new-session-7d9rxz), wait, record-release with the identical note.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  69fcfcf1 ckpt 2349: RX3-Y3 live YouTube check recorded (research/YOUTUBE_ADS.md section 5, tools
  d10a5f4a ckpt 2348: alpha41 RE-DISPATCHED (second run; the first failed on the flaky FLK1 test a
  ff61cb61 ckpt 2347: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
  79518152 ckpt 2346: first alpha41 run FAILED on GitHub: core ProxyLinkTest.theRenewalKeepsItOpen
  d6379d16 ckpt 2345: alpha41 (gecko code 142, manifest 1.0.142) GATED, PUSHED and the android.yml
  836d9546 ckpt 2344: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
  14a6a198 ckpt 2343: RX3-G1 DONE (guard+library declared in the manifest, world MAIN; emulator-pr
  e462351b ckpt 2342: RX3-G3 DONE (script side): the cross-frame postMessage assist channel is gon
  279f3b45 ckpt 2341: RX3-BK1/BK2 first emulator proof (static case): page left at y=1800, Back re
  8888c78b ckpt 2340: RX3-BK1/BK2 Kotlin wiring DONE and COMPILES (geckoapp + app debug): ShieldsB
```

(21 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
