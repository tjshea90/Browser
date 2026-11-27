# CHECKPOINT 3496 — read me first, then TASKS.md

**Written:** 2026-11-27T07:56:59Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/eager-hawking-6z6tcf` · **builds on:** `8b21f578` (this checkpoint is the commit after it)

## Just done
alpha112 (code 213) gated green by ship.sh and DISPATCHED (android.yml, app=gecko, full_build=true, ref claude/eager-hawking-6z6tcf); ND2/ND2d coded+tested; research/PROFILE_CLAUDE_AI.md written; TASKS ND2b/ND2c open

## Do this next
wait for the tag: git ls-remote --tags origin kestrel-g-v0.3.0-alpha112; then mcp__github__get_release_by_tag (tjshea90/Browser) shows the APK only after test-jvm, test-js and build-gecko pass; THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha112 with the IDENTICAL note from docs/handoff/ALPHA112_RELEASE_NOTE.txt; tick ND2 in TASKS.md; answer the owner in plain words with the Release link on its own line; re-list open items (ND2b second recording, ND2c, ND1g keep-ready hour test, PERF25a, OPT3e, PERF17c/d, PERF19, PERF23, PERF14e, PERF9, VPS2d, keyboard-resize wait, NET_PARTIAL_TRANSFER, first-run-after-install death)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  8b21f578 ckpt 3495: gated kestrel-g-v0.3.0-alpha112 (gecko code 213) and pushed it: handoff self
  9c29320d ckpt 3493: pre-ship: alpha112: the first real look inside a page load, and a first fix 
  5111ed55 ckpt 3479: ND2: marker pairing fixed in tools/profile_digest.py (phase 1/2/3), network 
  cf0d3989 ckpt 3465: alpha110 AND alpha111 RELEASED AND RECORDED (BUILDLOG lines 211 and 212). ND
  9a47bf75 ckpt 3461: alpha111 RE-DISPATCHED ONCE after fixes: run 45954070324 (run number 313, he
  240256a0 ckpt 3460: gated kestrel-g-v0.3.0-alpha111 (gecko code 212) and pushed it: handoff self
  3382c4f8 ckpt 3457: alpha111 DISPATCHED ONCE: run 45952674368 (run number 312, head 84d5657e, ap
  84d5657e ckpt 3456: gated kestrel-g-v0.3.0-alpha111 (gecko code 212) and pushed it: handoff self
  561bfd0f ckpt 3454: pre-ship: alpha111: two new instruments, so the next review can see what cla
  1f3ac22f ckpt 3448: ND1d/ND1e CODED (not shipped yet): core ProfileRule (+5 tests), geckoapp Pag
```
