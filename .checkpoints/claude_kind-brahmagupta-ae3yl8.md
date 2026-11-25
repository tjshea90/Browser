# CHECKPOINT 3336 — read me first, then TASKS.md

**Written:** 2026-11-25T04:56:50Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `e096411f` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha108 (gecko code 209) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/kind-brahmagupta-ae3yl8, inputs {"app":"gecko","full_build":"true","note":"alpha108: your first Auto-tune run, acted on. WHAT THE RUN SHOWED (31 minutes on your tablet, hardware drawing trial 1 on): nothing it tried beat your settings by more than the tablet's own noise (about 5 percent), so none of your settings were changed. With trial 1 the test pages draw at 66 frames a second (the screen's limit), scrolling is smooth (17 ms frames) and typing waits 30 ms. WHAT CHANGED BECAUSE OF IT: (1) Hardware drawing trial 2 is gone from Settings: it used 3.5 times the processor time (25 s against 7 s per test pass) and WebGL stopped working; a saved 2 now counts as trial 1. (2) Calmer redrawing (30 a second) stays off: it halves animation and typing waited 43 ms instead of 30. (3) Auto-tune fixes, found by reading the run: the looping-clip test never ran a clip (your press-to-play rule blocked all 8, in all 37 passes), so the clips now start inside the test and the test says why if they do not; the time budget counted planned minutes (75) but the run took 31, so three experiments were skipped (no limit on looping clips, the Rust font reader, compile off the main thread): the budget now counts real minutes; one odd sample (13 s of processor time once, 8 s the next) rejected a setting: a loss must now show in every sample. Every setting is measured again at your next run (about 40 minutes). NOT CHANGED because the numbers did not support it: scroll feel, script clean-up, style threads, a second page process, compile settings, gradients, form fill, captcha watchers and the new Firefox 161 settings were all inside the noise, and none lowered memory. The pages that hurt most, such as claude.ai, are slowed by their own scripts (the page was too busy to answer for 90 percent of its load): no setting here can be shown to speed them up. NOT RUN on your tablet: this build, the clip test with clips really playing, and the three experiments that were skipped. STILL OPEN: is any text missing with hardware drawing trial 1 (the run did not ask because you already had it on)."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha108), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha108 "alpha108: your first Auto-tune run, acted on. WHAT THE RUN SHOWED (31 minutes on your tablet, hardware drawing trial 1 on): nothing it tried beat your settings by more than the tablet's own noise (about 5 percent), so none of your settings were changed. With trial 1 the test pages draw at 66 frames a second (the screen's limit), scrolling is smooth (17 ms frames) and typing waits 30 ms. WHAT CHANGED BECAUSE OF IT: (1) Hardware drawing trial 2 is gone from Settings: it used 3.5 times the processor time (25 s against 7 s per test pass) and WebGL stopped working; a saved 2 now counts as trial 1. (2) Calmer redrawing (30 a second) stays off: it halves animation and typing waited 43 ms instead of 30. (3) Auto-tune fixes, found by reading the run: the looping-clip test never ran a clip (your press-to-play rule blocked all 8, in all 37 passes), so the clips now start inside the test and the test says why if they do not; the time budget counted planned minutes (75) but the run took 31, so three experiments were skipped (no limit on looping clips, the Rust font reader, compile off the main thread): the budget now counts real minutes; one odd sample (13 s of processor time once, 8 s the next) rejected a setting: a loss must now show in every sample. Every setting is measured again at your next run (about 40 minutes). NOT CHANGED because the numbers did not support it: scroll feel, script clean-up, style threads, a second page process, compile settings, gradients, form fill, captcha watchers and the new Firefox 161 settings were all inside the noise, and none lowered memory. The pages that hurt most, such as claude.ai, are slowed by their own scripts (the page was too busy to answer for 90 percent of its load): no setting here can be shown to speed them up. NOT RUN on your tablet: this build, the clip test with clips really playing, and the three experiments that were skipped. STILL OPEN: is any text missing with hardware drawing trial 1 (the run did not ask because you already had it on)."
NEVER ship kestrel-g-v0.3.0-alpha108 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e096411f ckpt 3335: OPT4: alpha108 built from the evidence: hw2 gone (Settings, Prefs 0..1, Cata
  0aea0586 ckpt 3330: OPT4 written into TASKS in the owner's words (act on the Auto-tune run + all
  803695fb ckpt 3329: reviewed the owner's first real Auto-tune run (run 20261125-034537, DONE on 
  eeac4ff3 ckpt 3328: alpha107 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha107, run 455370
  788928f7 ckpt 3326: alpha107 build dispatched ONCE (run 45537054629, head d23bd30b, app=gecko fu
  d23bd30b ckpt 3325: gated kestrel-g-v0.3.0-alpha107 (gecko code 208) and pushed it: handoff self
  13f6ec0b ckpt 3324: OPT1c lab v14 read: the screen thread froze inside a native draw call right 
  62999022 ckpt 3321: OPT1c: lab opt v14 dispatched (run 45533868639, head c64696f3)
  c64696f3 ckpt 3320: OPT1c lab v13 read: clean up to the first experiment's restart, then the emu
  d05a3d57 ckpt 3318: OPT1c: lab opt v13 dispatched (run 45530873833, head 86130d11)
```
