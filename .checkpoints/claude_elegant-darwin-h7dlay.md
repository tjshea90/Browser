# CHECKPOINT 3606 — read me first, then TASKS.md

**Written:** 2026-11-28T09:12:14Z · **tests:** 1 RED: test_handoff.sh (0 green)
**Branch:** `claude/elegant-darwin-h7dlay` · **builds on:** `6f8dd104` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha116: a small follow-up to alpha115 (alpha115, with the Proton Drive download fix, is inside this build, so install this one even if you have not installed alpha115). WHAT CHANGED: (1) A download that fails now leaves a trace in the diagnostics: a counter for each reason and one line with the reason, the answer of the server, the site and how far it got. Your failed Proton Drive download left nothing at all, so the diagnostics could not even show that downloads were failing. Downloads from private tabs leave nothing, as always. (2) A privacy slip of alpha115 is fixed: its new download counters also counted downloads from private tabs (only a number, never a site, but your rule is that a private tab leaves nothing behind). They now skip private downloads. (3) The Downloads screen no longer says In progress over a failed download: failed downloads and ones that wait for a decision have their own heading, Stopped, above the Finished ones. WHAT THE NEWEST DIAGNOSTICS SHOWED (build alpha114, 60 uploads): nothing new beyond the known slow spots: the first page after a cold start takes 16 to 19 seconds, claude.ai is limited by its own thousand small scripts, 15 engine kills (12 while the app was away), and the first start after an update ended by itself in 27 of 27 updates (alpha114 and alpha115 changed how Kestrel steps aside during an update; the next update shows whether that worked). The notes are in research/ND6_DIAGNOSTICS_2026-11-28.md. NOT RUN: nothing of this was run on the tablet; the proof is the tests of the download engine and the build checks.

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6a134cd1 ckpt 3604: ND6a-B/C in the tree (not released): Downloads screen 'Stopped' heading for 
  ce3957d6 ckpt 3600: alpha115 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha115 (gecko code 216, r
  0ae06bdc ckpt 3594: ND6a-A DONE in the tree (not released): Engine.Listener.onFailed(DownloadVie
  439bcc1b ckpt 3589: alpha115 (gecko code 216) gated green by ship.sh and DISPATCHED: android.yml
  aae77d3c ckpt 3588: gated kestrel-g-v0.3.0-alpha115 (gecko code 216) and pushed it: handoff self
  165f5255 ckpt 3586: pre-ship: alpha115: downloads that a page makes itself, like Proton Drive, n
  f47e6119 ckpt 3582: PDL1 CODED (not yet released): core Handed.kt (HandedStreams registry, Hande
  3261ff02 ckpt 3569: Second owner message (ND6: after PDL1, look for improvements from sent diagn
  59778a61 ckpt 3567: Owner message (Proton Drive zip download fails in Kestrel with 'The site sen
  a50908c2 ckpt 3539: alpha114 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha114, run 460786
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
