# CHECKPOINT 3519 — read me first, then TASKS.md

**Written:** 2026-11-27T22:13:53Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/eager-hawking-6z6tcf` · **builds on:** `62f3b899` (this checkpoint is the commit after it)

## Just done
owner message after alpha113 written as TASKS ND4 (new profile; app still crashes after update; text is slow right now)

## Do this next
read the evidence BEFORE code: git -C ../Kestrel-Private fetch; list profiles/ + diag/ newest; python3 -B -I tools/profile_digest.py --symbolicate NEWEST; python3 -B -I tools/diag_digest.py; look at the crash section, snapshot Last run ended, run start/life lines around the alpha113 install, typing latency, cpu busy; then fix + ship alpha114 if there is a real change

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  62f3b899 ckpt 3518: alpha113 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha113, run 460705
  f0095b38 ckpt 3513: alpha113 (code 214) gated green by ship.sh and DISPATCHED (android.yml app=g
  2012784b ckpt 3512: gated kestrel-g-v0.3.0-alpha113 (gecko code 214) and pushed it: handoff self
  5af245e5 ckpt 3511: pre-ship: alpha113: a crash fixed, and a better look inside claude.ai (alpha
  90abb06a ckpt 3499: owner message after alpha112: 'I sent a new diagnostics and a new page profi
  7198cbef ckpt 3498: alpha112 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha112, run 459619
  0dc5985c ckpt 3496: alpha112 (code 213) gated green by ship.sh and DISPATCHED (android.yml, app=
  8b21f578 ckpt 3495: gated kestrel-g-v0.3.0-alpha112 (gecko code 213) and pushed it: handoff self
  9c29320d ckpt 3493: pre-ship: alpha112: the first real look inside a page load, and a first fix 
  5111ed55 ckpt 3479: ND2: marker pairing fixed in tools/profile_digest.py (phase 1/2/3), network 
```
