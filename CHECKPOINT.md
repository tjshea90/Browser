# CHECKPOINT 382 — read me first, then TASKS.md

**Written:** 2026-11-11T16:23:18Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `3e0fcfe` (this checkpoint is the commit after it)

## Just done
Owner request 6 recorded in TASKS.md (R1 neon not visible, R2 scrolling stutter/white space/unresponsive split second + 'implement what other browsers do', R3 pull-to-refresh). It came 3 min after alpha6 was published, so the owner is on alpha5 or older. Plan: evidence on the emulator first (scroll away/back test, dump real default prefs), then implement R1 (drop the areAnimatorsEnabled gate, stronger pulse, more places), R3 (pull-to-refresh via GeckoView InputResultDetail + a wrapper that intercepts a downward drag at the top), R2 (APZ wait, passive touch listeners, speculative connect, priority hint, memory/live-session policy) and ship alpha7.

## Do this next
1) emulator is running (adb root works): measure scroll-away/back blank time on /site/long.html with alpha6 prefs; extend GeckoEngine.readBackPrefs with candidate prefs (dom.event.default_to_passive_touch_listeners, apz.*, image.mem.*, browser.sessionhistory.max_total_viewers, gfx.webrender.*) and read them in Diagnostics; 2) Neon.kt: remove ValueAnimator.areAnimatorsEnabled() gate, stronger core stroke, rest policy (Home/tab grid never rest), Diagnostics line 'neon: running/held because ...', Settings subtitle; 3) PullToRefresh: KestrelGeckoView.onTouchEventForDetailResult -> canScrollUp; wrapper intercepts downward drag; indicator chip; reload on release; 4) implement R2 items on evidence; 5) bump 106 / 0.2.0-alpha7, ship.sh gecko, dispatch android.yml (ToolSearch select mcp__github__actions_run_trigger,mcp__github__actions_list), record-release only after green; 6) answer the owner (Release link on its own line).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  4ac5081 ckpt 376: alpha6 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha6, gecko code 105, run 430556
  69d37b2 ckpt 372: gated kestrel-g-v0.2.0-alpha6 (gecko code 105) and pushed it: handoff self-tes
  3c8f9fe ckpt 371: pre-ship: alpha6: a page that Android frees to save memory now comes back wher
  1d4be6f ckpt 369: Q2 work for alpha6 (code compiles, emulator-tested: Q2a recovery + Scroll test
  6339191 ckpt 352: alpha5 SHIPPED + RECORDED (run 42989432755 green, kestrel-g-v0.2.0-alpha5, APK
  e9f12cd ckpt 344: gated kestrel-g-v0.2.0-alpha5 (gecko code 104) and pushed it: handoff self-tes
  b84308a ckpt 342: P3+P4 DONE and verified on the emulator (blue REAPER theme: Home flames/scythe
  bf1ed62 ckpt 316: P3/P4 neon wired and compiling (both modules): Neon engine (RenderThread alpha
  6438c0a ckpt 306: Owner request 4 recorded (P1-P4: hardware speed research, engine research incl
  8d8143c ckpt 301: N6 step 2 compiles: Android download shell (SqliteStore, MediaStoreSinks, UrlC
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
