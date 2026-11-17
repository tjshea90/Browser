# CHECKPOINT 2582 — read me first, then TASKS.md

**Written:** 2026-11-17T00:19:32Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-eb19b61e-9kt9nz` · **builds on:** `86d845d2` (this checkpoint is the commit after it)

## Just done
alpha62 prepared but NOT shipped: geckoapp versionCode 163 / 0.3.0-alpha62, extension manifest 1.0.163, docs/handoff/ALPHA62_RELEASE_NOTE.txt draft (has TESTED_PLACEHOLDER to fill from the lab result)

## Do this next
wait for lab run 43986542855 (head 977ab6a8) and the full npm test (scratchpad webtests.log, bg task bx5tcwunh); fill TESTED_PLACEHOLDER with what was really run; then bash ship.sh gecko with the SAME note, ONE dispatch app=gecko full_build=true, verify tag+jobs+get_release_by_tag, then tools/record-release.sh kestrel-g-v0.3.0-alpha62 with identical note, tick ANS61 in TASKS.md, answer owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  241f219d ckpt 2581: lab ho run 43986542855 DISPATCHED (head 977ab6a8, app=lab lab_suite=ho): pro
  977ab6a8 ckpt 2580: ANS61c: lab variants preroll (ad-state class, ad set up before the press) + 
  25e3ced5 ckpt 2579: ANS61: :core:test 1413/0 failed (OwnerRulesTest guard added), :geckoapp:comp
  a86c1e78 ckpt 2578: ANS61b core+app+page done: WebHandOver ad rules (Element.ad, looksLikeAdAddr
  59855a57 ckpt 2577: ANS61 written into TASKS.md in the owner's words (pre-roll ad played in Kest
  484824ab ckpt 2576: PRIV1 done: INBOX.md, OUTBOX.md and .handoff now live in the PRIVATE repo tj
  18ce6643 ckpt 2575: PRIV1: owner chose group 1 only (INBOX.md, OUTBOX.md, .handoff) and history 
  352c3027 ckpt 2574: PRIV1: owner changed the plan (builds on a PUBLIC repo for free minutes, sen
  9ac3a45b ckpt 2573: alpha61 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha61 (code 162, ru
  6d7a9cdb ckpt 2572: ANS60a-d ticked in TASKS.md with the lab evidence; HANDOFF alpha61 bullet re
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
