# CHECKPOINT 2660 — read me first, then TASKS.md

**Written:** 2026-11-18T03:23:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `becf9ce4` (this checkpoint is the commit after it)

## Just done
VS1d: vidfast 'stuck on loading' recorded in TASKS (owner words). Lab: vidlab_srv.py got /watch3?v=mse (movie page with a Play button that sets the iframe address, like doraby's servers.js) + /embed/mse (a MediaSource player that waits for sourceopen, appends a fragmented MP4 and plays by itself; reports sourceopen/playing/playrejected/stuck to /state in the lab log); soak.py vs_scene variant mse (tap the page's Play, 16 s, read the states; 'player-stuck' = the owner's symptom).

## Do this next
dispatch ONE more lab: lab_suite=vs (now includes mse as the FIRST variant) ref ccr-587430ac-royssd lab_minutes=5 lab_profile=standard; read it with the earlier vs/dm/ql runs; if mse shows stuck (no sourceopen): fix = a tap on the PARENT page that leads to the frame opens that frame's gate (content.js top-frame trusted tap on a play/server-looking control -> background tap time -> sub_frame inherits within 8 s -> guard answer flag -> gate cfg '1p')

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5375bf9c ckpt 2659: alpha69 CORRECTION: run 44224672021 did NOT build: test-jvm FAILED on GitHub
  de4ea102 ckpt 2658: QL1 + VS1 + DM2 coded and self-tested. QL1: page side (quality-main.js, qual
  96a653ee ckpt 2657: Owner message 3 of this session written into TASKS.md BEFORE any code: VS1 (
  1587c92f ckpt 2656: QL1 page side DONE: quality-main.js (MAIN: notes hls.js / dash.js / Shaka pl
  b73892a9 ckpt 2655: QL1 written into TASKS.md in the owner's words (quality choice in full scree
  8033e054 ckpt 2654: alpha69 DISPATCHED ONCE: android.yml run 44224672021 (run_number 225, app=ge
  cba14e7e ckpt 2653: gated kestrel-g-v0.3.0-alpha69 (gecko code 170) and pushed it: handoff self-
  f8747df3 ckpt 2652: CR1: lab run 44214894368 READ: inconclusive (emulated system died twice, You
  250c6e4b ckpt 2651: DM1 coded: (1) BrowserActivity.openDownloadsAsked + core ManagerIntent (a re
  70eca79a ckpt 2650: Owner message 2 of this session written into TASKS.md BEFORE any code: DM1 (
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
