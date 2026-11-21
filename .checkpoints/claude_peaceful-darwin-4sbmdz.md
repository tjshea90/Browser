# CHECKPOINT 2839 — read me first, then TASKS.md

**Written:** 2026-11-21T22:20:24Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `5c3b1852` (this checkpoint is the commit after it)

## Just done
VD1c/d part 1 green: per-site flag ShieldFlag.AUTOSTART (core + Shields panel row 'Videos may start by themselves' + page config bits 1024/2048 only when the site has not allowed it + muted-autoplay permission allowed for that site; core test), extension: videoprobe.js (media error probe, all frames, new manifest entry) + background vgh/vch/verr forwarders (rationed, checked); web-tests videoprobe 4/4, background 128/128

## Do this next
Next: core VideoSites (per-site table: gate/codec/error) + test; ShieldsBridge callbacks videoGate/videoCodec/videoError + dispatch vgh/vch/verr; BrowserActivity handlers (Io.main, host from the page url, never private) -> Health.log video gate/codec-no/error + Diagnostics line; DiagJournal important 'video gate|codec-no'; digest VIDEO PROBLEMS by site (verdict per cause in research/VIDEO_NOLOAD.md); app tests (PrivateTabsTest canary for the new lines); then gate, alpha87

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  4b9a2d4a ckpt 2838: VD1a/b done: research/VIDEO_NOLOAD.md written. Cause 1 (strongest for Reels 
  ddf1d945 ckpt 2837: VD1 written into TASKS.md in the owner's words (diagnose why Instagram reels
  cb695e57 ckpt 2836: alpha86 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/
  2f0f8b2b ckpt 2835: alpha86 gated (core + app full suites via ship.sh, lint 0 errors, test_hando
  0df22017 ckpt 2834: gated kestrel-g-v0.3.0-alpha86 (gecko code 187) and pushed it: handoff self-
  620bf412 ckpt 2833: pre-ship: alpha86: private tabs checked end to end, and every hole found is 
  253c10df ckpt 2832: PV1: audit written (research/PRIVATE_TABS.md, 26 rows), 7 holes fixed: Shiel
  dcf47bcb ckpt 2831: PV1: tests (PrivateTabsTest 12 incl. canary leak scans of ring / live journa
  8abe214c ckpt 2830: PV1 code in place and compiling: core PrivateScrub + PrivateWindow (71 core 
  985f693e ckpt 2829: PV1 written into TASKS.md in the owner's words (private tabs: nothing saved 
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
