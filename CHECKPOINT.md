# CHECKPOINT 1555 — read me first, then TASKS.md

**Written:** 2026-11-13T17:16:00Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `5559a49` (this checkpoint is the commit after it)

## Just done
AJ1a DONE on the Android 10 emulator + Android 10 source: install reserve = min(5 % of storage, 500 MiB) (sys_storage_threshold_percentage / _max_bytes), NOT 10 %; install needs free >= reserve + 2(package + unpacked libs) (+margin): small APK 302-304 MiB (5 %+2A), 2 %: 123, 100 MiB cap: 105, 20 %: 503 (cap binds); big APK 861-868 default, 676-685 at 2 %, passes 760 with 100 MiB cap; cache keys no effect. Owner tablet: 500 MiB + 2x(94.4+131.7) = about 0.95 GiB needed vs 1.51 GiB free = it should already fit. Notes in research/ADB_FOR_THE_OWNER.md; AJ1d added to TASKS

## Do this next
AJ1d: (1) core UpdateLogic: add lowReserve(total, pct=5, max=500MiB) + new storagePlan (installTakes = 2(A+L)+margin 32 MiB, needKeep = reserve+installTakes, needDrop = needKeep - A) + rewrite UpdateLogicTest (emulator boundaries 863/685/302, owner tablet fits); (2) UpdateManager.reserveBytes -> read Settings.Global sys_storage_threshold_percentage/_max_bytes, prefer free - getAllocatableBytes when it answers; (3) texts in UpdatePanel (heads-up line 87, NEEDS_SPACE line 170), StorageInfo line 27+updateVerdict, publish-release.sh line 35, docs/ARCHITECTURE.md 125-128, research/UPDATE_STORAGE.md correction banner; run ./gradlew :core:test :app:testDebugUnitTest; then AJ1b: answer the owner (plain words, adb recipe, first read-only commands, public-repo warning, ask which computer); heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN still on: stand down on its wakes

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  18ae1e8 ckpt 1550: owner message 23 (question: can the browser benefit from adb now that they ha
  9a8cb75 ckpt 1545: alpha30 SHIPPED and RECORDED (Release kestrel-g-v0.3.0-alpha30, run 434028420
  c43cfff ckpt 1541: alpha30 RE-DISPATCHED on the test-fix commit 59a5f37: android.yml run 4340284
  59a5f37 ckpt 1540: alpha30 CI run 43399774749 FAILED in test-js (not the app build): web-tests/b
  ea9b9f4 ckpt 1536: alpha30 DISPATCHED: android.yml run 43399774749 (run_number 80) queued 09:15:
  ab7b158 ckpt 1535: gated kestrel-g-v0.3.0-alpha30 (gecko code 129) and pushed it: handoff self-t
  ac7766e ckpt 1534: pre-ship: alpha30 (includes alpha29 and alpha28): your three requests. 1) Set
  613fb9e ckpt 1531: AI1a + AI1b EMULATOR-PROVEN (Settings in the private-window menu; long-press 
  53ae8c1 ckpt 1525: new context window (session restart): the owner re-sent items 1-3 of message 
  5930d95 ckpt 1519: handoff saved (.handoff/handoff.md; CLAUDE.md/AGENTS.md Session Learnings upd
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
