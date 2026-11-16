# CHECKPOINT 2523 — read me first, then TASKS.md

**Written:** 2026-11-16T16:17:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `d0daa8b8` (this checkpoint is the commit after it)

## Just done
alpha59 RELEASE DISPATCHED (ONE time): android.yml run 43905841979 (run 174, app=gecko full_build=true, head d0daa8b8, queued 16:17Z); lab ho run 43903287513 (head 09476513): 53 check rows ok, 0 issues incl. hlsdecoy, audioonly, hlstwo; web 573/573; ship gates ok

## Do this next
do NOT dispatch alpha59 again unless run 43905841979 ends WITHOUT a tag. Wait for it (git ls-remote --tags origin | grep alpha59; actions_list list_workflow_jobs 43905841979 latest perPage 8 is ~6 KB; get_release_by_tag kestrel-g-v0.3.0-alpha59): tag + test-jvm + test-js + build-gecko success + APK on the release; ONLY THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha59 "$(cat docs/handoff/ALPHA59_RELEASE_NOTE.txt)"; tick ANS55a-c,e and ANS56a-b in TASKS; HANDOFF alpha59 bullet; answer owner messages 55 + 56 (Release link on its own line; the inline player is NOT in alpha59: say so). Meanwhile build the inline player (ANS55f): PlayerFactory + InlinePlayer + PlayerControlsView flags + BrowserActivity overlay + gate geometry; draft design in research/ANS55_INLINE_PLAYER.md

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d0daa8b8 ckpt 2522: gated kestrel-g-v0.3.0-alpha59 (gecko code 160) and pushed it: handoff self-
  c4c4f1df ckpt 2521: pre-ship: alpha59: your message 55 (on alpha58). (1) FIXED: the black pictur
  04ffa9c9 ckpt 2520: lab ho run dispatched: 43903287513 (head 09476513 = ckpt 2519, started 16:03
  09476513 ckpt 2519: owner message 56 written into TASKS.md (ANS56a-b): 'even when I selected a t
  f7ab7a01 ckpt 2518: ANS55e fix coded (core tests + gecko compile green): Yahoo/Brightcove root c
  5851f383 ckpt 2517: owner message 55 (alpha58 on the tablet: Yahoo video black, resumed the wron
  6f31c2fb ckpt 2516: session handoff saved by the handoff tool (.handoff/handoff.md + json + arch
  3eb91380 ckpt 2515: alpha58 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha58 (code 159, AP
  8dccb965 ckpt 2514: alpha58 RELEASE DISPATCHED AGAIN (the first run 43843245891 made no release:
  596e4e4a ckpt 2513: release run 43843245891 FAILED before any build: GitHub's web-test job faile
```
