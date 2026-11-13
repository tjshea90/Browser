# CHECKPOINT 1719 — read me first, then TASKS.md

**Written:** 2026-11-13T21:49:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `e9da0f9` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha32 (gecko code 131) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha32 (includes alpha31): 1) The video list only shows videos of the page you are looking at, and when you switch servers on a video site the old download choices are replaced by the new server's. 2) Every download now shows a Download started message at the top and a small progress window in the top right with pause and resume, a bar, an X that cancels and deletes the unfinished file, and an X on the window that closes it without stopping anything. 3) The Downloads screen has a taller bar with the numbers (MB done, MB left, percent, speed) written inside it, readable on the empty and the filled part. 4) Cancelling any download deletes the partly downloaded file. 5) Videos get names like Ozark S02E05 when the page says what it is. 6) Diagnosis tools that were no longer needed are gone (tap lab, decode lab, scroll test, blocker switch and others). 7) Video downloads that say the link expired now try again by themselves and with other headers before giving up. 8) The video list keeps the real video when a page loads many previews, and Diagnostics shows a start-up timeline line."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha32), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha32 "alpha32 (includes alpha31): 1) The video list only shows videos of the page you are looking at, and when you switch servers on a video site the old download choices are replaced by the new server's. 2) Every download now shows a Download started message at the top and a small progress window in the top right with pause and resume, a bar, an X that cancels and deletes the unfinished file, and an X on the window that closes it without stopping anything. 3) The Downloads screen has a taller bar with the numbers (MB done, MB left, percent, speed) written inside it, readable on the empty and the filled part. 4) Cancelling any download deletes the partly downloaded file. 5) Videos get names like Ozark S02E05 when the page says what it is. 6) Diagnosis tools that were no longer needed are gone (tap lab, decode lab, scroll test, blocker switch and others). 7) Video downloads that say the link expired now try again by themselves and with other headers before giving up. 8) The video list keeps the real video when a page loads many previews, and Diagnostics shows a start-up timeline line."
NEVER ship kestrel-g-v0.3.0-alpha32 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e9da0f9 ckpt 1718: EY-2 (start timeline, emulator marks + Diagnostics line, research/COLD_START.
  caaf9c8 ckpt 1700: AL1-AL6 + AM1 ticked with evidence (AL5 final emulator chain: S1->S2 dropped=
  42dbcff ckpt 1683: AL5 root cause found with a debug logcat mirror of the Health events (new: He
  37e9055 ckpt 1678: AL5: found why a script-named address in a player frame outlived a server swi
  a3bd814 ckpt 1673: report saved as research/REVIEW-EXTERNAL-2.md and its TASKS block (EY-1..EY-3
  d19f54f ckpt 1672: owner message 28 written into TASKS as AN1 (review and complete all fixes and
  d14b0ab ckpt 1665: AL5 emulator lab found a real bug and it is fixed in code: after two server s
  e03327f ckpt 1661: AM1 CODE DONE: removed Tap lab, Decode lab (assets/labs 394 KB), Scroll test 
  bab3a84 ckpt 1650: owner message 27 written into TASKS as AM1 (remove finished diagnosis tools);
  a395248 ckpt 1648: AL2 coded (core/download/MiniTracker + 9 tests, ui/DownloadPopup banner + top
```
