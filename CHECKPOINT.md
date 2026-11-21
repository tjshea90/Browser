# CHECKPOINT 2811 — read me first, then TASKS.md

**Written:** 2026-11-21T16:13:51Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `fb20d562` (this checkpoint is the commit after it)

## Just done
LD3 code done and green: core DiagJournal (exact commit, honest lost/thinned counts, noisy kinds thinned, trouble never) + DiagStats.mergeFrom + DiagSchedule.waitMs + DiagBatch v3 (thinned, collision-safe path suffix); LiveDiag.kt uses them: one upload at a time, create-only PUT (no sha, no delete) retries a taken name with -2/-3, 409 once, check re-armed while in front (BrowserActivity.onPause -> LiveDiag.onBack), Send now not counted in the automatic 4/day, dated run marker line + uptimeS + manualSend flag in device, rolling 7-day week feeds the advisor. core livediag 49 tests green, app diag 72 green (LiveDiagTest 28, 8 new)

## Do this next
(1) digest: print thinned, runs (from 'run start' markers), auto vs manual sends (device.manualSend); (2) update research/LIVE_DIAG.md LD3 + CLAUDE.md private clone note + TASKS LD3 ticks; (3) full gate: ./gradlew --offline -q :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest :geckoapp:compileDebugKotlin then :app:lintDebug; (4) bump geckoapp versionCode 185 / 0.3.0-alpha84, bash tools/sync-ext-version.sh, ship.sh gecko with note file, ONE dispatch, record-release

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  bb443992 ckpt 2810: LD3d tools done: private.sh + diag_digest.py find the clone in any letter ca
  2015daad ckpt 2809: LD3 audit written into research/LIVE_DIAG.md from the 4 real uploads: manual
  51209a26 ckpt 2808: LD3 written into TASKS.md in the owner's words (4 checks of the live diagnos
  91a90882 ckpt 2807: alpha83 SHIPPED and RECORDED: run 44882471063 (run_number 264, head 805cabb9
  6b5bf826 ckpt 2806: alpha83 DISPATCHED once at 07:55Z on 2026-11-21 (android.yml, app=gecko, ful
  805cabb9 ckpt 2805: gated kestrel-g-v0.3.0-alpha83 (gecko code 184) and pushed it: handoff self-
  6eac0f1a ckpt 2804: LD2e: full gate green (:core:test :app:testDebugUnitTest :geckoapp:compileDe
  d2831830 ckpt 2803: LD2a-d built and tested: app LiveDiag (tables fed after switch+whitelist, fa
  9ebf9afd ckpt 2802: LD2 core written + tested (33 tests in core livediag): DiagStats (event hist
  6719ce3e ckpt 2801: gated kestrel-g-v0.3.0-alpha82 (gecko code 183) and pushed it: handoff self-
```

(10 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
