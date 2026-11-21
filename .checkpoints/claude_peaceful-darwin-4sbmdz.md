# CHECKPOINT 2833 — read me first, then TASKS.md

**Written:** 2026-11-21T19:55:04Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `253c10df` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha86: private tabs checked end to end, and every hole found is closed. Most of it was already safe (history, saved tabs, page pictures, the downloads list, keyboard words, the Recents picture, warm-up connections), but I found seven places where something from a private tab could be saved or sent. (1) Changing Shields for a site from a private tab saved that site's name on the tablet; now the change lasts only while a private tab is open and is never written or backed up. (2) The Diagnostics Engine line printed the address of the front page even in a private tab, and the live diagnostics could have uploaded it to your private GitHub repository; now nothing is built or sent while a private tab is open or for two minutes after the last one closes. (3) Other diagnostic lines could name sites; now a safety net hides every site name and address in them while a private tab exists, and private tabs are not even counted. (4) The engine's crash reports kept a memory copy and the page address for up to 7 days; they are now deleted or cut at start and after every engine crash. (5) The keyboard learned where your thumb lands even in a private tab; not any more. (6) The find-in-page box and the video rename box now tell the keyboard not to learn. (7) Firefox's Safe Browsing asks Google about a short code of an address, in private tabs too; it is now paused while a private tab exists, which also means regular tabs have no malware or phishing address check during that time. What cannot be closed: the website you visit and its helpers see your visit, DNS lookups, your phone hotspot carries all traffic, and the Firefox engine's own private storage is relied on rather than tested here. Diagnostics now has a Private tabs line that shows the safety net working. NOT run on the tablet.

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/build.gradle.kts
     M geckoapp/src/main/assets/kestrel-shields/manifest.json
    ?? docs/handoff/ALPHA86_RELEASE_NOTE.txt

## Last ten checkpoints
```
  253c10df ckpt 2832: PV1: audit written (research/PRIVATE_TABS.md, 26 rows), 7 holes fixed: Shiel
  dcf47bcb ckpt 2831: PV1: tests (PrivateTabsTest 12 incl. canary leak scans of ring / live journa
  8abe214c ckpt 2830: PV1 code in place and compiling: core PrivateScrub + PrivateWindow (71 core 
  985f693e ckpt 2829: PV1 written into TASKS.md in the owner's words (private tabs: nothing saved 
  ede36761 ckpt 2828: alpha85 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/
  0f2cd29a ckpt 2827: alpha85 gated (core 1991 incl. FLK1 flake passing alone, app 478, lint 0 err
  b9f6b0b0 ckpt 2826: gated kestrel-g-v0.3.0-alpha85 (gecko code 186) and pushed it: handoff self-
  495ef4db ckpt 2825: pre-ship: alpha85: the live diagnostics now explain WHY a page is slow, whic
  4cf115af ckpt 2824: LD4 gate so far: core 1991 tests (1 failed = FLK1 PhoneBackendTest flake, pa
  a1863e08 ckpt 2823: LD4c done: diag_digest.py LOAD ANALYSIS (per site: loads, load/first paint/f
```
