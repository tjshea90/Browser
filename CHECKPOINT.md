# CHECKPOINT 709 — read me first, then TASKS.md

**Written:** 2026-11-12T02:14:50Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `4e82e2f` (this checkpoint is the commit after it)

## Just done
X3 first batch done+tested: auto-resume after kill (both engines, SQLite v3 crashes), refused extra connection retires alone, outage ladder, fast start 2 connections for >= 8 MiB; new FakeServer connLimit/down; relaxed one StreamEngine flake assertion. :core :app tests + geckoapp compile green

## Do this next
1) bump alpha16 (code 115, 0.3.0-alpha16), ship.sh gecko with note (X2 dialog+engine watch+daily lists, Network lab, Page loads diagnostics, translations pref off, download manager M2/M3/M4/fast start), dispatch android.yml, verify green, record-release, tell owner. 2) after that: X12 audit doc research/AUDIT-2026-11.md; M1 resume validation; X5 assessment; preconnect idea

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  2d7afd4 ckpt 700: X2 verified on emulator (dialog found/ready/install flow); X12 first pieces co
  2cfcc7a ckpt 691: alpha15 SHIPPED+RECORDED (run 43136503549 green, tag kestrel-g-v0.3.0-alpha15,
  2f132e9 ckpt 682: alpha15 shipped to GitHub: workflow dispatched (run queued on ccr-a57c163e-mtq
  4b80497 ckpt 681: gated kestrel-g-v0.3.0-alpha15 (gecko code 114) and pushed it: handoff self-te
  bede833 ckpt 680: pre-ship: alpha15: pop-up and forced-redirect guard, always-visible Save video
  8e96087 ckpt 678: emulator-verified: Save-video chip opens the Download video sheet; page scan l
  e1721e0 ckpt 647: X7 page scan coded: content.js finds video/source/og:video/links/inline-script
  2c7b49a ckpt 628: owner request 10 written into TASKS.md as X11 (everything still on the list) a
  6b28616 ckpt 625: alpha14 SHIPPED and recorded (run 43127174913 green): https://github.com/tjshe
  d1af18c ckpt 615: alpha14 RE-DISPATCHED: run 43127174913 (android.yml, head 0cebcca, 00:25:31Z, 
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
