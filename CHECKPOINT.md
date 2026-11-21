# CHECKPOINT 2847 — read me first, then TASKS.md

**Written:** 2026-11-21T22:53:08Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `85221da4` (this checkpoint is the commit after it)

## Just done
alpha87 (code 188) gated and pushed, ONE android.yml dispatch made at about 22:56Z (workflow run not yet seen in the list; do NOT dispatch again). Gates were green: core+app tests, geckoapp compile, lint, web-tests 664/664, handoff 88/88

## Do this next
Next: find the new run (mcp__github__actions_list list_workflow_runs android.yml, branch claude/peaceful-darwin-4sbmdz, event workflow_dispatch, newest after run_number 267), wait for all jobs green, confirm get_release_by_tag kestrel-g-v0.3.0-alpha87 shows the APK, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha87 with the note in docs/handoff/ALPHA87_RELEASE_NOTE.txt, tick VD1/IMG1 boxes in TASKS.md, answer the owner: VD1 Reels, IMG1 screenshots, VD2 pornhub, GL1 undrawn parts (needs screenshots + steps). NEVER ship alpha87 twice.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  85221da4 ckpt 2846: gated kestrel-g-v0.3.0-alpha87 (gecko code 188) and pushed it: handoff self-
  bf9a7857 ckpt 2845: pre-ship: alpha87: why videos do not load, and a test you can run today. I c
  a3480ef8 ckpt 2844: Digest FILE PICKS section (+2 checks, handoff 88/88); OwnerRulesTest pins up
  0a054723 ckpt 2843: IMG1 picked-file facts written (core FilePickFacts + FilePickStats.lastPick 
  39f5dc40 ckpt 2842: Research notes written for the 3 new owner requests (IMG1_UPLOAD.md, VD2 sec
  a0c4325f ckpt 2841: Owner sent 3 more requests mid-gate (IMG1 screenshots 'can't process images'
  6ed63137 ckpt 2840: VD1c/d part 2 written (gate running): core VideoSites + test (4 pass), Shiel
  418ba1cd ckpt 2839: VD1c/d part 1 green: per-site flag ShieldFlag.AUTOSTART (core + Shields pane
  4b9a2d4a ckpt 2838: VD1a/b done: research/VIDEO_NOLOAD.md written. Cause 1 (strongest for Reels 
  ddf1d945 ckpt 2837: VD1 written into TASKS.md in the owner's words (diagnose why Instagram reels
```
