# CHECKPOINT 2325 — read me first, then TASKS.md

**Written:** 2026-11-14T19:47:33Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `9d3d31c` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha40 (gecko code 141) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha40 (includes alpha39): 1) No more 'This page is slow' prompt. When a page's script runs for a long time, Kestrel now simply waits, with no question and no dialog; if a page is stuck for too long, close its tab yourself. Tried on the emulator with a page that runs a script for 50 seconds: the engine asked after about 30 seconds, Kestrel answered wait by itself, nothing was shown and the page finished. Menu > Diagnostics counts how often it waited. 2) The navigation panel (Back, Home, Forward and the address bar with its keyboard) now comes up where you long press: long press an open spot on a page (not a link, picture, video, button, text field or text) and the panel appears under your finger, anywhere on the page. A long press on a link still gives that link's options, on a picture the picture options, on text it selects the word, as before. The top right corner is no longer special. To close the panel, tap the little bar on top of it. Settings > Mini navigation panel still switches the whole thing off. 3) The panel floats: long press it (on its buttons, its address bar or its frame, not on its keys) and drag, and it follows your finger and stays exactly where you let go; it no longer jumps back to the corner. The keys keep their own long press (hold a top row letter for its digit). 4) On the panel's keyboard, press the space key and slide your finger left or right to move the text cursor, in the address bar and in a web page's text field; a quick tap still types a space. Also fixed: the neon glows could stay frozen after a long press that opened the panel, and exporting what the keyboard learned is safer on a slow tablet. Tried on the emulator with real touches: blank space, link, linked picture, picture, text, button, text field, clickable box, the Home screen, dragging the panel, tapping its top bar, sliding the space key in the address bar and in a page field, and the 50 second script. Not tried on your tablet: how the long press time and the slide distance feel under your thumb, a PDF, and a page inside a frame that has no address of its own (the panel does not open on those)."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha40), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha40 "alpha40 (includes alpha39): 1) No more 'This page is slow' prompt. When a page's script runs for a long time, Kestrel now simply waits, with no question and no dialog; if a page is stuck for too long, close its tab yourself. Tried on the emulator with a page that runs a script for 50 seconds: the engine asked after about 30 seconds, Kestrel answered wait by itself, nothing was shown and the page finished. Menu > Diagnostics counts how often it waited. 2) The navigation panel (Back, Home, Forward and the address bar with its keyboard) now comes up where you long press: long press an open spot on a page (not a link, picture, video, button, text field or text) and the panel appears under your finger, anywhere on the page. A long press on a link still gives that link's options, on a picture the picture options, on text it selects the word, as before. The top right corner is no longer special. To close the panel, tap the little bar on top of it. Settings > Mini navigation panel still switches the whole thing off. 3) The panel floats: long press it (on its buttons, its address bar or its frame, not on its keys) and drag, and it follows your finger and stays exactly where you let go; it no longer jumps back to the corner. The keys keep their own long press (hold a top row letter for its digit). 4) On the panel's keyboard, press the space key and slide your finger left or right to move the text cursor, in the address bar and in a web page's text field; a quick tap still types a space. Also fixed: the neon glows could stay frozen after a long press that opened the panel, and exporting what the keyboard learned is safer on a slow tablet. Tried on the emulator with real touches: blank space, link, linked picture, picture, text, button, text field, clickable box, the Home screen, dragging the panel, tapping its top bar, sliding the space key in the address bar and in a page field, and the 50 second script. Not tried on your tablet: how the long press time and the slide distance feel under your thumb, a PDF, and a page inside a frame that has no address of its own (the panel does not open on those)."
NEVER ship kestrel-g-v0.3.0-alpha40 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  917a332 ckpt 2320: AY2 final: content.js treats only a NEW non-empty selection as text (emulator
  097f61f ckpt 2312: AY1, AY3, AY4 ticked with evidence (emulator proof of long press matrix, pick
  ca5c468 ckpt 2300: AY2+AY4 coded and unit-tested: MiniNav.showAt, PanelLayout long press pick-up
  bad447a ckpt 2287: AY1 coded+guarded; AY2 compiled (content.js contextmenu classifier -> backgro
  9a31c5a ckpt 2282: AY1 code done (askSlowScript answers CONTINUE with no dialog, Health counter 
  1eb7220 ckpt 2270: owner message 39 written into TASKS (AY1 no slow-page prompt, AY2 long press 
  44a36fb ckpt 2266: alpha39 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha39 (gecko code 140, run 
  418e420 ckpt 2260: alpha39 DISPATCHED (AX1: glide shapes + export/import): android.yml run_workf
  db0854d ckpt 2259: gated kestrel-g-v0.3.0-alpha39 (gecko code 140) and pushed it: handoff self-t
  4c766a1 ckpt 2258: pre-ship: alpha39 (includes alpha38): The keyboard now learns the SHAPE of yo
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
