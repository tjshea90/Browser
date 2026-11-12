# CHECKPOINT 691 — read me first, then TASKS.md

**Written:** 2026-11-12T01:50:16Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `689c825` (this checkpoint is the commit after it)

## Just done
alpha15 SHIPPED+RECORDED (run 43136503549 green, tag kestrel-g-v0.3.0-alpha15, owner told). X2 coded: UpdatePlan/EngineWatch (core, tested), UpdateManager.checkEngine (ETag, tested), BrowserActivity.maybeShowUpdateNotice dialog (Update/Later/Details, Install when ready, once per news key), autoUpdateDownload default false, list refresh daily (UpdateJob period, Settings labels, 90 s front fallback). Compiles, :app tests green; dialog NOT yet seen on emulator

## Do this next
1) emulator-test X2 dialog: debug feed (adb shell setprop debug.kestrel.feed http://10.0.2.2:8000/releases.json, edit site/releases.json to offer code 115) + Flavor engine URL; 2) bump alpha16 (code 115, 0.3.0-alpha16) ship+dispatch+record; 3) X5 assessment doc (research/ADBLOCK_ENGINE.md already; add AUDIT section), 4) X3 download manager per research/DOWNLOAD_MANAGERS.md (M1 etc), 5) X12/X4 audit per research/GECKO_TUNING_2.md (measure first, preconnect speculativeConnect, typing path, Proton Mail). Owner asked: scheduled CI list-snapshot workflow needs OK (asked in last reply)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2f132e9 ckpt 682: alpha15 shipped to GitHub: workflow dispatched (run queued on ccr-a57c163e-mtq
  4b80497 ckpt 681: gated kestrel-g-v0.3.0-alpha15 (gecko code 114) and pushed it: handoff self-te
  bede833 ckpt 680: pre-ship: alpha15: pop-up and forced-redirect guard, always-visible Save video
  8e96087 ckpt 678: emulator-verified: Save-video chip opens the Download video sheet; page scan l
  e1721e0 ckpt 647: X7 page scan coded: content.js finds video/source/og:video/links/inline-script
  2c7b49a ckpt 628: owner request 10 written into TASKS.md as X11 (everything still on the list) a
  6b28616 ckpt 625: alpha14 SHIPPED and recorded (run 43127174913 green): https://github.com/tjshe
  d1af18c ckpt 615: alpha14 RE-DISPATCHED: run 43127174913 (android.yml, head 0cebcca, 00:25:31Z, 
  0cebcca ckpt 614: gated kestrel-g-v0.3.0-alpha14 (gecko code 113) and pushed it: handoff self-te
  fec7b3d ckpt 609: alpha14 DISPATCHED: run 43126355049 (android.yml, head 3ed1b75, started 00:18:
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
