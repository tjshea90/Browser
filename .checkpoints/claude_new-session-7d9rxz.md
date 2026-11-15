# CHECKPOINT 2370 — read me first, then TASKS.md

**Written:** 2026-11-15T03:17:01Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `95cc5c99` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha46 (gecko code 147) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/new-session-7d9rxz, inputs {"app":"gecko","full_build":"true","note":"alpha46: the floating navigation panel can be closed again: a long press on an empty spot ABOVE it closes it (that spot used to count as a press on the panel itself, which is why it never reacted), tapping the little bar on top of it is more forgiving, and while you carry the panel a large X appears at the bottom of the screen: let go over the X and the panel closes and stays hidden until you long press somewhere to open it again; a tab you left on the Home screen comes back as the Home screen when you return to it; the Private box at the top of the tab selector disappears when there are no private tabs, and the regular tabs page shows; when Kestrel starts it shows the app's own symbol (the shield with the bird from the app drawer) instead of a plain shield, with a very short glow (about a third of a second) as the screen comes up, and the Home screen wears the same symbol. Tested on the emulator and in unit tests, not on your tablet."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha46), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha46 "alpha46: the floating navigation panel can be closed again: a long press on an empty spot ABOVE it closes it (that spot used to count as a press on the panel itself, which is why it never reacted), tapping the little bar on top of it is more forgiving, and while you carry the panel a large X appears at the bottom of the screen: let go over the X and the panel closes and stays hidden until you long press somewhere to open it again; a tab you left on the Home screen comes back as the Home screen when you return to it; the Private box at the top of the tab selector disappears when there are no private tabs, and the regular tabs page shows; when Kestrel starts it shows the app's own symbol (the shield with the bird from the app drawer) instead of a plain shield, with a very short glow (about a third of a second) as the screen comes up, and the Home screen wears the same symbol. Tested on the emulator and in unit tests, not on your tablet."
NEVER ship kestrel-g-v0.3.0-alpha46 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  95cc5c99 ckpt 2369: pre-ship: alpha46: the floating navigation panel can be closed again: a long
  42026781 ckpt 2368: owner message 41 CODED + unit-proven (emulator proof still to do, rows not t
  ea36cfba ckpt 2367: wrote the owner's message 41 into TASKS.md as boxes (MN1 panel will not clos
  5abf8de1 ckpt 2366: alpha45 (gecko code 146) RELEASED, RECORDED in BUILDLOG.md (run 43664857352 
  ebd2f1ef ckpt 2365: alpha45 gated (JVM + web 373/373), pushed and dispatched to GitHub (workflow
  d636a48a ckpt 2364: gated kestrel-g-v0.3.0-alpha45 (gecko code 146) and pushed it: handoff self-
  c8a968e7 ckpt 2363: RX3-V4 coded + unit-proven (not ticked yet): core MediaList.applyBlob/matchB
  1dea80c7 ckpt 2362: RX3-S2 DONE and ticked (counters only): background.js LST + lstTimed around 
  d90ec3e7 ckpt 2361: alpha44 gated, pushed and dispatched (G4 fake play layers, V3 played element
  02082e19 ckpt 2360: gated kestrel-g-v0.3.0-alpha44 (gecko code 145) and pushed it: handoff self-
```
