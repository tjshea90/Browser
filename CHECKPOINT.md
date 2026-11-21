# CHECKPOINT 2869 — read me first, then TASKS.md

**Written:** 2026-11-21T23:52:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `24415c3e` (this checkpoint is the commit after it)

## Just done
alpha88 RELEASED and recorded (run 44962198254 green, Release has the APK). alpha89 prepared: lag fixes (scroll-settled look every 30 s, never video/battery saver; Battery Saver advice+Diagnostics words), pornhub layout-rule exceptions (CosmeticFixes), note docs/handoff/ALPHA89_RELEASE_NOTE.txt; gate g7.log running

## Do this next
Next: g7.log GRADLE_EXIT=0 (core+app tests, gecko compile, lint; CosmeticFixesTest uses the real default.kfs) -> bump 190 / 0.3.0-alpha89, sync-ext-version, ship.sh gecko with the ALPHA89 note, ONE dispatch, verify, record-release; answer owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  4bec7a6a ckpt 2864: PERF2c lag evidence from the owner's NEW upload (234658, alpha87, Send now):
  306d0ccc ckpt 2861: Owner follow-up written to TASKS.md as PERF2a-c: hiding off = whole circle (
  bf872eae ckpt 2860: alpha88 (code 189) gated and shipped to GitHub: ONE dispatch made about 23:3
  6f8657d1 ckpt 2859: gated kestrel-g-v0.3.0-alpha88 (gecko code 189) and pushed it: handoff self-
  f02e9214 ckpt 2858: pre-ship: alpha88: what your pornhub clue shows, and two new probes. The cut
  c6fc62aa ckpt 2857: alpha87 RELEASED and recorded (run 44960366094 green, Release has the APK, B
  b34edd20 ckpt 2850: alpha87 SECOND dispatch queued 23:09Z = run 44960366094 (the first, 44958961
  8a2a9b11 ckpt 2849: alpha87 first dispatch (run 44958961031) FAILED in test-js on GitHub: pageti
  3811af55 ckpt 2848: Owner follow-up on pornhub (VD2c, in TASKS.md): Shields ON cuts off the load
  7579817e ckpt 2847: alpha87 (code 188) gated and pushed, ONE android.yml dispatch made at about 
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
