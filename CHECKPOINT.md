# CHECKPOINT 1570 — read me first, then TASKS.md

**Written:** 2026-11-13T17:43:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `71ffdcf` (this checkpoint is the commit after it)

## Just done
AJ1 DONE and answered in chat: adb question (owner msgs 23 + newer msg 24 'without writing anything to this repo, what adb commands/permissions help speed or function', captured by session ccr-f28fb6d8-1n4nmu). Tested on the Android 10 emulator + Android 10 source: install reserve = min(5 % of storage, 500 MiB), install needs reserve + 2 x (package + libraries); owner tablet needs about 1 GiB and had 1.5 GiB free. AJ1d (updater model + texts) done in code, 21 core tests + app tests + lint green, emulator Updates sheet shows 'Android keeps 297.6 MB' = framework's own number; NOT released (ships with the next version). Verified adb commands on Android 10: cmd appops set REQUEST_INSTALL_PACKAGES, pm grant, cmd deviceidle whitelist, settings put global, pm trim-caches, dumpsys diskstats/meminfo/gfxinfo, am kill-all

## Do this next
WAIT for the owner: (a) output of adb install -r of the alpha30 APK (Success or the failure line) + adb shell df /data + the two sys_storage_threshold settings (null = defaults), (b) which computer, (c) the old open list (Doraby page report, Diagnostics App: line, site wording, choppy video, scroll/Back feel, Proton Mail, keyboard, GitHub list build yes/no, signing key SEC-1). Optional only if they ask: AJ1c hidden USB-debug switch (GeckoRuntimeSettings remoteDebuggingEnabled + consoleOutput, off by default; today both are false in GeckoEngine.kt:107-108 so logcat shows no page errors). Next release (alpha31) carries AJ1d: bump versionCode, bash ship.sh gecko note, run web-tests first (cd web-tests && npm test), dispatch android.yml. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN still on: stand down on its wakes, delete after 24 h owner silence. Another session ccr-f28fb6d8-1n4nmu is live (only INBOX so far): do not duplicate its answer, read its OUTBOX tail before replying

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  47dbd77 ckpt 1568: AJ1d code done + green: core UpdateLogic.lowReserve + new storagePlan (reserv
  db37d60 ckpt 1555: AJ1a DONE on the Android 10 emulator + Android 10 source: install reserve = m
  18ae1e8 ckpt 1550: owner message 23 (question: can the browser benefit from adb now that they ha
  9a8cb75 ckpt 1545: alpha30 SHIPPED and RECORDED (Release kestrel-g-v0.3.0-alpha30, run 434028420
  c43cfff ckpt 1541: alpha30 RE-DISPATCHED on the test-fix commit 59a5f37: android.yml run 4340284
  59a5f37 ckpt 1540: alpha30 CI run 43399774749 FAILED in test-js (not the app build): web-tests/b
  ea9b9f4 ckpt 1536: alpha30 DISPATCHED: android.yml run 43399774749 (run_number 80) queued 09:15:
  ab7b158 ckpt 1535: gated kestrel-g-v0.3.0-alpha30 (gecko code 129) and pushed it: handoff self-t
  ac7766e ckpt 1534: pre-ship: alpha30 (includes alpha29 and alpha28): your three requests. 1) Set
  613fb9e ckpt 1531: AI1a + AI1b EMULATOR-PROVEN (Settings in the private-window menu; long-press 
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
