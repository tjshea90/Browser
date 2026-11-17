# CHECKPOINT 2579 — read me first, then TASKS.md

**Written:** 2026-11-17T00:16:05Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-eb19b61e-9kt9nz` · **builds on:** `2e114ac2` (this checkpoint is the commit after it)

## Just done
ANS61: :core:test 1413/0 failed (OwnerRulesTest guard added), :geckoapp:compileDebugKotlin green online (android-37.0 installed with sdkmanager; geckoapp/app deps are NOT cached offline here), web handover tests pass; full npm test running (log in scratchpad webtests.log)

## Do this next
write ANS61c lab scene: soaklab.page_ho variants preroll (ad-state class, page plays ad then film) and prerollhid (undetected ad: Kestrel plays it to its end, Back, press again = loop breaker, page plays it then the film) + soak.py HO_CASES + ho_scene branch 'adpage'; then dispatch android.yml app=lab lab_suite=ho, bump version, ship alpha62

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a86c1e78 ckpt 2578: ANS61b core+app+page done: WebHandOver ad rules (Element.ad, looksLikeAdAddr
  59855a57 ckpt 2577: ANS61 written into TASKS.md in the owner's words (pre-roll ad played in Kest
  484824ab ckpt 2576: PRIV1 done: INBOX.md, OUTBOX.md and .handoff now live in the PRIVATE repo tj
  18ce6643 ckpt 2575: PRIV1: owner chose group 1 only (INBOX.md, OUTBOX.md, .handoff) and history 
  352c3027 ckpt 2574: PRIV1: owner changed the plan (builds on a PUBLIC repo for free minutes, sen
  9ac3a45b ckpt 2573: alpha61 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha61 (code 162, ru
  6d7a9cdb ckpt 2572: ANS60a-d ticked in TASKS.md with the lab evidence; HANDOFF alpha61 bullet re
  c5280d94 ckpt 2571: alpha61 build DISPATCHED ONCE: run 43975612672 (android.yml app=gecko full_b
  1e7b8d2f ckpt 2570: gated kestrel-g-v0.3.0-alpha61 (gecko code 162) and pushed it: handoff self-
  d414de3b ckpt 2569: FINAL lab vp run 43971358668 (head ed7587da, includes all review fixes): 12 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
