# CHECKPOINT 2776 — read me first, then TASKS.md

**Written:** 2026-11-21T01:20:33Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `79a107c9` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha79 (gecko code 180) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/sweet-gates-gr3bj5, inputs {"app":"gecko","full_build":"true","note":"alpha79: attaching a file on a web page (your Google Gemini report), a first fix that is NOT proven on your tablet. When a page asks for a file, Kestrel opens Android file chooser; if that failed for any reason it did so silently and the page just waited, which is what you saw. Changed: (1) if no app on the tablet can choose a file, Kestrel now says so on screen (No app on this tablet can pick a file. Install a file manager.) and answers the page at once, instead of leaving it stuck; (2) when the usual chooser (Get content) has no app it falls back to the second kind of chooser (Open document); (3) the list of file kinds shown is fixed: a page that accepts pictures plus .pdf or .docx used to get a chooser that showed pictures only, because extensions were dropped, so the documents were hidden; now extensions are understood, and a list Kestrel cannot read means no filter at all, so no file is hidden; (4) a page that asks for a whole folder is answered at once with a message instead of waiting; (5) on Android 11 and later the app can now see the file chooser apps (it declared none); (6) Diagnostics has a new line File chooser: how many times a page asked, how many times the chooser opened, how many had no chooser app, how many times nothing was chosen, how many files were handed to the page, and the last thing that happened. If Gemini attachment still does nothing, open the menu, then Diagnostics, copy the line File chooser and send it: it says exactly where it stops. Quick check without that: Settings, Back up and restore, Restore opens the same kind of chooser; if that shows nothing, the tablet has no file chooser app at all. The addresses Gemini uses to upload were checked against Kestrel ad and tracker lists and are not blocked. NOT run on the tablet."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha79), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha79 "alpha79: attaching a file on a web page (your Google Gemini report), a first fix that is NOT proven on your tablet. When a page asks for a file, Kestrel opens Android file chooser; if that failed for any reason it did so silently and the page just waited, which is what you saw. Changed: (1) if no app on the tablet can choose a file, Kestrel now says so on screen (No app on this tablet can pick a file. Install a file manager.) and answers the page at once, instead of leaving it stuck; (2) when the usual chooser (Get content) has no app it falls back to the second kind of chooser (Open document); (3) the list of file kinds shown is fixed: a page that accepts pictures plus .pdf or .docx used to get a chooser that showed pictures only, because extensions were dropped, so the documents were hidden; now extensions are understood, and a list Kestrel cannot read means no filter at all, so no file is hidden; (4) a page that asks for a whole folder is answered at once with a message instead of waiting; (5) on Android 11 and later the app can now see the file chooser apps (it declared none); (6) Diagnostics has a new line File chooser: how many times a page asked, how many times the chooser opened, how many had no chooser app, how many times nothing was chosen, how many files were handed to the page, and the last thing that happened. If Gemini attachment still does nothing, open the menu, then Diagnostics, copy the line File chooser and send it: it says exactly where it stops. Quick check without that: Settings, Back up and restore, Restore opens the same kind of chooser; if that shows nothing, the tablet has no file chooser app at all. The addresses Gemini uses to upload were checked against Kestrel ad and tracker lists and are not blocked. NOT run on the tablet."
NEVER ship kestrel-g-v0.3.0-alpha79 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  79a107c9 ckpt 2775: ATT1: file-chooser hardening coded + tested (core FilePickRulesTest + OwnerR
  299f3c4b ckpt 2774: ATT1 written into TASKS.md in the owner's words (new owner message: attachin
  9a0d1e24 ckpt 2773: alpha78 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha78, gecko code 179, run
  c8ad5f40 ckpt 2772: REL78b: alpha78 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  74fab19a ckpt 2771: gated kestrel-g-v0.3.0-alpha78 (gecko code 179) and pushed it: handoff self-
  afb6d504 ckpt 2770: YTC2: cause NOT proven. Read every tap path again (ControlsMachine, tap matc
  2ffbb683 ckpt 2769: YTC2 written into TASKS.md in the owner's words: alpha77 did not change the 
  36547fc5 ckpt 2768: alpha77 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha77, gecko code 178, run
  3f0787f7 ckpt 2767: REL77b: alpha77 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  2a4e281c ckpt 2766: gated kestrel-g-v0.3.0-alpha77 (gecko code 178) and pushed it: handoff self-
```
