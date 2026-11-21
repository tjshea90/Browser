# CHECKPOINT 2838 — read me first, then TASKS.md

**Written:** 2026-11-21T22:15:49Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `249aba1d` (this checkpoint is the commit after it)

## Just done
VD1a/b done: research/VIDEO_NOLOAD.md written. Cause 1 (strongest for Reels / script-started videos): ANS53 press-to-play rule is GLOBAL: gate-main.js holds blob: MSE sources and refuses play() without a tap, AND the engine's muted-autoplay permission is denied while it is on; counters are summed over all pages so they cannot say which site; no per-site exception existed. Others: embeds wait for a tap (EX-10), format policy fps/size/AV1 'no' (vid_fps30 is ON in the real settings), DRM, Shields A/B, hotspot, Instagram login wall. Ruled out: Save-Data header, orientation

## Do this next
VD1c/d build: (1) core ShieldFlag.AUTOSTART=256 + ShieldSettings.autostartVideo + SiteShields.applyTo + effective(); ShieldsPanel row 'Videos may start by themselves' (not dimmed by Shields master); ShieldsBridge.pageConfig bits 1024/2048 only when !autostartVideo; TabDelegates AUTOPLAY_INAUDIBLE allow when the site flag is on; tests. (2) per-site lines: background.js vgt/vcx forward page p + host; new content-side media error probe (verr) ; ShieldsBridge callbacks videoGate/videoCodec/videoError -> BrowserActivity -> Health video gate/codec-no/error (never private); DiagJournal important 'video error|gate|codec-no'; digest VIDEO PROBLEMS by site; (3) tests, gate, alpha87

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ddf1d945 ckpt 2837: VD1 written into TASKS.md in the owner's words (diagnose why Instagram reels
  cb695e57 ckpt 2836: alpha86 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/
  2f0f8b2b ckpt 2835: alpha86 gated (core + app full suites via ship.sh, lint 0 errors, test_hando
  0df22017 ckpt 2834: gated kestrel-g-v0.3.0-alpha86 (gecko code 187) and pushed it: handoff self-
  620bf412 ckpt 2833: pre-ship: alpha86: private tabs checked end to end, and every hole found is 
  253c10df ckpt 2832: PV1: audit written (research/PRIVATE_TABS.md, 26 rows), 7 holes fixed: Shiel
  dcf47bcb ckpt 2831: PV1: tests (PrivateTabsTest 12 incl. canary leak scans of ring / live journa
  8abe214c ckpt 2830: PV1 code in place and compiling: core PrivateScrub + PrivateWindow (71 core 
  985f693e ckpt 2829: PV1 written into TASKS.md in the owner's words (private tabs: nothing saved 
  ede36761 ckpt 2828: alpha85 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
