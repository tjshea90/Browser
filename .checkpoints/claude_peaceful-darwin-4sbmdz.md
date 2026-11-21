# CHECKPOINT 2864 — read me first, then TASKS.md

**Written:** 2026-11-21T23:50:08Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `567047ef` (this checkpoint is the commit after it)

## Just done
PERF2c lag evidence from the owner's NEW upload (234658, alpha87, Send now): (1) powerSave=1 in BOTH alpha86 and alpha87 uploads = Android Battery Saver was ON; Advisor rule + Diagnostics words added; (2) the app read back the page picture every 4-6 s while scrolling (27 'tabs look-saved', kestrel-io 35% CPU, readback is a synchronous compositor call): scroll-settled look now every 30 s, never during a video or battery saver; (3) pornhub video errors in the upload: code 9/4 are mostly the hover PREVIEW videos (media PARSED_DATA_CACHED), the main player's spin gives no error -> alpha88 stall probe. Tests not yet run for the lag changes

## Do this next
Next: run :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin (g7) + web-tests unaffected; PERF2a exception table for pornhub's geometry rules (div.container margin-top, .video-wrapper > #player ~ .hd.clear): the owner A/B proved the cosmetic rules cut the circle; then ship alpha89; alpha88 run must be recorded first (send_later trig_01CcoH9KEMy1roPiSc9Ki1Fu 23:54Z)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/src/main/kotlin/app/kestrel/browser/BrowserActivity.kt

## Last ten checkpoints
```
  306d0ccc ckpt 2861: Owner follow-up written to TASKS.md as PERF2a-c: hiding off = whole circle (
  bf872eae ckpt 2860: alpha88 (code 189) gated and shipped to GitHub: ONE dispatch made about 23:3
  6f8657d1 ckpt 2859: gated kestrel-g-v0.3.0-alpha88 (gecko code 189) and pushed it: handoff self-
  f02e9214 ckpt 2858: pre-ship: alpha88: what your pornhub clue shows, and two new probes. The cut
  c6fc62aa ckpt 2857: alpha87 RELEASED and recorded (run 44960366094 green, Release has the APK, B
  b34edd20 ckpt 2850: alpha87 SECOND dispatch queued 23:09Z = run 44960366094 (the first, 44958961
  8a2a9b11 ckpt 2849: alpha87 first dispatch (run 44958961031) FAILED in test-js on GitHub: pageti
  3811af55 ckpt 2848: Owner follow-up on pornhub (VD2c, in TASKS.md): Shields ON cuts off the load
  7579817e ckpt 2847: alpha87 (code 188) gated and pushed, ONE android.yml dispatch made at about 
  85221da4 ckpt 2846: gated kestrel-g-v0.3.0-alpha87 (gecko code 188) and pushed it: handoff self-
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
