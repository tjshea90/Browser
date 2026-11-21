# CHECKPOINT 2850 — read me first, then TASKS.md

**Written:** 2026-11-21T23:09:51Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `8a2a9b11` (this checkpoint is the commit after it)

## Just done
alpha87 SECOND dispatch queued 23:09Z = run 44960366094 (the first, 44958961031, failed test-js, built nothing). Do NOT dispatch again unless this one fails too

## Do this next
Next: wait for run 44960366094 (all jobs: test-jvm, test-js, build-gecko), then mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha87 shows the APK, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha87 with the note from docs/handoff/ALPHA87_RELEASE_NOTE.txt. Meanwhile (no release files): alpha88 work for VD2c/GL1: videoprobe stall report + a per-page 'cosmetic hid' count of which site rules matched elements

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  8a2a9b11 ckpt 2849: alpha87 first dispatch (run 44958961031) FAILED in test-js on GitHub: pageti
  3811af55 ckpt 2848: Owner follow-up on pornhub (VD2c, in TASKS.md): Shields ON cuts off the load
  7579817e ckpt 2847: alpha87 (code 188) gated and pushed, ONE android.yml dispatch made at about 
  85221da4 ckpt 2846: gated kestrel-g-v0.3.0-alpha87 (gecko code 188) and pushed it: handoff self-
  bf9a7857 ckpt 2845: pre-ship: alpha87: why videos do not load, and a test you can run today. I c
  a3480ef8 ckpt 2844: Digest FILE PICKS section (+2 checks, handoff 88/88); OwnerRulesTest pins up
  0a054723 ckpt 2843: IMG1 picked-file facts written (core FilePickFacts + FilePickStats.lastPick 
  39f5dc40 ckpt 2842: Research notes written for the 3 new owner requests (IMG1_UPLOAD.md, VD2 sec
  a0c4325f ckpt 2841: Owner sent 3 more requests mid-gate (IMG1 screenshots 'can't process images'
  6ed63137 ckpt 2840: VD1c/d part 2 written (gate running): core VideoSites + test (4 pass), Shiel
```
