# CHECKPOINT 2883 — read me first, then TASKS.md

**Written:** 2026-11-22T01:01:13Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `146f0c2b` (this checkpoint is the commit after it)

## Just done
PERF3 findings from the owner's alpha88/89 uploads: Battery Saver is OFF now (powerSave 0) and typing is still slow; the app UI thread had a 29 s late-beat at +105 s of the alpha89 run (Settings screen drawing under MTK ViewDebug); first start after an install paints at 30-43 s (oat none, claude.ai restored tab 124 s to load, content process lateness 11 s); the page report lists pornhub LISTING page hiding rules (ad links + cookie banner, nothing geometric): the video page was not opened before Send now. Built probes (not shipped yet): typing latency in the address bar (win typing-address) and in page fields (win typing-page via content.js input timestamps), hiding count now includes generic batches; digest TYPING LATENCY section. Gates g9.log + w3.log running

## Do this next
Next: g9 GRADLE_EXIT=0 and w3 WEB_EXIT=0 -> bump 191 / 0.3.0-alpha90, sync-ext-version, ALPHA90 note (docs/handoff/ALPHA90_RELEASE_NOTE.txt), ship.sh gecko, ONE dispatch, record-release; ask the owner for ONE upload: type in the address bar, then in claude.ai's prompt box and in google.com's search box, open a pornhub VIDEO page for 20 s, then Send now

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  67c4ba53 ckpt 2875: Owner after alpha89: video still stuck, typing very laggy, loading animation
  df05f162 ckpt 2874: alpha89 RELEASED and recorded (run 44964790281 green, Release has the APK). 
  69bc7341 ckpt 2872: alpha89 (code 190) gated and shipped: ONE dispatch made right after ship.sh 
  d89d6370 ckpt 2871: gated kestrel-g-v0.3.0-alpha89 (gecko code 190) and pushed it: handoff self-
  c66293fb ckpt 2870: pre-ship: alpha89: the lag, and the cut-off loading circle. Your last upload
  ccdb2da4 ckpt 2869: alpha88 RELEASED and recorded (run 44962198254 green, Release has the APK). 
  4bec7a6a ckpt 2864: PERF2c lag evidence from the owner's NEW upload (234658, alpha87, Send now):
  306d0ccc ckpt 2861: Owner follow-up written to TASKS.md as PERF2a-c: hiding off = whole circle (
  bf872eae ckpt 2860: alpha88 (code 189) gated and shipped to GitHub: ONE dispatch made about 23:3
  6f8657d1 ckpt 2859: gated kestrel-g-v0.3.0-alpha88 (gecko code 189) and pushed it: handoff self-
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
