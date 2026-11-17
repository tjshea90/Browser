# CHECKPOINT 2580 — read me first, then TASKS.md

**Written:** 2026-11-17T00:18:15Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-eb19b61e-9kt9nz` · **builds on:** `1f91475f` (this checkpoint is the commit after it)

## Just done
ANS61c: lab variants preroll (ad-state class, ad set up before the press) + prerollhid (undetected ad: Kestrel plays it, Back, press again = loop breaker) in tools/sitelab/soaklab.py page_ho and tools/emu/soak.py HO_CASES/ho_scene (branches adpage/adloop); py_compile ok; NOT yet run on the emulator

## Do this next
dispatch android.yml app=lab lab_suite=ho on this branch (mcp__github__actions_run_trigger, inputs app=lab lab_suite=ho), read the result (get_workflow_job, artifact emulator-lab-text via fetch_artifact.py); full npm test result in scratchpad webtests.log; then bump geckoapp versionCode 163 / 0.3.0-alpha62 + docs/handoff/ALPHA62_RELEASE_NOTE.txt + sync-ext-version + ship.sh gecko

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  25e3ced5 ckpt 2579: ANS61: :core:test 1413/0 failed (OwnerRulesTest guard added), :geckoapp:comp
  a86c1e78 ckpt 2578: ANS61b core+app+page done: WebHandOver ad rules (Element.ad, looksLikeAdAddr
  59855a57 ckpt 2577: ANS61 written into TASKS.md in the owner's words (pre-roll ad played in Kest
  484824ab ckpt 2576: PRIV1 done: INBOX.md, OUTBOX.md and .handoff now live in the PRIVATE repo tj
  18ce6643 ckpt 2575: PRIV1: owner chose group 1 only (INBOX.md, OUTBOX.md, .handoff) and history 
  352c3027 ckpt 2574: PRIV1: owner changed the plan (builds on a PUBLIC repo for free minutes, sen
  9ac3a45b ckpt 2573: alpha61 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha61 (code 162, ru
  6d7a9cdb ckpt 2572: ANS60a-d ticked in TASKS.md with the lab evidence; HANDOFF alpha61 bullet re
  c5280d94 ckpt 2571: alpha61 build DISPATCHED ONCE: run 43975612672 (android.yml app=gecko full_b
  1e7b8d2f ckpt 2570: gated kestrel-g-v0.3.0-alpha61 (gecko code 162) and pushed it: handoff self-
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
