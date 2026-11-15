# CHECKPOINT 2419 — read me first, then TASKS.md

**Written:** 2026-11-15T19:33:55Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `2aeb15dd` (this checkpoint is the commit after it)

## Just done
alpha49 review (a1b60178ab815123f) fixed: RA1 media state forgotten with the session (Tab.session setter + consumers check session), RA2 bars read from decor rootWindowInsets (the root listener hears zero on Android 10), RA3 onDestroy cleanup, RA4 pure TurnSettleMachine + 12 tests (found: a second turn during a turn was ignored after the video was paused); guard tests updated; core tests + compile green

## Do this next
NEXT: (1) CANCEL the pending lab run (dispatched before these fixes) and re-dispatch app=lab lab_suite=fsy lab_minutes=10 lab_net=full with this head (find the pending run id with actions_list list_workflow_runs status=queued/pending); (2) compat run 43747605584 ends about 20:10Z: read it into research/COMPAT_RUN1.md; (3) read the fsy report: bars note must say KEPT with nohide, strip rows, turn settled lines, Kestrel bar in a full-screen web component, yahoofs-* pictures, sound table; fix what it shows; (4) ship alpha50 (versionCode 151) with RA1-RA4 + the lab findings: bump geckoapp/build.gradle.kts, bash ship.sh gecko note, dispatch, record-release with the identical note only after green; (5) final answer to the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M HANDOFF.md
     M TASKS.md

## Last ten checkpoints
```
  2de3f20b ckpt 2418: alpha49 shipped and recorded: Release kestrel-g-v0.3.0-alpha49 (run 43751318
  7e7d3319 ckpt 2417: lab scene states fixed (the app turns a wide video sideways by itself at ful
  0a81c2b8 ckpt 2416: gated kestrel-g-v0.3.0-alpha49 (gecko code 150) and pushed it: handoff self-
  afdbb7c6 ckpt 2415: pre-ship: alpha49: full-screen video fixes after your three screenshots. (1)
  7c987e9f ckpt 2414: code for owner message 46 + second review: FSB1 (applyImmersive without LAYO
  7f2c4c92 ckpt 2413: owner message 46 written into TASKS.md (FSB1 bars cover the controls in full
  b8c5d333 ckpt 2412: hour soak 43740398387 read (commit 281c48f4, hotspot-shaped, 70 min, 79 scen
  9edf1ab7 ckpt 2411: LAB2 harness written: lab-only page-health probe (vfind.js probe(), guard an
  4a9a28f4 ckpt 2410: SND1a lab scene written: lab page /video/audible?kind=video|audio (big butto
  44f9516c ckpt 2409: wrote owner message 45 into TASKS.md in the owner's words: AU1 (sound must n
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
