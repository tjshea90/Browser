# CHECKPOINT 2781 — read me first, then TASKS.md

**Written:** 2026-11-21T05:46:32Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `709ac08b` (this checkpoint is the commit after it)

## Just done
BK10 coded + tested: (1) extension: blocking main_frame listener turns Cache-Control no-store into no-cache for web pages only when the app's 7th page-answer field says so (Settings > Instant Back, default ON); never private tabs / non-GET / non-200 / non-HTML / user:pw addresses / mail, bank, payment, sign-in, account, health hosts, brands and paths (bkSensitive); web-tests background.test.js 126 of 126 incl. 4 new BK10 tests (harness got URL + a bkHeaders slot). (2) app: Prefs.instantBack, Settings row, ShieldsBridge 7th field + noStoreRelaxed, BackLab.noStoreRelaxed + Back line counts relaxed pages (core BackLabTest +2, OwnerRulesTest ownerBackButtonRequestIsActedOn; core 89 + app 450 green). (3) BackPictures.MAX 4 -> 8. (4) RestoreOverlay.setWait: a loading line after 1 s and 'Still loading <host>' after 4 s on a waiting Back picture. (5) NEW lab suite 'back' (soak.py back_scene + back_emu prefix + backlab /login/back-nostore control + workflow choices): static baseline, no-store with Instant Back, sign-in-path control. NOT yet proven on the real engine.

## Do this next
BK10d: dispatch the lab ONCE: mcp__github__actions_run_trigger run_workflow android.yml ref claude/sweet-gates-gr3bj5 inputs app=lab lab_suite=back lab_profile=standard lab_net=full lab_minutes=5; poll jobs (get_workflow_job / list_workflow_jobs) until done; fetch the text artifact (python3 -B -I tools/emu/fetch_artifact.py <url> <empty dir>) and read the BK10 rows: static persisted?, nostore-instant-back (persisted or no new_get = WORKS; new_get = the header change did not reach the cache), sign-in control (new_get expected). Write the result into research/BACK_SCROLL.md section 6, tick BK5 / BK10a-b, then ship alpha80 (versionCode 181, ship.sh, ONE dispatch app=gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d91a5fd5 ckpt 2780: BK10 written into TASKS.md in the owner's words (Back button: slow load whil
  a70d55af ckpt 2779: alpha79 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha79, gecko code 180, run
  5a64fefc ckpt 2778: alpha79 run 44849629774: test-jvm failed ONCE on PhoneBackendTest.aPhoneThat
  a3a89687 ckpt 2777: REL79b: alpha79 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  e04b99e6 ckpt 2776: gated kestrel-g-v0.3.0-alpha79 (gecko code 180) and pushed it: handoff self-
  79a107c9 ckpt 2775: ATT1: file-chooser hardening coded + tested (core FilePickRulesTest + OwnerR
  299f3c4b ckpt 2774: ATT1 written into TASKS.md in the owner's words (new owner message: attachin
  9a0d1e24 ckpt 2773: alpha78 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha78, gecko code 179, run
  c8ad5f40 ckpt 2772: REL78b: alpha78 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  74fab19a ckpt 2771: gated kestrel-g-v0.3.0-alpha78 (gecko code 179) and pushed it: handoff self-
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
