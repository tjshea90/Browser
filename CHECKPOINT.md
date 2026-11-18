# CHECKPOINT 2662 — read me first, then TASKS.md

**Written:** 2026-11-18T03:57:24Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `df57854f` (this checkpoint is the commit after it)

## Just done
FD1 floating download window done: 8 DownloadPopupFloatTest + OwnerRulesTest + FloatBoxTest green (fixed: drag from title bar judged by where the finger LANDED, not the first move). VP2 gate inheritance + NM1 naming coded earlier, web tests 614/614. alpha69 NOT released (run 44224672021 must not be rerun).

## Do this next
1) run the full JVM suite ALONE in the background (./gradlew --offline -q :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest > scratchpad/jvm-suite5.log; the earlier 1668-test run lost its XML files because two gradle runs overlapped) and fix real failures; 2) dispatch ONE lab run lab_suite=new (dm+ql+vs) lab_profile=standard lab_net=full lab_minutes=5 on branch ccr-587430ac-royssd, read the report; 3) rewrite docs/handoff/ALPHA69_RELEASE_NOTE.txt, bash ship.sh gecko note, ONE fresh run_workflow dispatch, verify tag kestrel-g-v0.3.0-alpha69, record-release, tick TASKS items, answer owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  bb4a5af6 ckpt 2661: Owner message 5 of this session written into TASKS.md BEFORE any code: FD1 (
  78aff752 ckpt 2660: VS1d: vidfast 'stuck on loading' recorded in TASKS (owner words). Lab: vidla
  5375bf9c ckpt 2659: alpha69 CORRECTION: run 44224672021 did NOT build: test-jvm FAILED on GitHub
  de4ea102 ckpt 2658: QL1 + VS1 + DM2 coded and self-tested. QL1: page side (quality-main.js, qual
  96a653ee ckpt 2657: Owner message 3 of this session written into TASKS.md BEFORE any code: VS1 (
  1587c92f ckpt 2656: QL1 page side DONE: quality-main.js (MAIN: notes hls.js / dash.js / Shaka pl
  b73892a9 ckpt 2655: QL1 written into TASKS.md in the owner's words (quality choice in full scree
  8033e054 ckpt 2654: alpha69 DISPATCHED ONCE: android.yml run 44224672021 (run_number 225, app=ge
  cba14e7e ckpt 2653: gated kestrel-g-v0.3.0-alpha69 (gecko code 170) and pushed it: handoff self-
  f8747df3 ckpt 2652: CR1: lab run 44214894368 READ: inconclusive (emulated system died twice, You
```

(24 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
