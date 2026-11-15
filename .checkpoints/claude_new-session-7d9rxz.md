# CHECKPOINT 2368 — read me first, then TASKS.md

**Written:** 2026-11-15T02:28:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `16915df8` (this checkpoint is the commit after it)

## Just done
owner message 41 CODED + unit-proven (emulator proof still to do, rows not ticked yet): MN1b long press above the open panel closes it (MiniNav.showAt/isAbovePanel), MN1a grip tap forgiving (600 ms, 1.5 slop; old limit 250 ms), MN2a large X (84 dp) at the bottom while the panel is carried, let go over it = closed + hidden (CloseTarget view, watchCloseTarget), MiniNavTest +7 (94 -> all green, one old test updated to the new rule), OwnerRulesTest pins MN1/MN2 and HM1; HM1 BrowserActivity.show() no longer clears tab.showHome (the line existed since the first Home overlay); PT1 TabGrid hides the Private box when there are no private tabs and falls back to the regular pane (TabGridPrivateBoxTest 4). SP1 not started: launch_window.xml shows launch_shield (flat shield); the app drawer icon is ic_launcher_foreground (shield + falcon, gradients).

## Do this next
1) SP1: (a) launch_window.xml: use ic_launcher_foreground (148 dp) instead of launch_shield; (b) a short own-clock logo flourish over the first frame (the owner runs with Android animations OFF: NO system animators; same approach as MiniNav slide: postOnAnimation + uptimeMillis): pure curve math in core (LaunchAnim, tests), View LaunchLogo in app/ui, added in BrowserActivity.onCreate only when savedInstanceState == null, never takes a touch, removes itself in <= 450 ms; 2) emulator (booted at 02:25Z, adb may need a minute): build ./gradlew --offline :geckoapp:assembleDebug -PgeckoAbi=x86_64, adb install -r -t, pm clear is NOT needed (no extension change), then real touches with tools/emu/glide_emu.py (longpress / holddrag / tap / shot) for MN1/MN2, a Home-then-other-tab-then-back run for HM1, close the last private tab for PT1, early screencaps for SP1; 3) tick MN1a/b MN2a HM1a PT1a SP1a with claim.sh --done, full gates, alpha46 (versionCode 147), answer the owner (MN3: first time the X request reached me).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ea36cfba ckpt 2367: wrote the owner's message 41 into TASKS.md as boxes (MN1 panel will not clos
  5abf8de1 ckpt 2366: alpha45 (gecko code 146) RELEASED, RECORDED in BUILDLOG.md (run 43664857352 
  ebd2f1ef ckpt 2365: alpha45 gated (JVM + web 373/373), pushed and dispatched to GitHub (workflow
  d636a48a ckpt 2364: gated kestrel-g-v0.3.0-alpha45 (gecko code 146) and pushed it: handoff self-
  c8a968e7 ckpt 2363: RX3-V4 coded + unit-proven (not ticked yet): core MediaList.applyBlob/matchB
  1dea80c7 ckpt 2362: RX3-S2 DONE and ticked (counters only): background.js LST + lstTimed around 
  d90ec3e7 ckpt 2361: alpha44 gated, pushed and dispatched (G4 fake play layers, V3 played element
  02082e19 ckpt 2360: gated kestrel-g-v0.3.0-alpha44 (gecko code 145) and pushed it: handoff self-
  072024e2 ckpt 2359: alpha43 gated, pushed and dispatched to GitHub (V5, D1, S1, B2, G5, G6); wai
  cca67983 ckpt 2358: gated kestrel-g-v0.3.0-alpha43 (gecko code 144) and pushed it: handoff self-
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
