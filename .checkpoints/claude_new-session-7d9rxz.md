# CHECKPOINT 2373 — read me first, then TASKS.md

**Written:** 2026-11-15T03:27:28Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `750729b7` (this checkpoint is the commit after it)

## Just done
owner message 42 written into TASKS.md as YS1 (+YS1a find why a tap on the YouTube seek bar does not seek in Kestrel, YS1b fix, YS1c buffering from the new place only) before any code. alpha46 released + recorded + answered before this.

## Do this next
YS1a: find the cause. Ideas to test (do not assume): (1) the touch/mouse event sequence Gecko Android sends for a tap vs what YouTube's progress bar needs (headless Chromium with hasTouch + mobile UA on the real page through the sandbox proxy shows which events the page listens to; node tools/sitelab/yt_live.js shows how the proxy is used); (2) our page-world guard (guard-main.js onTap/onClick capture listeners, notePress, G4 v1 in alpha44) and content.js press record, youtube.js, scriptlets from the YouTube list lines (set-constant / json-prune / trusted-replace-fetch-response), (3) Gecko prefs in geckoapp gecko-prefs.txt (media / apz / pointer events / double-tap), (4) BrowserActivity.dispatchTouchEvent (ACTION_CANCEL sent to the page by onOpenSpace / HotCorner, pull-to-refresh), (5) a known GeckoView/Firefox Android bug (web search). Then a lab page in tools/sitelab/guardlab.py: a <video> (silent wav 300 s) with a div seek bar that listens like YouTube does and logs; run on the emulator (reboot it: emulator -avd kestrel29x64 -no-window -no-audio -no-snapshot -no-boot-anim -gpu swiftshader_indirect -no-accel; adb root; the lowest multi-touch device is used by tools/emu/glide_emu.py now).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  43ba62e0 ckpt 2372: alpha46 (gecko code 147) RELEASED + RECORDED (run 43671662558 green, APK on 
  ba09cf6f ckpt 2371: alpha46 (gecko code 147) gated (JVM: core, tools, app incl. MiniNavTest 95, 
  c1e9d89b ckpt 2370: gated kestrel-g-v0.3.0-alpha46 (gecko code 147) and pushed it: handoff self-
  95cc5c99 ckpt 2369: pre-ship: alpha46: the floating navigation panel can be closed again: a long
  42026781 ckpt 2368: owner message 41 CODED + unit-proven (emulator proof still to do, rows not t
  ea36cfba ckpt 2367: wrote the owner's message 41 into TASKS.md as boxes (MN1 panel will not clos
  5abf8de1 ckpt 2366: alpha45 (gecko code 146) RELEASED, RECORDED in BUILDLOG.md (run 43664857352 
  ebd2f1ef ckpt 2365: alpha45 gated (JVM + web 373/373), pushed and dispatched to GitHub (workflow
  d636a48a ckpt 2364: gated kestrel-g-v0.3.0-alpha45 (gecko code 146) and pushed it: handoff self-
  c8a968e7 ckpt 2363: RX3-V4 coded + unit-proven (not ticked yet): core MediaList.applyBlob/matchB
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
