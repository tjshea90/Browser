# CHECKPOINT 2581 — read me first, then TASKS.md

**Written:** 2026-11-17T00:18:55Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-eb19b61e-9kt9nz` · **builds on:** `977ab6a8` (this checkpoint is the commit after it)

## Just done
lab ho run 43986542855 DISPATCHED (head 977ab6a8, app=lab lab_suite=ho): proves preroll (ad-state class: not taken over, page plays ad then film) and prerollhid (undetected ad taken, played to its end, Back, press again: NOT handed over again, page plays it then the film)

## Do this next
wait for run 43986542855 (background until-loop, then actions_list list_workflow_jobs / get_workflow_job, NOT get_workflow_run); read emulator-lab-text artifact (download_workflow_run_artifact + python3 -I tools/emu/fetch_artifact.py <url> <empty dir>) for the ho checks 'preroll:' and 'prerollhid:'; if all ok bump geckoapp versionCode 163 / 0.3.0-alpha62, docs/handoff/ALPHA62_RELEASE_NOTE.txt, bash tools/sync-ext-version.sh, bash ship.sh gecko note, dispatch app=gecko full_build=true

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  977ab6a8 ckpt 2580: ANS61c: lab variants preroll (ad-state class, ad set up before the press) + 
  25e3ced5 ckpt 2579: ANS61: :core:test 1413/0 failed (OwnerRulesTest guard added), :geckoapp:comp
  a86c1e78 ckpt 2578: ANS61b core+app+page done: WebHandOver ad rules (Element.ad, looksLikeAdAddr
  59855a57 ckpt 2577: ANS61 written into TASKS.md in the owner's words (pre-roll ad played in Kest
  484824ab ckpt 2576: PRIV1 done: INBOX.md, OUTBOX.md and .handoff now live in the PRIVATE repo tj
  18ce6643 ckpt 2575: PRIV1: owner chose group 1 only (INBOX.md, OUTBOX.md, .handoff) and history 
  352c3027 ckpt 2574: PRIV1: owner changed the plan (builds on a PUBLIC repo for free minutes, sen
  9ac3a45b ckpt 2573: alpha61 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha61 (code 162, ru
  6d7a9cdb ckpt 2572: ANS60a-d ticked in TASKS.md with the lab evidence; HANDOFF alpha61 bullet re
  c5280d94 ckpt 2571: alpha61 build DISPATCHED ONCE: run 43975612672 (android.yml app=gecko full_b
```
