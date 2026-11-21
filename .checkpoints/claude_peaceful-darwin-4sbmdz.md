# CHECKPOINT 2848 — read me first, then TASKS.md

**Written:** 2026-11-21T23:03:10Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `7579817e` (this checkpoint is the commit after it)

## Just done
Owner follow-up on pornhub (VD2c, in TASKS.md): Shields ON cuts off the loading circle over the video at the bottom (Shields OFF shows it whole = first confirmed example of GL1, caused by a Shields rule); the video loads forever even with Shields off AND press-to-play off, so the gate/Shields are not the cause of the endless loading. alpha87 build run 44958961031 is still running (do not dispatch again)

## Do this next
Next: (1) when run 44958961031 is green: get_release_by_tag kestrel-g-v0.3.0-alpha87 shows the APK, then tools/record-release.sh with docs/handoff/ALPHA87_RELEASE_NOTE.txt; (2) find which Shields rule cuts the spinner: evaluate the REAL default.kfs cosmetic + scriptlet rules for www.pornhub.com in a scratch core test (RealCosmeticTokenIndexTest style), look for player/loader/spinner/overlay selectors; (3) add a video STALL probe to videoprobe.js (element with src/play that sits under readyState 3 for 15 s with no error) so the next upload names it; (4) alpha88 with the fix, answer the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  7579817e ckpt 2847: alpha87 (code 188) gated and pushed, ONE android.yml dispatch made at about 
  85221da4 ckpt 2846: gated kestrel-g-v0.3.0-alpha87 (gecko code 188) and pushed it: handoff self-
  bf9a7857 ckpt 2845: pre-ship: alpha87: why videos do not load, and a test you can run today. I c
  a3480ef8 ckpt 2844: Digest FILE PICKS section (+2 checks, handoff 88/88); OwnerRulesTest pins up
  0a054723 ckpt 2843: IMG1 picked-file facts written (core FilePickFacts + FilePickStats.lastPick 
  39f5dc40 ckpt 2842: Research notes written for the 3 new owner requests (IMG1_UPLOAD.md, VD2 sec
  a0c4325f ckpt 2841: Owner sent 3 more requests mid-gate (IMG1 screenshots 'can't process images'
  6ed63137 ckpt 2840: VD1c/d part 2 written (gate running): core VideoSites + test (4 pass), Shiel
  418ba1cd ckpt 2839: VD1c/d part 1 green: per-site flag ShieldFlag.AUTOSTART (core + Shields pane
  4b9a2d4a ckpt 2838: VD1a/b done: research/VIDEO_NOLOAD.md written. Cause 1 (strongest for Reels 
```
