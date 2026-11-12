# CHECKPOINT 609 — read me first, then TASKS.md

**Written:** 2026-11-12T00:19:08Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `3ed1b75` (this checkpoint is the commit after it)

## Just done
alpha14 DISPATCHED: run 43126355049 (android.yml, head 3ed1b75, started 00:18:45Z, expect green about 00:25Z). DO NOT dispatch again.

## Do this next
WAIT for run 43126355049 (mcp__github__actions_list list_workflow_jobs 43126355049; all jobs completed and build-gecko success = green). If green: (1) mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha14 shows the APK; (2) VERIFY THE RESCUE: mcp__github__list_releases perPage 8 page 1 must contain kestrel-g-v0.3.0-alpha14 (the old updaters read only the first 8); (3) bash tools/record-release.sh kestrel-g-v0.3.0-alpha14 "$(cat /tmp/claude-0/-home-user-Browser/30a5aa00-41c9-5bd3-87d1-b10052a52d84/scratchpad/note14.txt)"; (4) ckpt; (5) tell the owner (release link on its own line, the cause in plain words, what to do: Menu > Updates > Check now; if it still says up to date install the APK by hand once). Meanwhile develop X6/X7 (popup/redirect guard + DOM media scan) and X8/X10/X9.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  3ed1b75 ckpt 608: gated kestrel-g-v0.3.0-alpha14 (gecko code 113) and pushed it: handoff self-te
  b646b31 ckpt 606: X1 DONE in code and tests: updater root cause found (GitHub lists releases by 
  6400e12 ckpt 600: owner request 9 written into TASKS.md as X1-X10 in the owner's words (INBOX ha
  f1f1d48 ckpt 597: alpha13 SHIPPED and recorded (run 43123971432 green, BUILDLOG written): https:
  e860a70 ckpt 595: alpha13 DISPATCHED on GitHub Actions: run 43123971432 (android.yml, head 19e3a
  19e3a6d ckpt 594: gated kestrel-g-v0.2.0-alpha13 (gecko code 112) and pushed it: handoff self-te
  16223f8 ckpt 592: D1 code complete and verified in unit tests: real-lists differential (63,378 r
  4316802 ckpt 571: D1 steps 1+2 DONE and verified: core/.../filter/ExtImage.kt (KEX1 little-endia
  6e451c7 ckpt 555: alpha12 SHIPPED and recorded (run 43111148506 green, BUILDLOG written): https:
  66aa048 ckpt 551: alpha12 DISPATCHED on GitHub Actions: run 43111148506 (android.yml, head 77eac
```
