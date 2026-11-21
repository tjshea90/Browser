# CHECKPOINT 2858 — read me first, then TASKS.md

**Written:** 2026-11-21T23:30:30Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `c6fc62aa` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha88: what your pornhub clue shows, and two new probes. The cut-off loading circle is Shields' doing: your alpha86 upload shows Shields blocked several of pornhub's own requests, and the bundled lists hide pornhub's player overlay container, the strip under the player and every image that has a srcset. I cannot tell from here which one cuts the circle, so every page report now lists which element-hiding rules matched something on that page and how many times (Diagnostics > Page report: hidden by Shields element-hiding rules on this page). The next upload or Diagnostics screenshot of a blank area then names the rule, and I remove just that rule. The endless loading is NOT Shields and NOT the press-to-play rule: you turned both off and it still spins, and your own numbers show the format policy refused nothing and the press-to-play rule held nothing on that visit. So alpha88 adds a probe for the case no error message ever appears: a video that tries to load for 15 seconds, gets no data and raises no error is reported with its state (stuck loading, where its file comes from, paused or not, how much was buffered), site name only, never in a private tab. One pornhub load on alpha88 should name it. Also the diagnostics digest now prints the picked-file facts from alpha87 and tells the file from the page. Two quick tests that need no new version: in the Shields panel for pornhub.com turn off only the row Hide ad spaces & cookie banners and reload (if the circle is whole, it is one of those rules); and in Settings turn off At most 30 frames a second and reload (if the film plays, that was it). Not run on the tablet: everything above.

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/build.gradle.kts
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  c6fc62aa ckpt 2857: alpha87 RELEASED and recorded (run 44960366094 green, Release has the APK, B
  b34edd20 ckpt 2850: alpha87 SECOND dispatch queued 23:09Z = run 44960366094 (the first, 44958961
  8a2a9b11 ckpt 2849: alpha87 first dispatch (run 44958961031) FAILED in test-js on GitHub: pageti
  3811af55 ckpt 2848: Owner follow-up on pornhub (VD2c, in TASKS.md): Shields ON cuts off the load
  7579817e ckpt 2847: alpha87 (code 188) gated and pushed, ONE android.yml dispatch made at about 
  85221da4 ckpt 2846: gated kestrel-g-v0.3.0-alpha87 (gecko code 188) and pushed it: handoff self-
  bf9a7857 ckpt 2845: pre-ship: alpha87: why videos do not load, and a test you can run today. I c
  a3480ef8 ckpt 2844: Digest FILE PICKS section (+2 checks, handoff 88/88); OwnerRulesTest pins up
  0a054723 ckpt 2843: IMG1 picked-file facts written (core FilePickFacts + FilePickStats.lastPick 
  39f5dc40 ckpt 2842: Research notes written for the 3 new owner requests (IMG1_UPLOAD.md, VD2 sec
```
