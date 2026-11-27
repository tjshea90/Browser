# CHECKPOINT 3513 — read me first, then TASKS.md

**Written:** 2026-11-27T21:28:36Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/eager-hawking-6z6tcf` · **builds on:** `2012784b` (this checkpoint is the commit after it)

## Just done
alpha113 (code 214) gated green by ship.sh and DISPATCHED (android.yml app=gecko full_build=true ref claude/eager-hawking-6z6tcf): KeepReadyService crash fix (core KeepReady.stopStep), recorder v3 (GeckoMain only, 20 s, memory release 2.5 s before end, page_process meta), profile_digest --symbolicate, diag_digest swipe-away kills

## Do this next
wait for the tag: git ls-remote --tags origin kestrel-g-v0.3.0-alpha113; then mcp__github__get_release_by_tag tjshea90/Browser; list_workflow_runs (perPage 1, branch, workflow_dispatch) must say success; THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha113 with the IDENTICAL note from docs/handoff/ALPHA113_RELEASE_NOTE.txt; tick ND3, ND3a, ND3b in TASKS.md; answer the owner in plain words with the Release link on its own line; re-list open items (ND3b owner step: third recording, ND2c ideas, ND1g keep-ready hour test, PERF19/23, PERF14e Back place missed 9 of 14, PERF9, VPS2d owner pick, keyboard-resize wait, NET_PARTIAL_TRANSFER, first-start-after-install deaths)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2012784b ckpt 3512: gated kestrel-g-v0.3.0-alpha113 (gecko code 214) and pushed it: handoff self
  5af245e5 ckpt 3511: pre-ship: alpha113: a crash fixed, and a better look inside claude.ai (alpha
  90abb06a ckpt 3499: owner message after alpha112: 'I sent a new diagnostics and a new page profi
  7198cbef ckpt 3498: alpha112 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha112, run 459619
  0dc5985c ckpt 3496: alpha112 (code 213) gated green by ship.sh and DISPATCHED (android.yml, app=
  8b21f578 ckpt 3495: gated kestrel-g-v0.3.0-alpha112 (gecko code 213) and pushed it: handoff self
  9c29320d ckpt 3493: pre-ship: alpha112: the first real look inside a page load, and a first fix 
  5111ed55 ckpt 3479: ND2: marker pairing fixed in tools/profile_digest.py (phase 1/2/3), network 
  cf0d3989 ckpt 3465: alpha110 AND alpha111 RELEASED AND RECORDED (BUILDLOG lines 211 and 212). ND
  9a47bf75 ckpt 3461: alpha111 RE-DISPATCHED ONCE after fixes: run 45954070324 (run number 313, he
```
