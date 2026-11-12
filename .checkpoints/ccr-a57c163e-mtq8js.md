# CHECKPOINT 625 — read me first, then TASKS.md

**Written:** 2026-11-12T00:31:53Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `be0036f` (this checkpoint is the commit after it)

## Just done
alpha14 SHIPPED and recorded (run 43127174913 green): https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha14 . RESCUE VERIFIED: list_releases page 1 (the old updaters' 8-entry window) shows kestrel-g-v0.3.0-alpha14 FIRST, so alpha9-13 can offer it. (GitHub's list order is not simply by tag name: alpha14 came first, then kestrel-v0.1.1, alpha9..alpha4; alpha13..alpha10 sit at the bottom - the exact rule is unknown, which is why the new updater trusts no order.) X1 DONE. Pop-up guard (X6) code is in the working tree: guard-main.js + content.js relay + background.js (MAIN-world registration, redirect guard, tap tracking) + Kotlin (Prefs.blockPopups, Settings toggle, cfg flags, ShieldsBridge 'pop', NoticeBar, BrowserActivity.onPopupBlocked/refusePopup, TabDelegates TARGET_WINDOW_NEW check); compiled OK; NOT yet run on the emulator and background.test.js has no tests for the guard yet.

## Do this next
1) Tell the owner about alpha14 (link on its own line, cause, what to do: Menu > Updates > Check now). 2) Emulator test of the guard: build debug (./gradlew :geckoapp:assembleDebug -PgeckoAbi=x86_64), pm clear, load a popups test page on port 8000 (buttons: window.open from a click on a div, dynamic anchor click, overlay link, redirect after play tap, legit target=_blank link, legit same-site window.open); CHECK Diagnostics > Blocker line for 'page guard on' (contentScripts.register world MAIN supported?) and 'sender.tab' availability (tap tracking needs sender.tab.id). 3) background.test.js tests for redirectBlocked/guard/pop. 4) then X8/X10 (floating Download chip), X9 (MP4), X7 (DOM scan), X2, X3, X5, X4.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d1af18c ckpt 615: alpha14 RE-DISPATCHED: run 43127174913 (android.yml, head 0cebcca, 00:25:31Z, 
  0cebcca ckpt 614: gated kestrel-g-v0.3.0-alpha14 (gecko code 113) and pushed it: handoff self-te
  fec7b3d ckpt 609: alpha14 DISPATCHED: run 43126355049 (android.yml, head 3ed1b75, started 00:18:
  3ed1b75 ckpt 608: gated kestrel-g-v0.3.0-alpha14 (gecko code 113) and pushed it: handoff self-te
  b646b31 ckpt 606: X1 DONE in code and tests: updater root cause found (GitHub lists releases by 
  6400e12 ckpt 600: owner request 9 written into TASKS.md as X1-X10 in the owner's words (INBOX ha
  f1f1d48 ckpt 597: alpha13 SHIPPED and recorded (run 43123971432 green, BUILDLOG written): https:
  e860a70 ckpt 595: alpha13 DISPATCHED on GitHub Actions: run 43123971432 (android.yml, head 19e3a
  19e3a6d ckpt 594: gated kestrel-g-v0.2.0-alpha13 (gecko code 112) and pushed it: handoff self-te
  16223f8 ckpt 592: D1 code complete and verified in unit tests: real-lists differential (63,378 r
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
