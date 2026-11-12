# CHECKPOINT 1101 — read me first, then TASKS.md

**Written:** 2026-11-12T16:30:37Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `129874a` (this checkpoint is the commit after it)

## Just done
AA4 speed batch code step: K1 LiveBudget (core, 9 tests) + RAM-adaptive live tabs + trim policy + private tab keeps URL + gecko-prefs v5 viewers 6 + Settings 'Tabs kept ready'; K2 TabStateStore (6 Robolectric tests) saves each tab's Back stack/scroll as sanitized JSON (formdata stripped) and restores it at start; K3 configChanges uiMode|locale|layoutDirection; K4 no engine restart while hidden; K5 preview cover while a tab revives/cold start; K6 link launch no longer loads the saved tab too; K9/K10/K11/K12 cheap fixes; JS: B1 css cache lifetime+generation+nav invalidation, J5.1 in-flight dedupe, B2 tabTop LRU, J3 small files/range-piece dedupe, J2a header prefilter, J4 content scan, J7 passive, J9 pageHashes memo; web-tests 75/75, JVM :core :app all green, compile of geckoapp ok

## Do this next
Build the debug APK (./gradlew :geckoapp:assembleDebug -PgeckoAbi=x86_64) and verify on the emulator: (1) Diagnostics 'Engine:' line shows budget + MemAvailable; open 4 tabs and confirm live sessions follow it; (2) open a long page, scroll, open a link, force-stop, relaunch: Back stack and scroll restored (logcat/Health: tabstate.restored) and tabstate/<id>.json has no formdata; (3) am send-trim-memory ... RUNNING_CRITICAL/UI_HIDDEN; (4) cmd uimode night yes keeps sessions. Then Back picture for no-store pages (BackPicture in BrowserActivity), K7 adaptive download cap, K8 init after first frame, then bump versionCode 121 / 0.3.0-alpha22, tools/sync-ext-version.sh, ship.sh gecko, dispatch android.yml, record-release, answer owner message 14. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6fc25a2 ckpt 1077: Reviews saved: research/PERF_KOTLIN.md, research/PERF_JS.md, research/PROMPT_
  7ceb366 ckpt 1075: AA3 audit done by an independent read-only agent: research/PROMPT_AUDIT.md (2
  9c34e86 ckpt 1072: AA1 proven on the emulator (simulated VPN: Network line, trr=0, http3 off, ti
  a0a56be ckpt 1069: AA2 + AA1 core done: Courier closed in the browser (every option OFF by defau
  ac00a1f ckpt 1061: Owner message 14 written into TASKS.md as AA1-AA4 in their words (INBOX has i
  d75864a ckpt 1057: Owner message 13 ANSWERED and SHIPPED: Kestrel Courier alpha3 (kestrel-courie
  5c2891c ckpt 1054: Docs: CLAUDE.md + BRIEF.md record the owner's unmetered/5 Mbit/s facts and th
  95c9050 ckpt 1053: gated kestrel-g-v0.3.0-alpha21 (gecko code 120) and pushed it: handoff self-t
  2a514af ckpt 1052: gated kestrel-courier-v0.1.0-alpha3 (courier code 3) and pushed it: handoff s
  d803d90 ckpt 1051: pre-ship: alpha3: pages through the phone. The phone now loads the tablet's w
```

(23 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
