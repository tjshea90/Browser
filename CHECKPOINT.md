# CHECKPOINT 2892 — read me first, then TASKS.md

**Written:** 2026-11-22T03:51:04Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `4e9d13f6` (this checkpoint is the commit after it)

## Just done
alpha90 upload read (PERF4f in TASKS): typing keys reach the page fast (13 ms address, 7 ms page); photo files are fine (2 PNG readable) so the upload failure is inside claude.ai; pornhub 720p is 60 fps with 40% dropped frames (rule off = no hand-over to Kestrel's player); pornhub watch page hides .watchpageAd x3 (the geometry suspect). alpha91 plan in PERF4g

## Do this next
Next: implement alpha91: CosmeticFixes drops (.watchpageAd, #mobileContainer ... has(> .watchpageAd), .mgp_overlayContainer, the .clear.hd:has(.adsbytrafficjunky) strip, .middleAdContainer, .noBottom, .ad-tabSplit) for pornhub + test; hidden-rules second pass at 12 s sending only new selectors; page typing response probe (rAF wait) in content.js/background/bridge/BrowserActivity/digest; gate; alpha91; answer owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  4e9d13f6 ckpt 2891: Owner clarified: 'typing here' and 'photos to this Claude chat' = claude.ai 
  ed86677d ckpt 2890: Owner after alpha90 (PERF4 in TASKS.md): photos still cannot be uploaded to 
  b02ae3d2 ckpt 2889: alpha90 RELEASED and recorded (run 44970827834 green, Release has the APK). 
  16cc4189 ckpt 2887: alpha90 (code 191) gated and shipped: ONE dispatch made right after ship.sh 
  eb28b328 ckpt 2886: gated kestrel-g-v0.3.0-alpha90 (gecko code 191) and pushed it: handoff self-
  9b7f6817 ckpt 2885: pre-ship: alpha90: finding out where the typing delay and the stuck video re
  a4f81831 ckpt 2883: PERF3 findings from the owner's alpha88/89 uploads: Battery Saver is OFF now
  67c4ba53 ckpt 2875: Owner after alpha89: video still stuck, typing very laggy, loading animation
  df05f162 ckpt 2874: alpha89 RELEASED and recorded (run 44964790281 green, Release has the APK). 
  69bc7341 ckpt 2872: alpha89 (code 190) gated and shipped: ONE dispatch made right after ship.sh 
```
