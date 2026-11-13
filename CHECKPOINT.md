# CHECKPOINT 1596 — read me first, then TASKS.md

**Written:** 2026-11-13T18:05:22Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `2118f48` (this checkpoint is the commit after it)

## Just done
AK4 steps 1-3 done in code + tests: (1) extension background.js captures the browser's real Referer/Origin per request (onBeforeSendHeaders) and sends rf/og with every media message; new 'maybe' message for script-fetched responses that say nothing (text/plain, octet-stream, css/image fetched by a script), 12 per page; (2) core MediaHeaders.sets (captured -> player page -> web page -> Referer only -> none), MediaSniff.classifyBytes (playlist/mp4/webm/ogg/flv/avi/mp3 by first bytes), MediaCandidate.referer/origin/viaHeaders, SegmentPrefix (picture header in front of TS parts) used by StreamEngine appendNext + decideMux; (3) app: ShieldsBridge media rf/og + maybe -> VideoFlow.maybe (reads first 4 KB with header fallback, adds candidate), VideoFlow.fetchPlaylist tries header sets and remembers the one that worked, honest HTTP-status messages, file downloads send the player's headers, Health.log('media',...) events. Tests green: MediaHeadersTest 6, MediaSniffTest 13, MediaListTest 9, SegmentPrefixTest 7, StreamEngineTest 31, web background.test.js 38; :geckoapp:compileDebugKotlin ok

## Do this next
AK4 next: (a) lab on the emulator: x86_64 debug build (./gradlew :geckoapp:assembleDebug -PgeckoAbi=x86_64), install, python lab server on 8000/8003/8004 (page A with an iframe to B, B fetches a masked playlist.txt (text/plain) + .jpg parts with PNG header + AES-128 key from C which 403s unless Referer is B's origin), check the Download-video sheet finds it and the saved file ffprobe-s OK; also AK3 (Home card) and AK2 (APK size) proofs; (b) research/VIDEO_SOURCES.md; (c) run cd web-tests && npm test (all) + ./gradlew :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin :app:lintDebug; (d) ship alpha31: versionCode 130 / 0.3.0-alpha31 in geckoapp/build.gradle.kts, bash ship.sh gecko note, dispatch android.yml, wait, record-release, answer the owner (plain words; the vidsrc cause = Referer of the web page instead of the player's page)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a32efe5 ckpt 1582: AK1 answered in chat (appops REQUEST_INSTALL_PACKAGES/RUN_IN_BACKGROUND/RUN_A
  24b86ca ckpt 1570: AJ1 DONE and answered in chat: adb question (owner msgs 23 + newer msg 24 'wi
  47dbd77 ckpt 1568: AJ1d code done + green: core UpdateLogic.lowReserve + new storagePlan (reserv
  db37d60 ckpt 1555: AJ1a DONE on the Android 10 emulator + Android 10 source: install reserve = m
  18ae1e8 ckpt 1550: owner message 23 (question: can the browser benefit from adb now that they ha
  9a8cb75 ckpt 1545: alpha30 SHIPPED and RECORDED (Release kestrel-g-v0.3.0-alpha30, run 434028420
  c43cfff ckpt 1541: alpha30 RE-DISPATCHED on the test-fix commit 59a5f37: android.yml run 4340284
  59a5f37 ckpt 1540: alpha30 CI run 43399774749 FAILED in test-js (not the app build): web-tests/b
  ea9b9f4 ckpt 1536: alpha30 DISPATCHED: android.yml run 43399774749 (run_number 80) queued 09:15:
  ab7b158 ckpt 1535: gated kestrel-g-v0.3.0-alpha30 (gecko code 129) and pushed it: handoff self-t
```

(13 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
