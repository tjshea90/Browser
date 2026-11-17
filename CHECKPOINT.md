# CHECKPOINT 2587 — read me first, then TASKS.md

**Written:** 2026-11-17T01:18:27Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `fe809368` (this checkpoint is the commit after it)

## Just done
LAB3 (owner message 62: github runner + hardware-matched emulator + daily-use simulation + findings report for a build) written into TASKS.md in his words as LAB3a-g. Private repo clone restored for this container (add_repo + clone + register_repo_root done). Finding: the lab already exists (emu-lab.yml + tools/emu/soak.py, suites smoke..compat, research/EMU_LAB.md, COMPAT_RUN1.md); the plan is to extend it, not rebuild it

## Do this next
LAB3a: check what sdkmanager offers for an Android 10 Go image (sdkmanager --list | grep -i go) and diff the AVD in emu-lab.yml against docs/HARDWARE.md; then LAB3b/c new suite 'daily' in tools/emu/soak.py (look at class Phone for UI helpers: typing in the address bar, tab switching); then LAB3d tools/emu/buildreport.py. No heartbeat trigger created yet

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  fe809368 ckpt 2586: alpha62 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha62 (code 163, ru
  a29d8d50 ckpt 2585: alpha62 build DISPATCHED ONCE: run 43989321170 (app=gecko full_build=true, h
  e7fd1dd9 ckpt 2584: gated kestrel-g-v0.3.0-alpha62 (gecko code 163) and pushed it: handoff self-
  75920d6d ckpt 2583: ANS61a-c ticked in TASKS.md with evidence: lab ho run 43986542855 = 61 check
  eb522196 ckpt 2582: alpha62 prepared but NOT shipped: geckoapp versionCode 163 / 0.3.0-alpha62, 
  241f219d ckpt 2581: lab ho run 43986542855 DISPATCHED (head 977ab6a8, app=lab lab_suite=ho): pro
  977ab6a8 ckpt 2580: ANS61c: lab variants preroll (ad-state class, ad set up before the press) + 
  25e3ced5 ckpt 2579: ANS61: :core:test 1413/0 failed (OwnerRulesTest guard added), :geckoapp:comp
  a86c1e78 ckpt 2578: ANS61b core+app+page done: WebHandOver ad rules (Element.ad, looksLikeAdAddr
  59855a57 ckpt 2577: ANS61 written into TASKS.md in the owner's words (pre-roll ad played in Kest
```
