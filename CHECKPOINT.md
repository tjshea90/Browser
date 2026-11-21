# CHECKPOINT 2814 — read me first, then TASKS.md

**Written:** 2026-11-21T16:30:33Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `1413bf1c` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha84 (gecko code 185) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/peaceful-darwin-4sbmdz, inputs {"app":"gecko","full_build":"true","note":"alpha84: the live diagnostics are checked against your first real uploads and fixed. Four files from your tablet reached your private repository, so the sending, the token and the private-repository check all work. Reading them found real faults, now fixed: (1) a trimming bug could send nine lines twice and lose nine without saying so; a file now says exactly how many lines were left out. (2) Lines that repeat all day (video quality every 12 seconds) pushed the important ones out; they are now thinned to 40 examples per upload and still counted, while crashes, kills, slow pages, stalls and errors are never thinned. (3) Every run now starts with a dated marker line, so Claude can tell runs apart and date them. (4) The automatic send was only asked once, two minutes after the app came to the front; it is now asked again while you keep the browser open, and your Send now button no longer uses up the four automatic sends of the day. Each file now says whether it was sent by your button or by itself, so the next files prove the automatic path. (5) An upload can never replace or delete an earlier one: every upload is a new file with no old file named, a name clash gets the next free name, and only one send runs at a time; the git history of your private repository shows four files, each added once. (6) Suggested changes now look at the last 7 days instead of one upload, so they can actually appear. Claude sessions can now find the private repository whatever its capital letters and read the newest files straight from GitHub. As before: only your private repository, only your token, the master switch off means nothing recorded, kept or sent. NOT run on the tablet and not against the real GitHub from here: the first automatic send is yours to produce."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha84), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha84 "alpha84: the live diagnostics are checked against your first real uploads and fixed. Four files from your tablet reached your private repository, so the sending, the token and the private-repository check all work. Reading them found real faults, now fixed: (1) a trimming bug could send nine lines twice and lose nine without saying so; a file now says exactly how many lines were left out. (2) Lines that repeat all day (video quality every 12 seconds) pushed the important ones out; they are now thinned to 40 examples per upload and still counted, while crashes, kills, slow pages, stalls and errors are never thinned. (3) Every run now starts with a dated marker line, so Claude can tell runs apart and date them. (4) The automatic send was only asked once, two minutes after the app came to the front; it is now asked again while you keep the browser open, and your Send now button no longer uses up the four automatic sends of the day. Each file now says whether it was sent by your button or by itself, so the next files prove the automatic path. (5) An upload can never replace or delete an earlier one: every upload is a new file with no old file named, a name clash gets the next free name, and only one send runs at a time; the git history of your private repository shows four files, each added once. (6) Suggested changes now look at the last 7 days instead of one upload, so they can actually appear. Claude sessions can now find the private repository whatever its capital letters and read the newest files straight from GitHub. As before: only your private repository, only your token, the master switch off means nothing recorded, kept or sent. NOT run on the tablet and not against the real GitHub from here: the first automatic send is yours to produce."
NEVER ship kestrel-g-v0.3.0-alpha84 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  0efd4ff7 ckpt 2813: ALPHA84 release note written (docs/handoff/ALPHA84_RELEASE_NOTE.txt); platfo
  c4318ee7 ckpt 2812: LD3 docs + TASKS ticks (a-d) written; digest prints sends automatic/manual +
  7369e028 ckpt 2811: LD3 code done and green: core DiagJournal (exact commit, honest lost/thinned
  bb443992 ckpt 2810: LD3d tools done: private.sh + diag_digest.py find the clone in any letter ca
  2015daad ckpt 2809: LD3 audit written into research/LIVE_DIAG.md from the 4 real uploads: manual
  51209a26 ckpt 2808: LD3 written into TASKS.md in the owner's words (4 checks of the live diagnos
  91a90882 ckpt 2807: alpha83 SHIPPED and RECORDED: run 44882471063 (run_number 264, head 805cabb9
  6b5bf826 ckpt 2806: alpha83 DISPATCHED once at 07:55Z on 2026-11-21 (android.yml, app=gecko, ful
  805cabb9 ckpt 2805: gated kestrel-g-v0.3.0-alpha83 (gecko code 184) and pushed it: handoff self-
  6eac0f1a ckpt 2804: LD2e: full gate green (:core:test :app:testDebugUnitTest :geckoapp:compileDe
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
