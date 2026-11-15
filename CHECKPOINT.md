# CHECKPOINT 2372 — read me first, then TASKS.md

**Written:** 2026-11-15T03:24:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `e7845778` (this checkpoint is the commit after it)

## Just done
alpha46 (gecko code 147) RELEASED + RECORDED (run 43671662558 green, APK on the Release, certificate verified, built from c1e9d89b); owner message 41 fully done and ticked (MN1 MN2 MN3 HM1 PT1 SP1 SH1); the answer to the owner is the reply that follows. No heartbeat exists (deleted after alpha45).

## Do this next
NOTHING is running or half-done. WAIT for the owner: BK5 (Back instant for no-store pages on chosen sites, YES/NO), AX1g (separate system keyboard app YES/NO), tablet feedback on alpha40-46 (especially: does a long press above the panel close it, the X, Home tab after switching, Private box, the launch picture/glow), ONE Diagnostics copy after a Proton Mail session (Blocker line 'Extension listeners' decides RX3-S2b; the new Health lines 'panel long-press / open-space' show what a long press did to the panel), RX4 (YouTube Movies & TV screenshot: ask for the Desktop site check). Without owner input, doable if asked: RX3-S4 (blocker histogram), RX3-D3, RX3-Y4, a second independent review of content.js/scriptlets-main.js (ask for a review BEFORE shipping page-wide scripts). Release recipe: bump versionCode 148 + versionName, bash tools/sync-ext-version.sh, JVM gates (stop other gradle runs first: overlapping gradle runs produce bogus EXIT 1), bash ship.sh gecko NOTE, dispatch android.yml, record-release.sh AFTER green.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ba09cf6f ckpt 2371: alpha46 (gecko code 147) gated (JVM: core, tools, app incl. MiniNavTest 95, 
  c1e9d89b ckpt 2370: gated kestrel-g-v0.3.0-alpha46 (gecko code 147) and pushed it: handoff self-
  95cc5c99 ckpt 2369: pre-ship: alpha46: the floating navigation panel can be closed again: a long
  42026781 ckpt 2368: owner message 41 CODED + unit-proven (emulator proof still to do, rows not t
  ea36cfba ckpt 2367: wrote the owner's message 41 into TASKS.md as boxes (MN1 panel will not clos
  5abf8de1 ckpt 2366: alpha45 (gecko code 146) RELEASED, RECORDED in BUILDLOG.md (run 43664857352 
  ebd2f1ef ckpt 2365: alpha45 gated (JVM + web 373/373), pushed and dispatched to GitHub (workflow
  d636a48a ckpt 2364: gated kestrel-g-v0.3.0-alpha45 (gecko code 146) and pushed it: handoff self-
  c8a968e7 ckpt 2363: RX3-V4 coded + unit-proven (not ticked yet): core MediaList.applyBlob/matchB
  1dea80c7 ckpt 2362: RX3-S2 DONE and ticked (counters only): background.js LST + lstTimed around 
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
