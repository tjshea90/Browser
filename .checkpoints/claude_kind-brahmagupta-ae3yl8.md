# CHECKPOINT 3113 — read me first, then TASKS.md

**Written:** 2026-11-24T00:51:18Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `f3284c94` (this checkpoint is the commit after it)

## Just done
owner request PERF18 (research online what major browsers do for fast loading, smooth video, smooth animations, tab-switch and Back caching; implement the findings) written into TASKS.md in his words; PERF17 (lazy restore after 4 h like Firefox for Android, laggy animations, slow bot checks) still in progress: findings so far in this ckpt's next

## Do this next
(1) research/BROWSER_TECHNIQUES.md from web searches (PERF18a); (2) PERF17b lazy restore design is decided and NOT yet coded: core LazyStart (AWAY_MS 4 h), Prefs.lazyStart + lastLeftAt written in onPause, BrowserActivity.restore() sets sel.showHome=true + sel.lazyStart when applies (earlyCover skips it), HomeView Continue row, Settings toggle, tests; Firefox for Android opens on the homepage after 4 h away (support.mozilla.org kb set-opening-screen-firefox-android); (3) animations: nothing measured on the tablet for page animation smoothness: owner has lab_soft=true and Calmer redrawing OFF; layout.frame_rate also sets global vsync (scroll goes 30 too); (4) bot checks: no instrument yet: add a challenge marker to pagetime.js (title/cdn-cgi markers) so the digest can time bot checks; (5) evidence: parent process Gecko thread 64-95% busy at page loads (extension request judging ~1.2-1.8 ms each, ~1500 requests on heavy pages), Shields A/B on the heavy chat app 41 s up vs 24 s down

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  9dcb9f9f ckpt 3111: owner request PERF17 written into TASKS.md in his words (a-e): faster speed,
  77f06ebc ckpt 3110: alpha99 SHIPPED and recorded: run 45292001575 green on attempt 1 (check, tes
  5ae527e4 ckpt 3108: alpha99 run is 45292001575 (workflow_dispatch, started 23:48Z, head da26da05
  fad5637b ckpt 3107: alpha99 (code 200) gated and pushed (ckpt 3106); workflow dispatched ONCE on
  da26da05 ckpt 3106: gated kestrel-g-v0.3.0-alpha99 (gecko code 200) and pushed it: handoff self-
  feb56817 ckpt 3104: PERF16 a-d done: focus_report, sec lines, journal bookkeeping thinning, advi
  a6a2207d ckpt 3092: owner after alpha98: it seems faster; NEW PERF16: focus future diagnostics o
  c5e4207f ckpt 3091: alpha98 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha98 (gecko code 199, run
  946e2e68 ckpt 3089: alpha98 (gecko code 199) gated by ship.sh and ONE dispatch made on claude/ki
  665bc7a9 ckpt 3088: gated kestrel-g-v0.3.0-alpha98 (gecko code 199) and pushed it: handoff self-
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
