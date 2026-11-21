# CHECKPOINT 2794 — read me first, then TASKS.md

**Written:** 2026-11-21T06:58:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `68f9d24e` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha81 (gecko code 182) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/sweet-gates-gr3bj5, inputs {"app":"gecko","full_build":"true","note":"alpha81: live diagnostics for Claude (your idea: browse normally, the browser quietly records what goes wrong, Claude reads it and fixes the browser from real data). It is OFF-able and private by design. Settings, Live diagnostics (for Claude). WHAT IT NOTES: crashes and kills of the last run, freezes, slow page loads by site name, Back problems, video stalls and decoder errors, memory, pages that do not work. WHAT IS NEVER NOTED OR SENT: passwords, anything you type, cookies, page text, page titles, file or video names, card numbers, e-mail addresses, phone numbers, tokens. Web addresses are cut down to the site name only (never the path or what follows it). Banking, health, mail, government, school and sign-in or account or checkout sites are not even named. Private tabs are never named. All of that is removed ON THE TABLET before anything is kept; a line that still looks risky is dropped. WHERE IT GOES: only to your PRIVATE GitHub repository kestrel-private, with your own token (one repository, Contents read and write), typed into Settings once; it stays in this app only (not in backups, not in the APK). If the repository is ever public, nothing is sent. At most 4 small files a day, 6 hours apart (a crash sooner), only while the app is in front. THE OFF SWITCH: Settings, Live diagnostics, off = nothing is recorded, what was kept is erased at once, nothing is ever sent. Also: Send now, Erase what is stored. Without a token nothing leaves the tablet. NOT RUN ON THE TABLET and not run against the real GitHub: your first Send now is the first real test. Claude sessions read the files with tools/diag_digest.py."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha81), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha81 "alpha81: live diagnostics for Claude (your idea: browse normally, the browser quietly records what goes wrong, Claude reads it and fixes the browser from real data). It is OFF-able and private by design. Settings, Live diagnostics (for Claude). WHAT IT NOTES: crashes and kills of the last run, freezes, slow page loads by site name, Back problems, video stalls and decoder errors, memory, pages that do not work. WHAT IS NEVER NOTED OR SENT: passwords, anything you type, cookies, page text, page titles, file or video names, card numbers, e-mail addresses, phone numbers, tokens. Web addresses are cut down to the site name only (never the path or what follows it). Banking, health, mail, government, school and sign-in or account or checkout sites are not even named. Private tabs are never named. All of that is removed ON THE TABLET before anything is kept; a line that still looks risky is dropped. WHERE IT GOES: only to your PRIVATE GitHub repository kestrel-private, with your own token (one repository, Contents read and write), typed into Settings once; it stays in this app only (not in backups, not in the APK). If the repository is ever public, nothing is sent. At most 4 small files a day, 6 hours apart (a crash sooner), only while the app is in front. THE OFF SWITCH: Settings, Live diagnostics, off = nothing is recorded, what was kept is erased at once, nothing is ever sent. Also: Send now, Erase what is stored. Without a token nothing leaves the tablet. NOT RUN ON THE TABLET and not run against the real GitHub: your first Send now is the first real test. Claude sessions read the files with tools/diag_digest.py."
NEVER ship kestrel-g-v0.3.0-alpha81 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  68f9d24e ckpt 2793: LD1e: full gate green (:core:test, :app:testDebugUnitTest, :geckoapp:compile
  a6c87bc8 ckpt 2792: LD1d done: tools/diag_digest.py (crashes/kills, slow sites by host, playback
  2a79bf47 ckpt 2791: LD1c tested: LiveDiagTest 13/13 (switch off = nothing kept, switch survives 
  2050ef12 ckpt 2790: LD1c: app side written + geckoapp compiles: diag/LiveDiag.kt (master switch,
  5d077b35 ckpt 2789: LD1b: core livediag package written + tested (DiagScrub: links -> host, sens
  808aa4f6 ckpt 2787: alpha80 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha80, gecko code 181, run
  f12802a2 ckpt 2786: LD1 written into TASKS.md in the owner's words (live diagnostics engine with
  5c4edd25 ckpt 2785: REL80b: alpha80 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  0c2b1e08 ckpt 2784: gated kestrel-g-v0.3.0-alpha80 (gecko code 181) and pushed it: handoff self-
  0f713e4a ckpt 2783: BK10 PROVEN on the real engine (lab run 44873006097): no-store page + Instan
```
