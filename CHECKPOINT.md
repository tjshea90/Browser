# CHECKPOINT 2817 — read me first, then TASKS.md

**Written:** 2026-11-21T16:44:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `4a9f3b5e` (this checkpoint is the commit after it)

## Just done
alpha84 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha84 (gecko code 185, run 44924979560 green, APK + sha256 + dm in the Release, BUILDLOG line pushed); heartbeat deleted. LD1, LD2, LD3a-e ticked; LD3f (owner test of the automatic send) and LD3g (ideas) open

## Do this next
Nothing is running. Waiting for the owner to install alpha84 and use it normally; then python3 -B -I tools/diag_digest.py must print 'Sends: N automatic' (PROVEN) - if not, look at LiveDiag.onFront/rearm first. Also open: YTC1/YTC2, ATT1, FLK1 PhoneBackendTest flake, PB8, LD3g ideas (re-arm after a successful send, scroll meter suggestion, decoder use per play)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  bcfe8a73 ckpt 2816: alpha84 build running: run id 44924979560 (run_number 265, head 2cf128f4), d
  4a42d19d ckpt 2815: alpha84 gated (core+app tests, gecko compile, lint 0 errors, self-test 80/80
  2cf128f4 ckpt 2814: gated kestrel-g-v0.3.0-alpha84 (gecko code 185) and pushed it: handoff self-
  0efd4ff7 ckpt 2813: ALPHA84 release note written (docs/handoff/ALPHA84_RELEASE_NOTE.txt); platfo
  c4318ee7 ckpt 2812: LD3 docs + TASKS ticks (a-d) written; digest prints sends automatic/manual +
  7369e028 ckpt 2811: LD3 code done and green: core DiagJournal (exact commit, honest lost/thinned
  bb443992 ckpt 2810: LD3d tools done: private.sh + diag_digest.py find the clone in any letter ca
  2015daad ckpt 2809: LD3 audit written into research/LIVE_DIAG.md from the 4 real uploads: manual
  51209a26 ckpt 2808: LD3 written into TASKS.md in the owner's words (4 checks of the live diagnos
  91a90882 ckpt 2807: alpha83 SHIPPED and RECORDED: run 44882471063 (run_number 264, head 805cabb9
```
