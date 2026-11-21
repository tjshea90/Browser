# CHECKPOINT 2815 — read me first, then TASKS.md

**Written:** 2026-11-21T16:31:11Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `2cf128f4` (this checkpoint is the commit after it)

## Just done
alpha84 gated (core+app tests, gecko compile, lint 0 errors, self-test 80/80) and DISPATCHED ONCE at 16:30Z on 2026-11-21: android.yml app=gecko full_build=true ref claude/peaceful-darwin-4sbmdz. DO NOT DISPATCH AGAIN.

## Do this next
Find the run id (mcp__github__actions_list list_workflow_runs android.yml, branch claude/peaceful-darwin-4sbmdz), wait for all jobs success (list_workflow_jobs; get_job_logs is 404 until the job ends), git ls-remote --tags origin for kestrel-g-v0.3.0-alpha84, get_release_by_tag shows the APK, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha84 "$(cat docs/handoff/ALPHA84_RELEASE_NOTE.txt | tr -d newline)" (identical note), delete heartbeat trig_01LyJ9xdLZhzt46J8vWq4DgX, tick LD3e, answer the owner in plain words with the Release link on its own line

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2cf128f4 ckpt 2814: gated kestrel-g-v0.3.0-alpha84 (gecko code 185) and pushed it: handoff self-
  0efd4ff7 ckpt 2813: ALPHA84 release note written (docs/handoff/ALPHA84_RELEASE_NOTE.txt); platfo
  c4318ee7 ckpt 2812: LD3 docs + TASKS ticks (a-d) written; digest prints sends automatic/manual +
  7369e028 ckpt 2811: LD3 code done and green: core DiagJournal (exact commit, honest lost/thinned
  bb443992 ckpt 2810: LD3d tools done: private.sh + diag_digest.py find the clone in any letter ca
  2015daad ckpt 2809: LD3 audit written into research/LIVE_DIAG.md from the 4 real uploads: manual
  51209a26 ckpt 2808: LD3 written into TASKS.md in the owner's words (4 checks of the live diagnos
  91a90882 ckpt 2807: alpha83 SHIPPED and RECORDED: run 44882471063 (run_number 264, head 805cabb9
  6b5bf826 ckpt 2806: alpha83 DISPATCHED once at 07:55Z on 2026-11-21 (android.yml, app=gecko, ful
  805cabb9 ckpt 2805: gated kestrel-g-v0.3.0-alpha83 (gecko code 184) and pushed it: handoff self-
```
