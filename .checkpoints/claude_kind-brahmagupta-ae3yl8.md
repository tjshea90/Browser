# CHECKPOINT 3221 — read me first, then TASKS.md

**Written:** 2026-11-24T08:23:05Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `cdad18e9` (this checkpoint is the commit after it)

## Just done
alpha105 (code 206) SHIPPED: run 45352897624 green, Release kestrel-g-v0.3.0-alpha105 has the APK, recorded in BUILDLOG; PERF25b/c ticked, PERF25a waits for the owner's answer (which device Brave ran on) and the tablet result of the hardware drawing trial

## Do this next
Waiting for the owner's tablet test of the hardware drawing trial (Settings > Network lab > Hardware drawing trial 1 then 2, restart, claude.ai table + YouTube, look for bands). Then: (1) python3 -B -I tools/diag_digest.py --days 2: read engine draw-trial lines (start / steady / reverted), engine draw-prefs read-back (do the 5 trial prefs show the new values?), SETTING A/B drawing=hardware trial N vs software (fm= frames/s, first paint, typing); (2) trial good: bisect to the smallest set, then consider making it the default for Kestrel G; trial bands/crash: record in research/BRAVE_SMOOTH.md section 5 and move to cheaper CPU drawing; (3) open: PERF17c/d (animations + Cloudflare checks), PERF19 cheaper blocker, PERF23 (Cache2 I/O, Classifier updater, HeapTaskDaemon), PERF14e Calmer redrawing A/B, PERF9 second film, Back MISSED false positive. Next version alpha106 / code 207. No heartbeat trigger exists.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  6fb30f29 ckpt 3219: alpha105 (code 206) gated (ckpt 3218, head 8b6eebf7) and dispatched ONCE: ru
  8b6eebf7 ckpt 3218: gated kestrel-g-v0.3.0-alpha105 (gecko code 206) and pushed it: handoff self
  c45aae47 ckpt 3217: PERF25: app unit tests green; bumped to alpha105 / code 206, release note do
  512c9e18 ckpt 3215: PERF25c: built the hardware drawing trial (core DrawTrial + DrawTrialTest + 
  6d20d18a ckpt 3203: wrote PERF25 (owner: Brave is smooth on the parlayapi site) into TASKS.md, n
  f3c4d330 ckpt 3202: alpha104 (code 205) SHIPPED: run 45346018174 green, Release kestrel-g-v0.3.0
  e168b9a7 ckpt 3199: alpha104 (code 205) gated (ckpt 3198, head 96feacef) and dispatched ONCE: ru
  96feacef ckpt 3198: gated kestrel-g-v0.3.0-alpha104 (gecko code 205) and pushed it: handoff self
  c6ef6bb0 ckpt 3197: pre-ship: alpha104: tab reloads, and the clip page. I read your two alpha103
  b46f56e2 ckpt 3193: PERF24 coded: tab sleep plan (core LiveBudget.sleepPlan + Tab.reloadMs + Pag
```
