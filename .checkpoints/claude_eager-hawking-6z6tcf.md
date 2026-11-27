# CHECKPOINT 3498 — read me first, then TASKS.md

**Written:** 2026-11-27T08:14:43Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/eager-hawking-6z6tcf` · **builds on:** `184fda69` (this checkpoint is the commit after it)

## Just done
alpha112 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha112, run 45961952478 green); ND1g-RESULT/ND2/ND2d ticked; research/PROFILE_CLAUDE_AI.md has the first real recording's findings

## Do this next
WAIT FOR THE OWNER: (1) ND2b: second recording of claude.ai on alpha112 (Settings > Live diagnostics > Record a profile of this page); then python3 -B -I tools/profile_digest.py and compare with research/PROFILE_CLAUDE_AI.md section 2/3 (Extension Suspend median 3.9 s, ExtensionParent events 2665, open-to-end median 7.9 s, parent main thread saturated 8-23 s) and look for the PAGE process; (2) ND1g: keep-ready hour test (diag_digest KILLS). No new owner message pending. Open: ND2c (classifier prefs A/B, per-tab blocker), PERF19/23, PERF14e, PERF9, VPS2d (owner must pick), keyboard-resize wait, NET_PARTIAL_TRANSFER, first-run-after-install death

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  0dc5985c ckpt 3496: alpha112 (code 213) gated green by ship.sh and DISPATCHED (android.yml, app=
  8b21f578 ckpt 3495: gated kestrel-g-v0.3.0-alpha112 (gecko code 213) and pushed it: handoff self
  9c29320d ckpt 3493: pre-ship: alpha112: the first real look inside a page load, and a first fix 
  5111ed55 ckpt 3479: ND2: marker pairing fixed in tools/profile_digest.py (phase 1/2/3), network 
  cf0d3989 ckpt 3465: alpha110 AND alpha111 RELEASED AND RECORDED (BUILDLOG lines 211 and 212). ND
  9a47bf75 ckpt 3461: alpha111 RE-DISPATCHED ONCE after fixes: run 45954070324 (run number 313, he
  240256a0 ckpt 3460: gated kestrel-g-v0.3.0-alpha111 (gecko code 212) and pushed it: handoff self
  3382c4f8 ckpt 3457: alpha111 DISPATCHED ONCE: run 45952674368 (run number 312, head 84d5657e, ap
  84d5657e ckpt 3456: gated kestrel-g-v0.3.0-alpha111 (gecko code 212) and pushed it: handoff self
  561bfd0f ckpt 3454: pre-ship: alpha111: two new instruments, so the next review can see what cla
```
