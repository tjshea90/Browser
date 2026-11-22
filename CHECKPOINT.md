# CHECKPOINT 2887 — read me first, then TASKS.md

**Written:** 2026-11-22T01:18:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `eb28b328` (this checkpoint is the commit after it)

## Just done
alpha90 (code 191) gated and shipped: ONE dispatch made right after ship.sh (do not dispatch again unless it fails); send_later reminder +24 min finishes record-release. Contents: typing-latency probes (address bar win typing-address; page fields content.js input timestamps -> win typing-page), hidden-rules count includes generic batches, digest TYPING LATENCY, pagetime test stabilised (grace 4000; it failed under load once). Web 672/674 under Gradle load (handover timed out at 60 s, pagetime counted 450): both pass alone 3x

## Do this next
Next: wait for the reminder; after it: record-release, then read the owner's next upload (TYPING LATENCY, VIDEO PROBLEMS code 99, page report hidden rules on the pornhub VIDEO page) and act on it; open: PERF3a/b/c, IMG1 cause, VD1 Reels switch test, PV1f/g, LD4f/g, YTC1/2, ATT1, FLK1, PB8

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  eb28b328 ckpt 2886: gated kestrel-g-v0.3.0-alpha90 (gecko code 191) and pushed it: handoff self-
  9b7f6817 ckpt 2885: pre-ship: alpha90: finding out where the typing delay and the stuck video re
  a4f81831 ckpt 2883: PERF3 findings from the owner's alpha88/89 uploads: Battery Saver is OFF now
  67c4ba53 ckpt 2875: Owner after alpha89: video still stuck, typing very laggy, loading animation
  df05f162 ckpt 2874: alpha89 RELEASED and recorded (run 44964790281 green, Release has the APK). 
  69bc7341 ckpt 2872: alpha89 (code 190) gated and shipped: ONE dispatch made right after ship.sh 
  d89d6370 ckpt 2871: gated kestrel-g-v0.3.0-alpha89 (gecko code 190) and pushed it: handoff self-
  c66293fb ckpt 2870: pre-ship: alpha89: the lag, and the cut-off loading circle. Your last upload
  ccdb2da4 ckpt 2869: alpha88 RELEASED and recorded (run 44962198254 green, Release has the APK). 
  4bec7a6a ckpt 2864: PERF2c lag evidence from the owner's NEW upload (234658, alpha87, Send now):
```
