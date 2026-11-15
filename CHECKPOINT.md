# CHECKPOINT 2367 — read me first, then TASKS.md

**Written:** 2026-11-15T02:19:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `92d40bac` (this checkpoint is the commit after it)

## Just done
wrote the owner's message 41 into TASKS.md as boxes (MN1 panel will not close / long press above it closes, MN2 drag-to-X close, MN3 answer 'did you get my prompt', HM1 tab Home screen forgotten, PT1 private box, SP1 first-start picture + quick animation, SH1 ship alpha46). alpha45 released + recorded + answered before this.

## Do this next
work the rows in this order: MN1a (why the panel does not close: reproduce on the emulator with real touches, tools/emu/glide_emu.py holddrag/tap/shot; code = MiniNav / PanelLayout in app/src/main/.../ui/MiniNav*.kt, openSpace chain BrowserActivity.onOpenSpace -> MiniNav.showAt; research/MINI_NAV.md), MN1b, MN2a, HM1a, PT1a, SP1a, then ship alpha46 (versionCode 147). Also RX4a (curl-free: node tools/sitelab/yt_live.js --ua mobile --video pQ73wo2JMb8 vs --ua desktop) for the re-sent YouTube screenshot. Claim each row with bash tools/claim.sh <ID> first; tick with --done and the proof.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  5abf8de1 ckpt 2366: alpha45 (gecko code 146) RELEASED, RECORDED in BUILDLOG.md (run 43664857352 
  ebd2f1ef ckpt 2365: alpha45 gated (JVM + web 373/373), pushed and dispatched to GitHub (workflow
  d636a48a ckpt 2364: gated kestrel-g-v0.3.0-alpha45 (gecko code 146) and pushed it: handoff self-
  c8a968e7 ckpt 2363: RX3-V4 coded + unit-proven (not ticked yet): core MediaList.applyBlob/matchB
  1dea80c7 ckpt 2362: RX3-S2 DONE and ticked (counters only): background.js LST + lstTimed around 
  d90ec3e7 ckpt 2361: alpha44 gated, pushed and dispatched (G4 fake play layers, V3 played element
  02082e19 ckpt 2360: gated kestrel-g-v0.3.0-alpha44 (gecko code 145) and pushed it: handoff self-
  072024e2 ckpt 2359: alpha43 gated, pushed and dispatched to GitHub (V5, D1, S1, B2, G5, G6); wai
  cca67983 ckpt 2358: gated kestrel-g-v0.3.0-alpha43 (gecko code 144) and pushed it: handoff self-
  18b548ee ckpt 2357: pre-ship: alpha43: the glow stops pulsing while a video plays; two more trac
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
