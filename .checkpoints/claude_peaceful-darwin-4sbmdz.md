# CHECKPOINT 2885 — read me first, then TASKS.md

**Written:** 2026-11-22T01:17:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `f8cf13be` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha90: finding out where the typing delay and the stuck video really are. Your last two uploads say Battery Saver is off now and typing was still slow, that the first start after an install takes 30 to 43 seconds to paint (the tablet is busy compiling and loading the restored claude.ai tab, which alone took 124 seconds to finish), and that on claude.ai the page itself was too busy to answer for up to 11 seconds at a time, so typing in a busy page is slow whatever Kestrel does. I will not guess further, so alpha90 measures it: how long each typed character waits, in the address bar and Home field (the app) and separately in page fields (the page and the engine), numbers only, never the text, nothing from a private tab; the digest prints it as TYPING LATENCY and says which side is slow. The page report for the pornhub LISTING page named only ad links and a cookie banner as hidden, which cannot clip a loading circle, so the rule is on the VIDEO page: the hidden-rules count now also covers the generic rules the page asks for. To get the answer please do this once on alpha90: open a pornhub video page, wait 20 seconds with the circle showing, type a few letters in the address bar, then press Send now. Not run on the tablet: everything above.

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/build.gradle.kts
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  a4f81831 ckpt 2883: PERF3 findings from the owner's alpha88/89 uploads: Battery Saver is OFF now
  67c4ba53 ckpt 2875: Owner after alpha89: video still stuck, typing very laggy, loading animation
  df05f162 ckpt 2874: alpha89 RELEASED and recorded (run 44964790281 green, Release has the APK). 
  69bc7341 ckpt 2872: alpha89 (code 190) gated and shipped: ONE dispatch made right after ship.sh 
  d89d6370 ckpt 2871: gated kestrel-g-v0.3.0-alpha89 (gecko code 190) and pushed it: handoff self-
  c66293fb ckpt 2870: pre-ship: alpha89: the lag, and the cut-off loading circle. Your last upload
  ccdb2da4 ckpt 2869: alpha88 RELEASED and recorded (run 44962198254 green, Release has the APK). 
  4bec7a6a ckpt 2864: PERF2c lag evidence from the owner's NEW upload (234658, alpha87, Send now):
  306d0ccc ckpt 2861: Owner follow-up written to TASKS.md as PERF2a-c: hiding off = whole circle (
  bf872eae ckpt 2860: alpha88 (code 189) gated and shipped to GitHub: ONE dispatch made about 23:3
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
