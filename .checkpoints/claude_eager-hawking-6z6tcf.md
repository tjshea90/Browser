# CHECKPOINT 3526 — read me first, then TASKS.md

**Written:** 2026-11-27T22:32:26Z · **tests:** 1 RED: test_handoff.sh (0 green)
**Branch:** `claude/eager-hawking-6z6tcf` · **builds on:** `86aee466` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha114: Kestrel steps out of the way when you update, and the page process finally showed itself (alpha113 is inside this build). WHAT THE NEW DATA SHOWED: (1) Your report that the app still crashes after an update: in all 26 updates since alpha75 the first process after the update ended by itself 14 to 35 seconds after the install (median 25), with no crash note and plenty of free memory, and the process after it always lived. The old alpha112 also crashed once during the update itself (the keep-ready bug that alpha113 fixed). The timing says the clock starts with the install, not with Kestrel's own start: Android's installer dialog sits on top of Kestrel while it replaces the app; Android restarts Kestrel behind the dialog and ends it again when the install is done. (2) Your third recording had the page process in it: claude.ai's page process is 100 percent busy for the whole 20 seconds of the load; only about 29 percent of that is the site's own code, the rest is Firefox's work on about a thousand small script modules plus garbage collection. Nothing of Kestrel's is more than about 3 percent of it. (3) Text slow right now: the typing counters in your 22:13 upload were fine (the page answered a key in 26 ms on average), but right after a recording the page can feel slow for a minute, because the recording asks the engine to give memory back and the engine then throws away compiled code and compiles it again. WHAT CHANGED: (1) Kestrel goes to the background just before Android's installer screen opens, so nothing of it is behind the dialog to be restarted and ended. After the update, open Kestrel once, from the launcher or the installer's Open button. NOT PROVEN ON THE TABLET: that this removes the ending. If the first start after this update still ends, the diagnostics now record whether an install was still running at that moment and when the installer said it was finished, which names the cause. (2) The recording's message now says the page may feel slow for a minute afterwards. (3) The diagnostics reader has a new table of first starts after installs. PLEASE: after this update open Kestrel once and tell me whether it stayed open; if text is slow, press Send now in the diagnostics while it is slow.

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  8617883a ckpt 3519: owner message after alpha113 written as TASKS ND4 (new profile; app still cr
  62f3b899 ckpt 3518: alpha113 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha113, run 460705
  f0095b38 ckpt 3513: alpha113 (code 214) gated green by ship.sh and DISPATCHED (android.yml app=g
  2012784b ckpt 3512: gated kestrel-g-v0.3.0-alpha113 (gecko code 214) and pushed it: handoff self
  5af245e5 ckpt 3511: pre-ship: alpha113: a crash fixed, and a better look inside claude.ai (alpha
  90abb06a ckpt 3499: owner message after alpha112: 'I sent a new diagnostics and a new page profi
  7198cbef ckpt 3498: alpha112 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha112, run 459619
  0dc5985c ckpt 3496: alpha112 (code 213) gated green by ship.sh and DISPATCHED (android.yml, app=
  8b21f578 ckpt 3495: gated kestrel-g-v0.3.0-alpha112 (gecko code 213) and pushed it: handoff self
  9c29320d ckpt 3493: pre-ship: alpha112: the first real look inside a page load, and a first fix 
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
