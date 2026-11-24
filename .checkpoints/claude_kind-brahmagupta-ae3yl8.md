# CHECKPOINT 3197 — read me first, then TASKS.md

**Written:** 2026-11-24T06:57:59Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `7b2d81ae` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha104: tab reloads, and the clip page. I read your two alpha103 uploads. (1) TABS RELOAD: the tab that kept going to sleep was your heavy chat page (it takes 13 to 23 seconds to paint again, 36 seconds to finish). Your other two live tabs were private tabs, which can never sleep, and the setting Tabs kept ready, Few (2), counted them, so the regular tab paid for them every time, even with 944 MB free. Now that number limits your regular tabs only, private tabs do not count against it, free memory still limits all live tabs (and now also caps a fixed number, so Many no longer ignores a memory shortage), and when a tab has to sleep the cheapest one to bring back goes first: a tab whose last page took 8 seconds or more to load is the last one put to sleep. (2) THE CLIP PAGE: the uploads show clips dropping 40 to 99 percent of their frames while three or four played within seconds of each other, and the app's own window stalling up to 0.98 seconds. Two clips at a time, the alpha102 limit, was still too many for this tablet, so now one clip plays at a time, and none while you scroll the page; they start again half a second after you stop. (3) TYPING AND SCROLLING: typing in text boxes waits for the page's own next picture (up to 1.2 seconds on busy pages, while the keyboard's own hand-over takes 16 to 24 milliseconds), and scrolling shows the same thing: pages draw about 6 pictures a second right after they load. The clip change removes the biggest source of late pictures on the clip page; the rest is the pages' own scripts on four weak cores, so there is no separate fix. One setting to try, no code: Settings, Appearance, Scroll feel: yours is Standard (Firefox's own), the least pre-drawing; the default is Smoother. Not run on the tablet: the tab rule and the clip limiter are tested in unit tests and in a desktop browser engine only. Said plainly: a heavy page that does have to reload still takes 13 to 23 seconds; the change makes it sleep far less often, not load faster.

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b46f56e2 ckpt 3193: PERF24 coded: tab sleep plan (core LiveBudget.sleepPlan + Tab.reloadMs + Pag
  64c28c57 ckpt 3188: owner request PERF24 (laggy typing in text boxes, the sports-odds page and i
  5a4fdea9 ckpt 3187: TEST1: the owner's step-by-step test guide written (research/TEST_PLAN_alpha
  2775495b ckpt 3186: alpha103 SHIPPED and recorded: run 45333801406 green on attempt 1, Release h
  4a09769f ckpt 3184: alpha103 (code 204) gated (ckpt 3183, head 3596f285) and dispatched ONCE: ru
  3596f285 ckpt 3183: gated kestrel-g-v0.3.0-alpha103 (gecko code 204) and pushed it: handoff self
  cb169fe5 ckpt 3182: pre-ship: alpha103: the deep low-end research, three speed experiments, and 
  ee5eb3f1 ckpt 3179: PERF22: research/LOW_END_PLAYBOOK.md written; three Network lab experiments 
  3b1d4574 ckpt 3175: FLK1 done: real-time waits tripled in StreamEngineTest, PhoneBackendTest, Se
  9e7ae1ef ckpt 3173: alpha102 SHIPPED and recorded: run 45326840944 green on attempt 2 (attempt 1
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
