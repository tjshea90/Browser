# CHECKPOINT 2857 — read me first, then TASKS.md

**Written:** 2026-11-21T23:27:34Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `4e95c1db` (this checkpoint is the commit after it)

## Just done
alpha87 RELEASED and recorded (run 44960366094 green, Release has the APK, BUILDLOG written): VD1a-e, IMG1a-b, VD2a ticked. alpha88 work is gated (core+app tests, gecko compile, lint green; web-tests full run in progress, w2.log)

## Do this next
Next: w2.log WEB_EXIT=0 -> bump geckoapp/build.gradle.kts to 189 / 0.3.0-alpha88, tools/sync-ext-version.sh, ship.sh gecko with docs/handoff/ALPHA88_RELEASE_NOTE.txt, ONE dispatch, verify, record-release; then answer the owner (VD2c: Shields cuts the circle = list rules named; endless loading not Shields/gate/format policy; alpha88 stall probe + hidden-rules line; tests to run now: Shields row 'Hide ad spaces & cookie banners' off for pornhub.com; Settings 'At most 30 frames' off). Open: IMG1 cause, VD2b fix, GL1 cause, PV1f/g, LD3f, LD4f/g, YTC1/2, ATT1, FLK1, PB8

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  b34edd20 ckpt 2850: alpha87 SECOND dispatch queued 23:09Z = run 44960366094 (the first, 44958961
  8a2a9b11 ckpt 2849: alpha87 first dispatch (run 44958961031) FAILED in test-js on GitHub: pageti
  3811af55 ckpt 2848: Owner follow-up on pornhub (VD2c, in TASKS.md): Shields ON cuts off the load
  7579817e ckpt 2847: alpha87 (code 188) gated and pushed, ONE android.yml dispatch made at about 
  85221da4 ckpt 2846: gated kestrel-g-v0.3.0-alpha87 (gecko code 188) and pushed it: handoff self-
  bf9a7857 ckpt 2845: pre-ship: alpha87: why videos do not load, and a test you can run today. I c
  a3480ef8 ckpt 2844: Digest FILE PICKS section (+2 checks, handoff 88/88); OwnerRulesTest pins up
  0a054723 ckpt 2843: IMG1 picked-file facts written (core FilePickFacts + FilePickStats.lastPick 
  39f5dc40 ckpt 2842: Research notes written for the 3 new owner requests (IMG1_UPLOAD.md, VD2 sec
  a0c4325f ckpt 2841: Owner sent 3 more requests mid-gate (IMG1 screenshots 'can't process images'
```

(13 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
