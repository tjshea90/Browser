# CHECKPOINT 2835 — read me first, then TASKS.md

**Written:** 2026-11-21T20:08:01Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `0df22017` (this checkpoint is the commit after it)

## Just done
alpha86 gated (core + app full suites via ship.sh, lint 0 errors, test_handoff 83/83, check-gecko-prefs 51/51) and DISPATCHED ONCE at 20:07Z on 2026-11-21: android.yml app=gecko full_build=true ref claude/peaceful-darwin-4sbmdz. DO NOT DISPATCH AGAIN.

## Do this next
Wait for tag kestrel-g-v0.3.0-alpha86 (git ls-remote --tags origin), then actions_list list_workflow_jobs for the run (find it: list_workflow_runs android.yml branch claude/peaceful-darwin-4sbmdz event workflow_dispatch) all success, get_release_by_tag shows the APK, bash tools/record-release.sh kestrel-g-v0.3.0-alpha86 with the IDENTICAL note from docs/handoff/ALPHA86_RELEASE_NOTE.txt (cat | tr -d newline); tick PV1d; answer the owner in plain words: what was already safe, 7 holes fixed, Safe Browsing trade-off + offer a Settings switch (PV1g), what cannot be closed, NOT run on tablet; re-list open items (LD3f, LD4f, LD4g, YTC1/2, ATT1, FLK1, PB8)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  0df22017 ckpt 2834: gated kestrel-g-v0.3.0-alpha86 (gecko code 187) and pushed it: handoff self-
  620bf412 ckpt 2833: pre-ship: alpha86: private tabs checked end to end, and every hole found is 
  253c10df ckpt 2832: PV1: audit written (research/PRIVATE_TABS.md, 26 rows), 7 holes fixed: Shiel
  dcf47bcb ckpt 2831: PV1: tests (PrivateTabsTest 12 incl. canary leak scans of ring / live journa
  8abe214c ckpt 2830: PV1 code in place and compiling: core PrivateScrub + PrivateWindow (71 core 
  985f693e ckpt 2829: PV1 written into TASKS.md in the owner's words (private tabs: nothing saved 
  ede36761 ckpt 2828: alpha85 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/
  0f2cd29a ckpt 2827: alpha85 gated (core 1991 incl. FLK1 flake passing alone, app 478, lint 0 err
  b9f6b0b0 ckpt 2826: gated kestrel-g-v0.3.0-alpha85 (gecko code 186) and pushed it: handoff self-
  495ef4db ckpt 2825: pre-ship: alpha85: the live diagnostics now explain WHY a page is slow, whic
```
