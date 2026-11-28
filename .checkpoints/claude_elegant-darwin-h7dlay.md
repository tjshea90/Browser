# CHECKPOINT 3609 — read me first, then TASKS.md

**Written:** 2026-11-28T09:26:29Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/elegant-darwin-h7dlay` · **builds on:** `7de7dd59` (this checkpoint is the commit after it)

## Just done
alpha116 (gecko code 217) gated green by ship.sh and DISPATCHED: android.yml run 46130447814 (run_number 318, head 7de7dd59, 09:25Z). Contains alpha115 + ND6a-A (download failed counters + line) + ND6a-B (Stopped heading) + ND6a-C (private-tab counters fix). The owner already has the interim answer with the alpha115 link.

## Do this next
WAIT for run 46130447814 (git ls-remote --tags origin kestrel-g-v0.3.0-alpha116, then mcp__github__actions_list list_workflow_jobs resource_id 46130447814: all success; get_release_by_tag shows the APK), then bash tools/record-release.sh kestrel-g-v0.3.0-alpha116 with the IDENTICAL note (docs/handoff/ALPHA116_RELEASE_NOTE.txt via NOTE=$(cat file)), tick ND6a (bash tools/claim.sh ND6a --done 'alpha116 run 46130447814; HandedTest 19/19; digest read in research/ND6_DIAGNOSTICS_2026-11-28.md') and ND6, then the FINAL answer to the owner: alpha116 link on its own line (install the newest, it contains alpha115), what changed in plain words, what was NOT run (nothing on the tablet, no Proton account), ND4a request, the honest ND6 summary (digest shows nothing new beyond known WATCH items; page-load levers need tablet A/B), re-list open items: PDL1a (retry the zip), ND4a (did the app stay open after the install), ND5c (Shields bisect on my.nobullnetworks.com: only Block ads and trackers off, then only Cosmetic filtering off, Send now), ND3f (record claude.ai again), VPS1b/VPS2d (pick), PERF25a (which device Brave ran on).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  7de7dd59 ckpt 3608: gated kestrel-g-v0.3.0-alpha116 (gecko code 217) and pushed it: handoff self
  72c084f5 ckpt 3606: pre-ship: alpha116: a small follow-up to alpha115 (alpha115, with the Proton
  6a134cd1 ckpt 3604: ND6a-B/C in the tree (not released): Downloads screen 'Stopped' heading for 
  ce3957d6 ckpt 3600: alpha115 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha115 (gecko code 216, r
  0ae06bdc ckpt 3594: ND6a-A DONE in the tree (not released): Engine.Listener.onFailed(DownloadVie
  439bcc1b ckpt 3589: alpha115 (gecko code 216) gated green by ship.sh and DISPATCHED: android.yml
  aae77d3c ckpt 3588: gated kestrel-g-v0.3.0-alpha115 (gecko code 216) and pushed it: handoff self
  165f5255 ckpt 3586: pre-ship: alpha115: downloads that a page makes itself, like Proton Drive, n
  f47e6119 ckpt 3582: PDL1 CODED (not yet released): core Handed.kt (HandedStreams registry, Hande
  3261ff02 ckpt 3569: Second owner message (ND6: after PDL1, look for improvements from sent diagn
```
