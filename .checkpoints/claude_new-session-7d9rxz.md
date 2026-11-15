# CHECKPOINT 2369 — read me first, then TASKS.md

**Written:** 2026-11-15T03:16:27Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `9f49da9b` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha46: the floating navigation panel can be closed again: a long press on an empty spot ABOVE it closes it (that spot used to count as a press on the panel itself, which is why it never reacted), tapping the little bar on top of it is more forgiving, and while you carry the panel a large X appears at the bottom of the screen: let go over the X and the panel closes and stays hidden until you long press somewhere to open it again; a tab you left on the Home screen comes back as the Home screen when you return to it; the Private box at the top of the tab selector disappears when there are no private tabs, and the regular tabs page shows; when Kestrel starts it shows the app's own symbol (the shield with the bird from the app drawer) instead of a plain shield, with a very short glow (about a third of a second) as the screen comes up, and the Home screen wears the same symbol. Tested on the emulator and in unit tests, not on your tablet.

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/build.gradle.kts
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  42026781 ckpt 2368: owner message 41 CODED + unit-proven (emulator proof still to do, rows not t
  ea36cfba ckpt 2367: wrote the owner's message 41 into TASKS.md as boxes (MN1 panel will not clos
  5abf8de1 ckpt 2366: alpha45 (gecko code 146) RELEASED, RECORDED in BUILDLOG.md (run 43664857352 
  ebd2f1ef ckpt 2365: alpha45 gated (JVM + web 373/373), pushed and dispatched to GitHub (workflow
  d636a48a ckpt 2364: gated kestrel-g-v0.3.0-alpha45 (gecko code 146) and pushed it: handoff self-
  c8a968e7 ckpt 2363: RX3-V4 coded + unit-proven (not ticked yet): core MediaList.applyBlob/matchB
  1dea80c7 ckpt 2362: RX3-S2 DONE and ticked (counters only): background.js LST + lstTimed around 
  d90ec3e7 ckpt 2361: alpha44 gated, pushed and dispatched (G4 fake play layers, V3 played element
  02082e19 ckpt 2360: gated kestrel-g-v0.3.0-alpha44 (gecko code 145) and pushed it: handoff self-
  072024e2 ckpt 2359: alpha43 gated, pushed and dispatched to GitHub (V5, D1, S1, B2, G5, G6); wai
```

(36 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
