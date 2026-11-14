# CHECKPOINT 2345 — read me first, then TASKS.md

**Written:** 2026-11-14T21:37:22Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/new-session-7d9rxz` · **builds on:** `836d9546` (this checkpoint is the commit after it)

## Just done
alpha41 (gecko code 142, manifest 1.0.142) GATED, PUSHED and the android.yml run DISPATCHED (app=gecko full_build=true ref claude/new-session-7d9rxz). Contains BK1+BK2 (Back place + Back lab), B1, G1, G2, G3 (proven in the real engine: assist 2/2, frame video started), Y1 mechanics. RX3-G3 ticked. NOT run on the tablet

## Do this next
WAIT for the run: git ls-remote --tags origin kestrel-g-v0.3.0-alpha41 shows the tag; mcp__github__actions_list list_workflow_runs (owner tjshea90 repo Browser resource_id android.yml, workflow_runs_filter branch claude/new-session-7d9rxz event workflow_dispatch) shows conclusion success; mcp__github__get_release_by_tag shows the APK; ONLY THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha41 "alpha41: Back remembers your place on each page and counts how Back behaves (Back lab), lists can only rewrite pages if they are the uBlock lists, a page can no longer fake your tap or click inside frames it embeds, the pop-up guard is in place from the very first page, the lists' full YouTube rules run (JSON edit engine). Emulator-tested, not run on the tablet." and then answer the owner in plain words with the Release link https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha41 on its own line, what was proven on the emulator vs NOT run on the tablet, and the open items (BK5 question, AX1g, tablet feedback, RX4 screenshot answer already given). While waiting (no build needed): RX3-Y2 the YouTube skipper (new geckoapp/src/main/assets/kestrel-shields/youtube.js, manifest static entry for the YouTube hosts, web tests with a mock player first) then RX3-Y3 live check with tools/sitelab/yt_probe.js style Playwright + research/YOUTUBE_ADS.md. Never ship alpha41 twice; next release needs versionCode 143

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  836d9546 ckpt 2344: gated kestrel-g-v0.3.0-alpha41 (gecko code 142) and pushed it: handoff self-
  14a6a198 ckpt 2343: RX3-G1 DONE (guard+library declared in the manifest, world MAIN; emulator-pr
  e462351b ckpt 2342: RX3-G3 DONE (script side): the cross-frame postMessage assist channel is gon
  279f3b45 ckpt 2341: RX3-BK1/BK2 first emulator proof (static case): page left at y=1800, Back re
  8888c78b ckpt 2340: RX3-BK1/BK2 Kotlin wiring DONE and COMPILES (geckoapp + app debug): ShieldsB
  687414dd ckpt 2339: owner message 40 (a screenshot, no words: YouTube Movies and TV page 'Video 
  2d3b6a3a ckpt 2338: RX3-BK1/BK2 core logic DONE and green: core/session/BackScroll.kt (ScrollMem
  13adf44e ckpt 2334: saved research/REVIEW-EXTERNAL-3.md (v2, byte-identical copy of the owner's 
  094aed9b ckpt 2337: alpha40 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha40 (gecko code 141, run
  e2056d0c ckpt 2333: alpha40 (gecko code 141) gated, pushed and the android.yml run DISPATCHED (a
```
