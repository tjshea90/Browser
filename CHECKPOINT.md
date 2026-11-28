# CHECKPOINT 3582 — read me first, then TASKS.md

**Written:** 2026-11-28T08:19:37Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/elegant-darwin-h7dlay` · **builds on:** `9e7fc88e` (this checkpoint is the commit after it)

## Just done
PDL1 CODED (not yet released): core Handed.kt (HandedStreams registry, HandedResponse one-shot with 2% cut guard, HandedFetcher kind 4 = site first + engine stream as fallback / kind 5 = engine stream only), Model.kt (FETCHER_HANDED 4, FETCHER_ONE_SHOT 5, FetchResponse.oneShot, Kind.HANDED_GONE, Reason.PAGE_FILE 20), Engine.kt (blob/data enqueue, probe marks the job one-shot, no limiter, longer stall, PAGE_FILE on a broken stream, wipe partial, Delete-only row, handedRunning), Planner text, PhoneBackend skips kind 5; Gecko: net/HandedOver.kt, TabDelegates.onExternalResponse keeps the body, BrowserActivity (drop on decline/refusal, kindFor + startNow/phone drop in startDownload, feedsDownload keeps the tab awake), GeckoApp registers kinds 4/5, counters download.handed.*; research/PDL1_PAGE_MADE_DOWNLOADS.md. HandedTest 17/17 green, :geckoapp and :app compile green (needed gradle WITHOUT --offline).

## Do this next
PDL1 remaining: (1) the full suites were started with ./gradlew -q :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest (run it again in the background and poll its log for EXIT if the result is not in a ckpt line below), then :app:lintDebug; (2) OwnerRulesTest-style source tests may read TabDelegates/BrowserActivity text: fix any red test, never skip one; (3) bump geckoapp/build.gradle.kts to versionCode 216 / 0.3.0-alpha115, bash tools/sync-ext-version.sh, write docs/handoff/ALPHA115_RELEASE_NOTE.txt (ASCII, no double quotes or backslashes), bash ship.sh gecko with that note, ONE android.yml dispatch (app=gecko full_build=true), verify tag + jobs + release, bash tools/record-release.sh with the identical note, tick PDL1a-d with evidence (HandedTest, run id), answer the owner in plain words incl. what was NOT run (no Proton account, tablet untested, WebView build unchanged); (4) then ND6a (claim it first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  3261ff02 ckpt 3569: Second owner message (ND6: after PDL1, look for improvements from sent diagn
  59778a61 ckpt 3567: Owner message (Proton Drive zip download fails in Kestrel with 'The site sen
  a50908c2 ckpt 3539: alpha114 SHIPPED and recorded (Release kestrel-g-v0.3.0-alpha114, run 460786
  6ee3c3de ckpt 3537: ND5c: research/ND5_CAPTCHA_LOGIN.md has the Shields-off analysis and the bis
  fca1cfcc ckpt 3536: owner: 'Shields off fixed it' (my.nobullnetworks.com login): ND5c = find whi
  2c6e069e ckpt 3535: ND5 read: research/ND5_CAPTCHA_LOGIN.md written (reCAPTCHA v2 invisible, key
  9acfa854 ckpt 3534: alpha114 (code 215) gated green by ship.sh and DISPATCHED (android.yml gecko
  62e73b2d ckpt 3533: gated kestrel-g-v0.3.0-alpha114 (gecko code 215) and pushed it: handoff self
  8c29adfa ckpt 3532: owner sent a screenshot: my.nobullnetworks.com/login says Captcha verificati
  202c2cbe ckpt 3531: pre-ship: alpha114: Kestrel steps out of the way when you update, and the pa
```

(12 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
