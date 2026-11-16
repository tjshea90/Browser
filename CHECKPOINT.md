# CHECKPOINT 2575 — read me first, then TASKS.md

**Written:** 2026-11-16T23:43:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `b0784a2f` (this checkpoint is the commit after it)

## Just done
PRIV1: owner chose group 1 only (INBOX.md, OUTBOX.md, .handoff) and history cleaning only if easy: it is not (rewriting pushed history is forbidden by the project rules and every branch/tag/release points at those commits), so no cleaning; private repo Kestrel-Private does not exist yet

## Do this next
BLOCKED on the owner creating the empty private repo tjshea90/Kestrel-Private + installing the Claude GitHub app on it; when mcp__claude-code-remote__list_repos query Kestrel shows it: add_repo, clone next to this repo, then change tools/capture_inbox.sh capture_outbox.sh autosave.sh posttool.sh resume.sh sync.sh test_handoff.sh to use that clone (error loudly if it is missing), git rm --cached INBOX.md OUTBOX.md .handoff and add them to .gitignore, run tools/test_handoff.sh and a cold-resume check

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  352c3027 ckpt 2574: PRIV1: owner changed the plan (builds on a PUBLIC repo for free minutes, sen
  9ac3a45b ckpt 2573: alpha61 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha61 (code 162, ru
  6d7a9cdb ckpt 2572: ANS60a-d ticked in TASKS.md with the lab evidence; HANDOFF alpha61 bullet re
  c5280d94 ckpt 2571: alpha61 build DISPATCHED ONCE: run 43975612672 (android.yml app=gecko full_b
  1e7b8d2f ckpt 2570: gated kestrel-g-v0.3.0-alpha61 (gecko code 162) and pushed it: handoff self-
  d414de3b ckpt 2569: FINAL lab vp run 43971358668 (head ed7587da, includes all review fixes): 12 
  48102393 ckpt 2568: JVM suites (core, app, geckoapp) green on the head with the review fixes; fi
  2e34191c ckpt 2567: review of alpha61 applied (5 small fixes, unit tests 26/26 in PlayerGestures
  d98f2fd2 ckpt 2566: lab vp dispatched on head 8addd28f (cp warm-up page): run 43966056564 starte
  8addd28f ckpt 2565: lab cp scene made robust against a race found by reading the extension (firs
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
