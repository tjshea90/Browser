# CHECKPOINT 3511 — read me first, then TASKS.md

**Written:** 2026-11-27T21:27:13Z · **tests:** 1 RED: test_handoff.sh (0 green)
**Branch:** `claude/eager-hawking-6z6tcf` · **builds on:** `3f9cf816` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha113: a crash fixed, and a better look inside claude.ai (alpha112 is inside this build). WHAT THE NEW RECORDING AND DIAGNOSTICS SHOWED: (1) alpha112's change worked where it should. In your second recording of claude.ai the main thread's queue for the script files is about a third shorter: a file now waits about 5 seconds from open to end instead of 8, and the messages through the blocker fell from 2,665 to 1,047. The page itself still needs 11 to 18 seconds to a first paint, and the reason is hidden again: the page's own process ran out of memory while writing its part of the recording, exactly as the first time. (2) A CRASH in the keep-the-page-ready feature, found in your Diagnostics: if you leave Kestrel and come back within a moment, Android 8 to 11 can close the app with the message that a service did not call startForeground. It ended one run of alpha111 that way. WHAT CHANGED: (1) The crash is fixed: the hold is only stopped after Android has really started it, and a hold that has not started yet ends itself right after it has. (2) Record a profile of this page is kinder to the page process: one thread instead of four, 20 seconds, and 2.5 seconds before the end Kestrel tells the engine to give memory back so the page process has room to write its part. The recording also tells you at once, in a message, if the page itself is missing from it. (3) The profile reader can now name the engine's own functions: it asks Mozilla's public symbol service, sending only the library name, its build id and offsets inside it, nothing about you, a page or a file. It showed that Kestrel's own blocker code is under 1 percent of the main thread, and Firefox's own hand-over around it is about a fifth. (4) The diagnostics reader no longer counts swiping the app away from the recent apps as a memory kill. NOT RUN ON THE TABLET: the crash fix (Android's own behaviour cannot be run here; the logic is tested), the new recording. PLEASE: open claude.ai and run Settings > Live diagnostics > Record a profile of this page once more, and keep Kestrel open until the message says it was sent. If it says the page is missing again, close your other tabs and try once more.

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  90abb06a ckpt 3499: owner message after alpha112: 'I sent a new diagnostics and a new page profi
  7198cbef ckpt 3498: alpha112 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha112, run 459619
  0dc5985c ckpt 3496: alpha112 (code 213) gated green by ship.sh and DISPATCHED (android.yml, app=
  8b21f578 ckpt 3495: gated kestrel-g-v0.3.0-alpha112 (gecko code 213) and pushed it: handoff self
  9c29320d ckpt 3493: pre-ship: alpha112: the first real look inside a page load, and a first fix 
  5111ed55 ckpt 3479: ND2: marker pairing fixed in tools/profile_digest.py (phase 1/2/3), network 
  cf0d3989 ckpt 3465: alpha110 AND alpha111 RELEASED AND RECORDED (BUILDLOG lines 211 and 212). ND
  9a47bf75 ckpt 3461: alpha111 RE-DISPATCHED ONCE after fixes: run 45954070324 (run number 313, he
  240256a0 ckpt 3460: gated kestrel-g-v0.3.0-alpha111 (gecko code 212) and pushed it: handoff self
  3382c4f8 ckpt 3457: alpha111 DISPATCHED ONCE: run 45952674368 (run number 312, head 84d5657e, ap
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
