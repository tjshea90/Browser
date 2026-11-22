# CHECKPOINT 2897 — read me first, then TASKS.md

**Written:** 2026-11-22T03:53:52Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `a8ab8760` (this checkpoint is the commit after it)

## Just done
alpha91 built, gates running (g10.log, w4.log): CosmeticFixes now drops pornhub's watch-page rules (.watchpageAd family, .mgp_overlayContainer, the ad row under the player); hidden-rules second pass at 12 s; page typing RESPONSE probe (key to next frame); unhandled promise rejections reported in the page report (photo upload on claude.ai); digest fields; note docs/handoff/ALPHA91_RELEASE_NOTE.txt

## Do this next
Next: g10 GRADLE_EXIT=0 + w4 WEB_EXIT=0 (re-run a flaky file alone if load made it fail) -> bump 192 / 0.3.0-alpha91, sync-ext-version, ship.sh gecko with the ALPHA91 note, ONE dispatch, send_later +24 min, record-release; tell the owner: turn press-to-play rule back ON and press play on pornhub to test the hand-over to Kestrel's player; try the photo upload again then Send now

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  237a0bc2 ckpt 2892: alpha90 upload read (PERF4f in TASKS): typing keys reach the page fast (13 m
  4e9d13f6 ckpt 2891: Owner clarified: 'typing here' and 'photos to this Claude chat' = claude.ai 
  ed86677d ckpt 2890: Owner after alpha90 (PERF4 in TASKS.md): photos still cannot be uploaded to 
  b02ae3d2 ckpt 2889: alpha90 RELEASED and recorded (run 44970827834 green, Release has the APK). 
  16cc4189 ckpt 2887: alpha90 (code 191) gated and shipped: ONE dispatch made right after ship.sh 
  eb28b328 ckpt 2886: gated kestrel-g-v0.3.0-alpha90 (gecko code 191) and pushed it: handoff self-
  9b7f6817 ckpt 2885: pre-ship: alpha90: finding out where the typing delay and the stuck video re
  a4f81831 ckpt 2883: PERF3 findings from the owner's alpha88/89 uploads: Battery Saver is OFF now
  67c4ba53 ckpt 2875: Owner after alpha89: video still stuck, typing very laggy, loading animation
  df05f162 ckpt 2874: alpha89 RELEASED and recorded (run 44964790281 green, Release has the APK). 
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
