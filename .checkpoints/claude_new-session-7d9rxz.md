# CHECKPOINT 2363 — read me first, then TASKS.md

**Written:** 2026-11-15T01:10:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `6482c198` (this checkpoint is the commit after it)

## Just done
RX3-V4 coded + unit-proven (not ticked yet): core MediaList.applyBlob/matchBlob/matchedBy + MediaCandidate.heights + MediaList.clipNextToFilm (stream under 60 s next to one of 5 min or more is not counted and goes to Other, never what played or was pressed) + MediaRank hook; MediaListBlobTest 14 + MediaListTest 33 + MediaRankTest 23 green; content.js reports blob: players as u='blob' (+ played) - web-tests/blob.test.js 4 green, played/videofacts 15 green; VideoFlow blob route + DASH early look + heights; :geckoapp:compileDebugKotlin green; lab page /blob-lab (tools/sitelab/guardlab.py). alpha44 RELEASED + RECORDED + ANSWERED (run 43660521253). RX3-S2 counters ticked. Independent review agent a47c2917ff11723db running on guard-main G4 + players.js (results arrive as a notification).

## Do this next
RX3-V4 emulator proof, then gates and tick: (1) build ./gradlew --offline :geckoapp:assembleDebug -PgeckoAbi=x86_64 (background, poll log for EXIT), adb install -r the APK, adb shell pm clear app.kestrel.browser.gecko.debug (content.js changed, manifest version unchanged), start the lab python3 -B -I tools/sitelab/guardlab.py 8090 LOG (+ adb reverse tcp:8090 tcp:8090), run python3 -u -I tools/emu/playerlab_emu.py --port 8090 --log LOG --path /blob-lab --grep 'info-blob|media look|DONE' --wait 40; expect 'media look HLS ... length=300s' and 'video info-blob PLAYED el=1 ... matched=1 counted=1 of 2' (the 45 s clip next to the film not counted); write what it showed into research/FINDER_LAB.md (new section 5, ls first) and research/REVIEW notes; (2) full gates: bash scratchpad gates (./gradlew --offline -q :core:test :tools:build :app:testDebugUnitTest :geckoapp:testDebugUnitTest :app:lintRelease then cd web-tests && npm test) detached, poll for EXIT; (3) bash tools/claim.sh RX3-V4 --done evidence; (4) handle the review agent's findings (fix real G4/players.js bugs test-first); (5) then V6 (soft words in MediaRank) and V7 (corpus + precision in FINDER_LAB), then alpha45 = S2 counters + V4 (+ review fixes): bump geckoapp/build.gradle.kts versionCode 146 + versionName 0.3.0-alpha45, bash tools/sync-ext-version.sh, gates, bash ship.sh gecko NOTE, dispatch android.yml. Heartbeat trig_01KEDB7Qxd8zFeawjGcAkSQZ stays until alpha45 is shipped.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  1dea80c7 ckpt 2362: RX3-S2 DONE and ticked (counters only): background.js LST + lstTimed around 
  d90ec3e7 ckpt 2361: alpha44 gated, pushed and dispatched (G4 fake play layers, V3 played element
  02082e19 ckpt 2360: gated kestrel-g-v0.3.0-alpha44 (gecko code 145) and pushed it: handoff self-
  072024e2 ckpt 2359: alpha43 gated, pushed and dispatched to GitHub (V5, D1, S1, B2, G5, G6); wai
  cca67983 ckpt 2358: gated kestrel-g-v0.3.0-alpha43 (gecko code 144) and pushed it: handoff self-
  18b548ee ckpt 2357: pre-ship: alpha43: the glow stops pulsing while a video plays; two more trac
  b7d3f84a ckpt 2356: RX3-S1, B2, G5, G6 DONE and ticked (S1 + G6 emulator-proven, B2 measured wit
  822e687f ckpt 2355: RX3-V5 and RX3-D1 DONE (ticked): AdMarkers/AdRuns/early look and the outage 
  dec2df9d ckpt 2354: alpha42 shipped to GitHub (gated + pushed + android.yml dispatched): G7 + V1
  652f583c ckpt 2353: gated kestrel-g-v0.3.0-alpha42 (gecko code 143) and pushed it: handoff self-
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
