# CHECKPOINT 2870 — read me first, then TASKS.md

**Written:** 2026-11-22T00:03:02Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `ccdb2da4` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha89: the lag, and the cut-off loading circle. Your last upload explains most of the lag. (1) Android Battery Saver was ON in both of today's uploads (it slows the processor, and this tablet only has four slow cores): please turn it off in Android Settings > Battery while you use Kestrel; Diagnostics now says so and Settings > Live diagnostics will advise it. (2) Kestrel itself was reading a picture of the page back from the screen every 4 to 6 seconds while you scrolled (27 times, the saving ran at 35 percent of a core, and the read-back is a call that makes the screen wait): that picture, which only shows at the next start, is now taken at most every 30 seconds, and never while a video plays or Battery Saver is on; leaving a page still saves it. Your hide-ad-spaces test proved the loading circle: two of the site's rules from the public lists change pornhub's own layout (a top margin on the page container, and the strip after the player) and hide no ad, so those two are now dropped for pornhub only; every other rule stays. If the circle is still cut, alpha88 and this one list which rule matched on that page (Diagnostics > Page report), so the next fix is exact. The video that spins forever is still not explained: your upload shows the format rule and the press-to-play rule did nothing, and the errors in it are the little preview clips of the listing, not the player; the stuck-loading probe of alpha88 will name it after one pornhub video page on alpha88 or alpha89. Not run on the tablet: everything above.

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/build.gradle.kts
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  ccdb2da4 ckpt 2869: alpha88 RELEASED and recorded (run 44962198254 green, Release has the APK). 
  4bec7a6a ckpt 2864: PERF2c lag evidence from the owner's NEW upload (234658, alpha87, Send now):
  306d0ccc ckpt 2861: Owner follow-up written to TASKS.md as PERF2a-c: hiding off = whole circle (
  bf872eae ckpt 2860: alpha88 (code 189) gated and shipped to GitHub: ONE dispatch made about 23:3
  6f8657d1 ckpt 2859: gated kestrel-g-v0.3.0-alpha88 (gecko code 189) and pushed it: handoff self-
  f02e9214 ckpt 2858: pre-ship: alpha88: what your pornhub clue shows, and two new probes. The cut
  c6fc62aa ckpt 2857: alpha87 RELEASED and recorded (run 44960366094 green, Release has the APK, B
  b34edd20 ckpt 2850: alpha87 SECOND dispatch queued 23:09Z = run 44960366094 (the first, 44958961
  8a2a9b11 ckpt 2849: alpha87 first dispatch (run 44958961031) FAILED in test-js on GitHub: pageti
  3811af55 ckpt 2848: Owner follow-up on pornhub (VD2c, in TASKS.md): Shields ON cuts off the load
```
