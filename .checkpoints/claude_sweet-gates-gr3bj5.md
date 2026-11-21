# CHECKPOINT 2802 — read me first, then TASKS.md

**Written:** 2026-11-21T07:39:29Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `ad18ca1a` (this checkpoint is the commit after it)

## Just done
LD2 core written + tested (33 tests in core livediag): DiagStats (event histogram + per-site load table, bounded, save/load), Advisor (11 rules over settings + stats + device/playback numbers -> 'Settings > <section> > <row>: change X to Y. Why: numbers'; tap-apply only for 8 whitelisted keys), DiagBatch v2 (settings, device, kinds, sites, advice). alpha82 gates passed (ship.sh EXIT 0) but alpha82 is NOT dispatched (folded into alpha83).

## Do this next
LD2 app side: (1) LiveDiag: DiagStats instance fed in observe() after the switch + whitelist, persisted in livediag.stats, cleared on send/off; facts() = device numbers (ActivityManager mem, StatFs free MB, thermal, powerSave, link kbps, phonePaired from prefs phone_code, filterAgeDays from upd_last, liveFailsInRow) + extraFacts supplier from BrowserActivity (slowFrames, worstFrameMs, NativePlayerStats plays/stalls/frames/linkBps); settings snapshot = Boolean/Int keys of SharedPreferences 'kestrel' minus phone_*/upd_*/data_* + string keys search/https/data_saver; advice() ; apply(advice) via Advisor.canApply; (2) OFF = asleep: removeCallbacks of the delayed send, in-flight upload re-checks enabled before PUT, advice/facts/onFront/sendNow do nothing when off; (3) Settings 'Suggested changes' rows with Apply buttons; (4) upload carries settings/device/kinds/sites/advice; (5) tools/diag_digest.py v2; (6) LiveDiagTest additions; gate; versionCode 184 / alpha83 + ALPHA83 note (also covers alpha82's scrub fixes); ship; answer owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6719ce3e ckpt 2801: gated kestrel-g-v0.3.0-alpha82 (gecko code 183) and pushed it: handoff self-
  f95e13ba ckpt 2800: LD2 written into TASKS.md in the owner's words (LD2a-e): smarter + more comp
  946369a8 ckpt 2799: alpha82 prepared: versionCode 183 / 0.3.0-alpha82, extension version synced,
  2d4c1e81 ckpt 2798: alpha81 SHIPPED and RECORDED: run 44878029773 green (check, test-js, test-jv
  dba49a28 ckpt 2797: LD1 privacy fix 2 (for alpha82): DiagScrub removes dotted-quad network addre
  db02a2b5 ckpt 2796: LD1 privacy fix (for alpha82): DiagScrub now drops the path after a BARE hos
  472a364c ckpt 2795: alpha81 DISPATCHED once: android.yml run 44878029773 (run_number 263, head 7
  7f084cff ckpt 2794: gated kestrel-g-v0.3.0-alpha81 (gecko code 182) and pushed it: handoff self-
  68f9d24e ckpt 2793: LD1e: full gate green (:core:test, :app:testDebugUnitTest, :geckoapp:compile
  a6c87bc8 ckpt 2792: LD1d done: tools/diag_digest.py (crashes/kills, slow sites by host, playback
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
