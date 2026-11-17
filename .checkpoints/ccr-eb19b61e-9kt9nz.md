# CHECKPOINT 2584 — read me first, then TASKS.md

**Written:** 2026-11-17T00:38:50Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-eb19b61e-9kt9nz` · **builds on:** `75920d6d` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha62 (gecko code 163) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-eb19b61e-9kt9nz, inputs {"app":"gecko","full_build":"true","note":"alpha62: your message 61 (a video page played an advertisement and you could not get to the real video). Cause found by reading the code: an advertisement before a film plays on the same video box as the film, so when you pressed play Kestrel's own player was given the ADVERTISEMENT; the page's own ad code never saw its ad finish, so it never went on to the film, and pressing play again gave Kestrel's player the same advertisement again. Fixed three ways: (1) when the page's player says an ad is playing, Kestrel's player does not take it, the page plays its own ad and then the film; if the film is already known it goes to Kestrel's player straight away. (2) A video address that names an advertisement (ads, advert, preroll, vast and similar words) is left to the page too. (3) Safety net for ads nobody could recognise: a short clip (up to 2 minutes) that Kestrel's player played to its end is not handed over a second time, so press play again and the page plays it and moves on to the real video. Cost: an ad Kestrel could not recognise plays once in Kestrel's player and a second time in the page. NOT tested on your tablet or on a real site: the Firefox engine in an Android 10 emulator played two test pages with an advertisement before the film on the same video box (one that says it is an ad: not taken over, the page plays the ad then the film; one that does not: taken once, then the second press goes to the page and the film plays), plus the automatic test suites. Not run on a real site or a real ad. If it still happens, tell me the site name."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha62), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha62 "alpha62: your message 61 (a video page played an advertisement and you could not get to the real video). Cause found by reading the code: an advertisement before a film plays on the same video box as the film, so when you pressed play Kestrel's own player was given the ADVERTISEMENT; the page's own ad code never saw its ad finish, so it never went on to the film, and pressing play again gave Kestrel's player the same advertisement again. Fixed three ways: (1) when the page's player says an ad is playing, Kestrel's player does not take it, the page plays its own ad and then the film; if the film is already known it goes to Kestrel's player straight away. (2) A video address that names an advertisement (ads, advert, preroll, vast and similar words) is left to the page too. (3) Safety net for ads nobody could recognise: a short clip (up to 2 minutes) that Kestrel's player played to its end is not handed over a second time, so press play again and the page plays it and moves on to the real video. Cost: an ad Kestrel could not recognise plays once in Kestrel's player and a second time in the page. NOT tested on your tablet or on a real site: the Firefox engine in an Android 10 emulator played two test pages with an advertisement before the film on the same video box (one that says it is an ad: not taken over, the page plays the ad then the film; one that does not: taken once, then the second press goes to the page and the film plays), plus the automatic test suites. Not run on a real site or a real ad. If it still happens, tell me the site name."
NEVER ship kestrel-g-v0.3.0-alpha62 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  75920d6d ckpt 2583: ANS61a-c ticked in TASKS.md with evidence: lab ho run 43986542855 = 61 check
  eb522196 ckpt 2582: alpha62 prepared but NOT shipped: geckoapp versionCode 163 / 0.3.0-alpha62, 
  241f219d ckpt 2581: lab ho run 43986542855 DISPATCHED (head 977ab6a8, app=lab lab_suite=ho): pro
  977ab6a8 ckpt 2580: ANS61c: lab variants preroll (ad-state class, ad set up before the press) + 
  25e3ced5 ckpt 2579: ANS61: :core:test 1413/0 failed (OwnerRulesTest guard added), :geckoapp:comp
  a86c1e78 ckpt 2578: ANS61b core+app+page done: WebHandOver ad rules (Element.ad, looksLikeAdAddr
  59855a57 ckpt 2577: ANS61 written into TASKS.md in the owner's words (pre-roll ad played in Kest
  484824ab ckpt 2576: PRIV1 done: INBOX.md, OUTBOX.md and .handoff now live in the PRIVATE repo tj
  18ce6643 ckpt 2575: PRIV1: owner chose group 1 only (INBOX.md, OUTBOX.md, .handoff) and history 
  352c3027 ckpt 2574: PRIV1: owner changed the plan (builds on a PUBLIC repo for free minutes, sen
```
