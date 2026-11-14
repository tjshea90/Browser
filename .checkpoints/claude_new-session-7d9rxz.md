# CHECKPOINT 2348 — read me first, then TASKS.md

**Written:** 2026-11-14T21:54:25Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `ff61cb61` (this checkpoint is the commit after it)

## Just done
alpha41 RE-DISPATCHED (second run; the first failed on the flaky FLK1 test and published nothing): ship gates green, commit pushed, android.yml run queued (app=gecko full_build=true ref claude/new-session-7d9rxz) with the note that includes the YouTube skipper. A background waiter on the tag exists in the dead session only

## Do this next
WAIT for the run: git ls-remote --tags origin kestrel-g-v0.3.0-alpha41 shows the tag; mcp__github__actions_list list_workflow_runs (owner tjshea90 repo Browser resource_id android.yml, workflow_runs_filter {event workflow_dispatch, branch claude/new-session-7d9rxz}) newest run shows conclusion success (if a test job fails read mcp__github__get_job_logs with return_content true tail_lines 70 for that job id; a failure in ServerTest.manyClientsAtOnceGetTheirOwnAnswers or another courier test is a known load flake: rerun with mcp__github__actions_run_trigger method rerun_failed_jobs run_id <id>, do NOT dispatch a new run for it); mcp__github__get_release_by_tag shows the APK; ONLY THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha41 "alpha41: Back remembers your place on each page and counts how Back behaves (Back lab); YouTube ad skipping (mutes and fast-forwards ads, presses Skip, closes banners, removes the ad-blocker box; one switch in Settings, on by default) and the lists' full YouTube rules; lists can only rewrite pages if they are the uBlock lists; a page can no longer fake your tap or click inside frames it embeds; the pop-up guard is there from the very first page. Tested on the emulator and in test browsers, not on your tablet." then answer the owner in plain words with https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha41 on its own line, what was proven (emulator: Back kept/reload/pull, guard from the first page, frame assist in the real engine; test browsers: tap forgery, JSON edit, skipper on a mock player built from the live markup) vs NOT run on the tablet, and re-list open items (BK5 question, AX1g, alpha40+41 tablet feedback, a screenshot reply already given). Meanwhile (no build needed): RX3-Y3, RX3-G7, then Wave B

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ff61cb61 ckpt 2347: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
  79518152 ckpt 2346: first alpha41 run FAILED on GitHub: core ProxyLinkTest.theRenewalKeepsItOpen
  d6379d16 ckpt 2345: alpha41 (gecko code 142, manifest 1.0.142) GATED, PUSHED and the android.yml
  836d9546 ckpt 2344: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
  14a6a198 ckpt 2343: RX3-G1 DONE (guard+library declared in the manifest, world MAIN; emulator-pr
  e462351b ckpt 2342: RX3-G3 DONE (script side): the cross-frame postMessage assist channel is gon
  279f3b45 ckpt 2341: RX3-BK1/BK2 first emulator proof (static case): page left at y=1800, Back re
  8888c78b ckpt 2340: RX3-BK1/BK2 Kotlin wiring DONE and COMPILES (geckoapp + app debug): ShieldsB
  687414dd ckpt 2339: owner message 40 (a screenshot, no words: YouTube Movies and TV page 'Video 
  2d3b6a3a ckpt 2338: RX3-BK1/BK2 core logic DONE and green: core/session/BackScroll.kt (ScrollMem
```
