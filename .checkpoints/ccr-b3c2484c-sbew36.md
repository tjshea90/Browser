# CHECKPOINT 2576 — read me first, then TASKS.md

**Written:** 2026-11-16T23:48:52Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `00087d4a` (this checkpoint is the commit after it)

## Just done
PRIV1 done: INBOX.md, OUTBOX.md and .handoff now live in the PRIVATE repo tjshea90/Kestrel-Private (cloned at /home/user/kestrel-private); here they are symlinks hidden by .git/info/exclude; tools/private.sh (setup at every autosave/capture/resume, push at the end of autosave) + tools/PRIVATE_ON switch; the three paths were git rm --cached (old text stays in public history, no rewrite)

## Do this next
verify the next turn's message lands in the private repo (git -C /home/user/kestrel-private log -1 and grep the message) and that git status here stays clean of the three paths; a NEW container must re-clone: add_repo tjshea90/Kestrel-Private then git clone --depth 1 https://github.com/tjshea90/kestrel-private /home/user/kestrel-private (resume.sh prints a loud warning when it is missing); optional: CLAUDE.md line about it

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  18ce6643 ckpt 2575: PRIV1: owner chose group 1 only (INBOX.md, OUTBOX.md, .handoff) and history 
  352c3027 ckpt 2574: PRIV1: owner changed the plan (builds on a PUBLIC repo for free minutes, sen
  9ac3a45b ckpt 2573: alpha61 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha61 (code 162, ru
  6d7a9cdb ckpt 2572: ANS60a-d ticked in TASKS.md with the lab evidence; HANDOFF alpha61 bullet re
  c5280d94 ckpt 2571: alpha61 build DISPATCHED ONCE: run 43975612672 (android.yml app=gecko full_b
  1e7b8d2f ckpt 2570: gated kestrel-g-v0.3.0-alpha61 (gecko code 162) and pushed it: handoff self-
  d414de3b ckpt 2569: FINAL lab vp run 43971358668 (head ed7587da, includes all review fixes): 12 
  48102393 ckpt 2568: JVM suites (core, app, geckoapp) green on the head with the review fixes; fi
  2e34191c ckpt 2567: review of alpha61 applied (5 small fixes, unit tests 26/26 in PlayerGestures
  d98f2fd2 ckpt 2566: lab vp dispatched on head 8addd28f (cp warm-up page): run 43966056564 starte
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
