# CHECKPOINT 2840 — read me first, then TASKS.md

**Written:** 2026-11-21T22:24:22Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `b69ecf67` (this checkpoint is the commit after it)

## Just done
VD1c/d part 2 written (gate running): core VideoSites + test (4 pass), ShieldsBridge videoGate/videoCodec/videoError + dispatch vgh/vch/verr, BrowserActivity.onVideoSite (private/unknown/sensitive dropped, host = tab's), Health video gate/codec-no/error lines, Diagnostics 'Videos by site' line, DiagJournal keeps video gate|codec, digest VIDEO PROBLEMS by site (+3 test_handoff checks, 86/86), PrivateTabsTest canary test

## Do this next
Next: read /tmp scratchpad g2.log GRADLE_EXIT (core + app + geckoapp tests + lint; never run a 2nd gradle at once), then web-tests npm test, check-gecko-prefs.sh, bump geckoapp/build.gradle.kts to 188 / 0.3.0-alpha87, tools/sync-ext-version.sh, ALPHA87 note, ship.sh gecko, ONE dispatch, verify, record-release, tick VD1a-e, tell the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  418ba1cd ckpt 2839: VD1c/d part 1 green: per-site flag ShieldFlag.AUTOSTART (core + Shields pane
  4b9a2d4a ckpt 2838: VD1a/b done: research/VIDEO_NOLOAD.md written. Cause 1 (strongest for Reels 
  ddf1d945 ckpt 2837: VD1 written into TASKS.md in the owner's words (diagnose why Instagram reels
  cb695e57 ckpt 2836: alpha86 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/
  2f0f8b2b ckpt 2835: alpha86 gated (core + app full suites via ship.sh, lint 0 errors, test_hando
  0df22017 ckpt 2834: gated kestrel-g-v0.3.0-alpha86 (gecko code 187) and pushed it: handoff self-
  620bf412 ckpt 2833: pre-ship: alpha86: private tabs checked end to end, and every hole found is 
  253c10df ckpt 2832: PV1: audit written (research/PRIVATE_TABS.md, 26 rows), 7 holes fixed: Shiel
  dcf47bcb ckpt 2831: PV1: tests (PrivateTabsTest 12 incl. canary leak scans of ring / live journa
  8abe214c ckpt 2830: PV1 code in place and compiling: core PrivateScrub + PrivateWindow (71 core 
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
