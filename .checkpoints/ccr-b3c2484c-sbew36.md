# CHECKPOINT 2507 — read me first, then TASKS.md

**Written:** 2026-11-16T09:18:06Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `d7d19eb2` (this checkpoint is the commit after it)

## Just done
independent review (9 findings, all real) applied: content.js relays only while a person touched the document, app counts taps inside the page view + keys only when not typing + the tap is used up by a launch, playLike only for a play control, refused press belongs to its page, Kestrel's bar asks, late yes counts, play() after a yes is interrupted until a new press, 3 s repeat guard cleared on failure/end, held script source waits for the answer, frame url whole, ShieldsBridge one parse; web suite 573/573, core + app + gecko unit suites green; the cancelled vp run 43832408756 was a lab flake (emulator boot gave a 16 MB heap limit, engine never ready), proves nothing

## Do this next
dispatch lab suite vp on this head (android.yml app=lab lab_suite=vp lab_minutes=10 lab_net=full full_build=false ref ccr-b3c2484c-sbew36), read it (job via get_workflow_job; artifact emulator-lab-text via fetch_artifact.py; if it opens with 'this boot of the emulated phone gave the app a 16 MB java heap limit' or no-engine it is a flake: dispatch again); every section green incl. ho (file again + from N s); then fill @WEBTESTS@ 573 and @LABCHECKS@ in docs/handoff/ALPHA58_RELEASE_NOTE.txt, bump 159 / alpha58 + sync-ext-version, bash ship.sh gecko with the note, ONE dispatch, record-release, answer owner message 54

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  9ba75b5b ckpt 2506: tap window widened for a slow tablet (core WebHandOver.pressWindowMs 15 s, 3
  51380853 ckpt 2505: ANS54e.9 SECURITY: a page script can fake the gate's question, so every hand
  4ad3a730 ckpt 2504: lab vp run dispatched: 43832408756 (lab job 133180783158, head 6b32808a = ck
  6b32808a ckpt 2503: lab run 43831112200 (ho run 2) read: file now hands over on the real engine 
  57239dd2 ckpt 2502: lab run 43831112200 (suite ho, head bbfc9c3f = ckpt 2501) dispatched; gradle
  bbfc9c3f ckpt 2501: ANS54e: lab run 43826954106 (suite ho) read: overlay/hls/unknown/never ok, f
  00b6ff6b ckpt 2500: lab run 43826954106 dispatched: suite ho (head 97294fcb): web videos play in
  97294fcb ckpt 2499: ANS54e: full web suite 562/562, gradle all green (before the late-hand-over 
  368a9961 ckpt 2498: ANS54e coded, compiles: core WebHandOver + test (10 ok), Prefs web_in_kestre
  a90fb92d ckpt 2497: ANS54 design written (research/ANS54_DESIGN.md) and sub-rows ANS54e.1-7 in T
```

(13 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
