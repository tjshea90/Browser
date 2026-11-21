# CHECKPOINT 2860 — read me first, then TASKS.md

**Written:** 2026-11-21T23:31:40Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `6f8657d1` (this checkpoint is the commit after it)

## Just done
alpha88 (code 189) gated and shipped to GitHub: ONE dispatch made about 23:33Z (do not dispatch again unless it fails). Gates: core+app tests, gecko compile, lint, web-tests 671/671, handoff 89/89. A send_later reminder (+22 min) will finish record-release

## Do this next
Next: find the alpha88 run, wait for all jobs green, get_release_by_tag kestrel-g-v0.3.0-alpha88 shows the APK, NOTE=$(cat docs/handoff/ALPHA88_RELEASE_NOTE.txt); bash tools/record-release.sh kestrel-g-v0.3.0-alpha88 "$NOTE"; tick VD2b? no: still open until the owner's next upload names the cause (VD2c), GL1b open until the hidden-rules line names the rule. Owner tests to ask for: Shields row 'Hide ad spaces & cookie banners' off for pornhub.com; Settings 'At most 30 frames a second' off; Videos may start by themselves for pornhub.com / instagram.com; then Send now in Live diagnostics

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6f8657d1 ckpt 2859: gated kestrel-g-v0.3.0-alpha88 (gecko code 189) and pushed it: handoff self-
  f02e9214 ckpt 2858: pre-ship: alpha88: what your pornhub clue shows, and two new probes. The cut
  c6fc62aa ckpt 2857: alpha87 RELEASED and recorded (run 44960366094 green, Release has the APK, B
  b34edd20 ckpt 2850: alpha87 SECOND dispatch queued 23:09Z = run 44960366094 (the first, 44958961
  8a2a9b11 ckpt 2849: alpha87 first dispatch (run 44958961031) FAILED in test-js on GitHub: pageti
  3811af55 ckpt 2848: Owner follow-up on pornhub (VD2c, in TASKS.md): Shields ON cuts off the load
  7579817e ckpt 2847: alpha87 (code 188) gated and pushed, ONE android.yml dispatch made at about 
  85221da4 ckpt 2846: gated kestrel-g-v0.3.0-alpha87 (gecko code 188) and pushed it: handoff self-
  bf9a7857 ckpt 2845: pre-ship: alpha87: why videos do not load, and a test you can run today. I c
  a3480ef8 ckpt 2844: Digest FILE PICKS section (+2 checks, handoff 88/88); OwnerRulesTest pins up
```
