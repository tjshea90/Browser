# CHECKPOINT 1550 — read me first, then TASKS.md

**Written:** 2026-11-13T16:38:50Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `823cf5b` (this checkpoint is the commit after it)

## Just done
owner message 23 (question: can the browser benefit from adb now that they have the cable) written into TASKS as AJ1a-c; plan: test the free-space levers on the emulator, then answer with a tested recipe

## Do this next
AJ1a: boot the emulator (bash scratchpad ae1_boot.sh, ~3 min), fill /data with fallocate to ~700 MB free, adb install the debug APK (expect INSUFFICIENT_STORAGE + logcat 'Failed to free up'), read the five sys_storage_* settings, put sys_storage_threshold_percentage / sys_storage_cache_percentage to 1 and retry, try pm trim-caches, undo with settings delete global; write research/ADB_FOR_THE_OWNER.md; then answer the owner (AJ1b); heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN still on: stand down on its wakes

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  9a8cb75 ckpt 1545: alpha30 SHIPPED and RECORDED (Release kestrel-g-v0.3.0-alpha30, run 434028420
  c43cfff ckpt 1541: alpha30 RE-DISPATCHED on the test-fix commit 59a5f37: android.yml run 4340284
  59a5f37 ckpt 1540: alpha30 CI run 43399774749 FAILED in test-js (not the app build): web-tests/b
  ea9b9f4 ckpt 1536: alpha30 DISPATCHED: android.yml run 43399774749 (run_number 80) queued 09:15:
  ab7b158 ckpt 1535: gated kestrel-g-v0.3.0-alpha30 (gecko code 129) and pushed it: handoff self-t
  ac7766e ckpt 1534: pre-ship: alpha30 (includes alpha29 and alpha28): your three requests. 1) Set
  613fb9e ckpt 1531: AI1a + AI1b EMULATOR-PROVEN (Settings in the private-window menu; long-press 
  53ae8c1 ckpt 1525: new context window (session restart): the owner re-sent items 1-3 of message 
  5930d95 ckpt 1519: handoff saved (.handoff/handoff.md; CLAUDE.md/AGENTS.md Session Learnings upd
  5c37df1 ckpt 1518: alpha29 SHIPPED and RECORDED (run 43383744581 green, BUILDLOG line written, R
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
