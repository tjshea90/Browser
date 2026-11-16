# CHECKPOINT 2574 — read me first, then TASKS.md

**Written:** 2026-11-16T23:39:35Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `1e981459` (this checkpoint is the commit after it)

## Just done
PRIV1: owner changed the plan (builds on a PUBLIC repo for free minutes, sensitive files in a PRIVATE repo): reverted the update address to tjshea90/Browser, kept the optional dual publish in publish-release.sh (no token = skipped), removed the guide; waiting for the owner to say what is sensitive

## Do this next
wait for the owner's answer on what moves to the private repo (INBOX.md, OUTBOX.md, .handoff, .checkpoints, CHECKPOINT.md?); then design the split: tools/capture_inbox.sh, capture_outbox.sh, autosave.sh, ckpt.sh, sync.sh, resume.sh all assume these files live in this repo

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M app/src/main/kotlin/app/kestrel/browser/update/UpdateManager.kt
     M app/src/test/kotlin/app/kestrel/browser/update/UpdateCheckTest.kt
    D  docs/PRIVATE-REPO.md

## Last ten checkpoints
```
  9ac3a45b ckpt 2573: alpha61 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha61 (code 162, ru
  6d7a9cdb ckpt 2572: ANS60a-d ticked in TASKS.md with the lab evidence; HANDOFF alpha61 bullet re
  c5280d94 ckpt 2571: alpha61 build DISPATCHED ONCE: run 43975612672 (android.yml app=gecko full_b
  1e7b8d2f ckpt 2570: gated kestrel-g-v0.3.0-alpha61 (gecko code 162) and pushed it: handoff self-
  d414de3b ckpt 2569: FINAL lab vp run 43971358668 (head ed7587da, includes all review fixes): 12 
  48102393 ckpt 2568: JVM suites (core, app, geckoapp) green on the head with the review fixes; fi
  2e34191c ckpt 2567: review of alpha61 applied (5 small fixes, unit tests 26/26 in PlayerGestures
  d98f2fd2 ckpt 2566: lab vp dispatched on head 8addd28f (cp warm-up page): run 43966056564 starte
  8addd28f ckpt 2565: lab cp scene made robust against a race found by reading the extension (firs
  abc3386d ckpt 2564: lab vp run 43960432251 (head 5beb6f7e) finished: new ANS60 checks all pass (
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
