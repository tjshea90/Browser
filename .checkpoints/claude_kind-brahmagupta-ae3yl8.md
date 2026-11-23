# CHECKPOINT 3087 — read me first, then TASKS.md

**Written:** 2026-11-23T21:16:01Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `5b028ada` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha98: switching between tabs no longer reloads them. Your diagnostics show why it did: Android tells apps that they are about to be ended within one second of EVERY time you leave Kestrel on this tablet (even with 400 to 540 MB of memory free and the low-memory limit at 144 MB), and Kestrel answered every such message by closing every tab except the one in front, so the next tab you switched to had to load its page again, which takes many seconds on heavy pages. Of the 30 tabs put to sleep in your logs, 16 were closed this way. Now Kestrel follows the free memory: below 300 MB only the tab in front stays, below 450 MB at most two tabs, otherwise your Tabs kept ready setting stands; so with Few (2 tabs) both tabs stay alive when you leave and come back. The risk, said plainly: Android may still end the whole app while you are away with two tabs open; the tabs then come back with their history as after any kill, and the diagnostics will show if that happens more often. New measurement: how long a tab switch takes until the first picture, for a live tab and for a sleeping one (the diagnostics TABS section), so the lag between two live tabs can be measured next. Not run on the tablet.

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a5e04498 ckpt 3085: PERF15 coded (not gated): LiveBudget.afterTrim level 60+ follows free memory
  adeb2192 ckpt 3082: owner after alpha97: Back is faster and images attach (CONFIRMED); NEW PERF1
  8a9c210e ckpt 3081: alpha97 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha97 (gecko code 198, run
  a7a096c8 ckpt 3079: alpha97 run 45259809870 FAILED at test-jvm on the GitHub runner: core Stream
  d47330ac ckpt 3078: alpha97 (gecko code 198) gated by ship.sh (core + app + geckoapp suites, han
  80d592db ckpt 3077: gated kestrel-g-v0.3.0-alpha97 (gecko code 198) and pushed it: handoff self-
  fc06903d ckpt 3074: PERF14 coded (not gated): photo attach copy (core PickStore + tests, Browser
  75f112b7 ckpt 3059: PERF14a research written: photo attach root cause (GeckoView 160 FilePrompt.
  678153fe ckpt 3057: PERF14 written to TASKS.md (owner after alpha96: Back still slow although th
  d8a9f0e7 ckpt 3056: alpha96 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha96 (gecko code 197, run
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
