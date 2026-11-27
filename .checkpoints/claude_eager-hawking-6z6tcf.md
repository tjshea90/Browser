# CHECKPOINT 3518 — read me first, then TASKS.md

**Written:** 2026-11-27T21:43:32Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/eager-hawking-6z6tcf` · **builds on:** `1c1bf661` (this checkpoint is the commit after it)

## Just done
alpha113 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha113, run 46070508048 green); ND3/ND3a/ND3b/ND3d/ND3e ticked; research/PROFILE_CLAUDE_AI.md section 8 has the second recording, the new diagnostics, the symbolicated parent main thread and the module-waterfall model

## Do this next
WAIT FOR THE OWNER: ND3f (third recording on alpha113; python3 -B -I tools/profile_digest.py --symbolicate; meta page_process; if missing again record without script stacks) and ND1g (keep-ready hour test). Then python3 -B -I tools/diag_digest.py: HEAVY PAGES BY BUILD and KILLS (swipes now separate). Open: ND2c (classifier prefs A/B, per-tab blocker, navigation_cache Network-lab A/B), PERF19/23, PERF14e Back place missed 9 of 14, PERF9, VPS2d owner pick, keyboard-resize wait, NET_PARTIAL_TRANSFER, first-start-after-install deaths (DG1)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  f0095b38 ckpt 3513: alpha113 (code 214) gated green by ship.sh and DISPATCHED (android.yml app=g
  2012784b ckpt 3512: gated kestrel-g-v0.3.0-alpha113 (gecko code 214) and pushed it: handoff self
  5af245e5 ckpt 3511: pre-ship: alpha113: a crash fixed, and a better look inside claude.ai (alpha
  90abb06a ckpt 3499: owner message after alpha112: 'I sent a new diagnostics and a new page profi
  7198cbef ckpt 3498: alpha112 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha112, run 459619
  0dc5985c ckpt 3496: alpha112 (code 213) gated green by ship.sh and DISPATCHED (android.yml, app=
  8b21f578 ckpt 3495: gated kestrel-g-v0.3.0-alpha112 (gecko code 213) and pushed it: handoff self
  9c29320d ckpt 3493: pre-ship: alpha112: the first real look inside a page load, and a first fix 
  5111ed55 ckpt 3479: ND2: marker pairing fixed in tools/profile_digest.py (phase 1/2/3), network 
  cf0d3989 ckpt 3465: alpha110 AND alpha111 RELEASED AND RECORDED (BUILDLOG lines 211 and 212). ND
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
