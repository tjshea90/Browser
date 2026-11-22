# CHECKPOINT 2872 — read me first, then TASKS.md

**Written:** 2026-11-22T00:04:15Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `d89d6370` (this checkpoint is the commit after it)

## Just done
alpha89 (code 190) gated and shipped: ONE dispatch made right after ship.sh (do not dispatch again unless it fails). Contents: scroll-settled last-look every 30 s and never during a video or Battery Saver, Battery Saver advice + Diagnostics words, CosmeticFixes dropping pornhub's two layout rules, FILE PICKS/VIDEO digest. Gates: core+app tests, gecko compile, lint, web-tests 671/671, handoff 89/89. send_later reminder +24 min finishes record-release

## Do this next
Next: find the alpha89 run, wait all jobs green, get_release_by_tag kestrel-g-v0.3.0-alpha89 has the APK, NOTE=$(cat docs/handoff/ALPHA89_RELEASE_NOTE.txt); bash tools/record-release.sh kestrel-g-v0.3.0-alpha89 "$NOTE"; tick PERF2a/PERF2c only after the owner confirms on the tablet; owner to test: Battery Saver off, pornhub circle, then Send now; still open: pornhub player spin (alpha88 stall probe), IMG1 cause, GL1 other sites, VD1 Reels switch test, PV1f/g, LD3f, LD4f/g, YTC1/2, ATT1, FLK1, PB8

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d89d6370 ckpt 2871: gated kestrel-g-v0.3.0-alpha89 (gecko code 190) and pushed it: handoff self-
  c66293fb ckpt 2870: pre-ship: alpha89: the lag, and the cut-off loading circle. Your last upload
  ccdb2da4 ckpt 2869: alpha88 RELEASED and recorded (run 44962198254 green, Release has the APK). 
  4bec7a6a ckpt 2864: PERF2c lag evidence from the owner's NEW upload (234658, alpha87, Send now):
  306d0ccc ckpt 2861: Owner follow-up written to TASKS.md as PERF2a-c: hiding off = whole circle (
  bf872eae ckpt 2860: alpha88 (code 189) gated and shipped to GitHub: ONE dispatch made about 23:3
  6f8657d1 ckpt 2859: gated kestrel-g-v0.3.0-alpha88 (gecko code 189) and pushed it: handoff self-
  f02e9214 ckpt 2858: pre-ship: alpha88: what your pornhub clue shows, and two new probes. The cut
  c6fc62aa ckpt 2857: alpha87 RELEASED and recorded (run 44960366094 green, Release has the APK, B
  b34edd20 ckpt 2850: alpha87 SECOND dispatch queued 23:09Z = run 44960366094 (the first, 44958961
```
