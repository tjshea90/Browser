# CHECKPOINT 2529 — read me first, then TASKS.md

**Written:** 2026-11-16T17:03:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `fe2b39a6` (this checkpoint is the commit after it)

## Just done
ANS55f lab run 1 (ip, 43911183498): the inline player WORKS on the real engine up to the first picture: 'inline placed page box 560x300 css at 0,115 scroll 0,0 -> view [0,124 600x321]' = the page's own rect [0,124 600x321]; first-picture 1412 ms; compositor updates 'scroll 0.0,4.7 zoom 1.071 -> view [0,119 ...]' (CSS px x zoom = dpr 1.0714, matches the javadoc); then my lab script died on a bare-tuple format (TypeError) - fixed; also added scroll-settled log + long press check

## Do this next
Lab run 2 of suite ip dispatched on this head (see CHECKPOINT/commit message for its run id); read it; independent review agent abc4fba2246ef5679 (ship-code-reviewer) is running - apply its real findings; then lab ho; then bump geckoapp versionCode 161 (alpha60), write docs/handoff/ALPHA60_RELEASE_NOTE.txt, ship.sh gecko, ONE dispatch, record-release, answer owner 55c/d.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6d700eee ckpt 2528: ANS55f: lab run 1 of suite ip DISPATCHED: run 43911183498 (head 72df6d09, st
  72df6d09 ckpt 2527: ANS55f: inline player coded + unit suites ALL green (core, app, geckoapp; gu
  441aa19c ckpt 2526: ANS55f step 2 coded and COMPILES (:geckoapp:compileDebugKotlin ok): PlayerFa
  16795a42 ckpt 2525: ANS55f step 1 done: gate-main.js geomOf + content.js geo() + background.js g
  50071840 ckpt 2524: alpha59 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha59 (code 160, ru
  2e6fccf5 ckpt 2523: alpha59 RELEASE DISPATCHED (ONE time): android.yml run 43905841979 (run 174,
  d0daa8b8 ckpt 2522: gated kestrel-g-v0.3.0-alpha59 (gecko code 160) and pushed it: handoff self-
  c4c4f1df ckpt 2521: pre-ship: alpha59: your message 55 (on alpha58). (1) FIXED: the black pictur
  04ffa9c9 ckpt 2520: lab ho run dispatched: 43903287513 (head 09476513 = ckpt 2519, started 16:03
  09476513 ckpt 2519: owner message 56 written into TASKS.md (ANS56a-b): 'even when I selected a t
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
