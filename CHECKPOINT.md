# CHECKPOINT 1568 — read me first, then TASKS.md

**Written:** 2026-11-13T17:23:04Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `4b17e9b` (this checkpoint is the commit after it)

## Just done
AJ1d code done + green: core UpdateLogic.lowReserve + new storagePlan (reserve + 2(A+L) + 32 MiB), StorageInfo.ruleReserve/allocatableBytes/reserveBytes (Android's own answer else the min(5 %,500 MiB) rule), UpdateManager uses it, panel + Diagnostics + publish-release.sh + ARCHITECTURE + UPDATE_STORAGE texts corrected, UpdateLogicTest rewritten around the measured emulator boundaries (21 tests); gradle :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin :app:lintDebug BUILD SUCCESSFUL

## Do this next
AJ1d emulator proof: ./gradlew :geckoapp:assembleDebug, adb install -r -d geckoapp-debug.apk, filler to 700 MiB free, setprop debug.kestrel.feed http://10.0.2.2:8000/releases_big.json (mediaserver on 8000 via ae1_boot.sh), open Menu>Updates and Menu>Diagnostics and read the Heads-up + Storage line (expect reserve 297.6 MB = free - getAllocatableBytes, need about 828 MB); then tick AJ1d in TASKS with evidence, then AJ1b: answer the owner (plain words, adb recipe, first read-only commands, public-repo warning, which computer); heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN still on: stand down on its wakes

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  db37d60 ckpt 1555: AJ1a DONE on the Android 10 emulator + Android 10 source: install reserve = m
  18ae1e8 ckpt 1550: owner message 23 (question: can the browser benefit from adb now that they ha
  9a8cb75 ckpt 1545: alpha30 SHIPPED and RECORDED (Release kestrel-g-v0.3.0-alpha30, run 434028420
  c43cfff ckpt 1541: alpha30 RE-DISPATCHED on the test-fix commit 59a5f37: android.yml run 4340284
  59a5f37 ckpt 1540: alpha30 CI run 43399774749 FAILED in test-js (not the app build): web-tests/b
  ea9b9f4 ckpt 1536: alpha30 DISPATCHED: android.yml run 43399774749 (run_number 80) queued 09:15:
  ab7b158 ckpt 1535: gated kestrel-g-v0.3.0-alpha30 (gecko code 129) and pushed it: handoff self-t
  ac7766e ckpt 1534: pre-ship: alpha30 (includes alpha29 and alpha28): your three requests. 1) Set
  613fb9e ckpt 1531: AI1a + AI1b EMULATOR-PROVEN (Settings in the private-window menu; long-press 
  53ae8c1 ckpt 1525: new context window (session restart): the owner re-sent items 1-3 of message 
```

(12 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
