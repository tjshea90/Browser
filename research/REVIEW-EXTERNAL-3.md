# REVIEW-EXTERNAL-3 (v2) - outside review of Kestrel G with the owner's answers (read-only, repo-only round)

Written for the Claude session that works in this repo. v2 replaces v1: on 2026-11-14 the owner answered "Yes to all four, and include YouTube ad skipping and blocking" and added a Back-button request. Keep this file ASCII (the repo hooks refuse other characters and absolute home paths).

- Read: branch `claude/keen-mendel-eq59zt` at `83803299` = branch `ccr-a57c163e-mtq8js` up to ckpt 2300 (AY1 coded, AY2+AY4 coded and unit-tested) + auto-checkpoints; alpha39 shipped (gecko versionCode 140). Line numbers are for that tree; if one is off, search for the symbol written next to it.
- Method: ONLY the repo and one read-only copy of the public uAssets list repo (commit `a8f394f`, 2026-11-14, kept outside the repo) were read. No build, no test run, no emulator, no tablet. A few web searches were made; their results were thin and are only cited where they agree with the repo. Nothing was written to the repo by the review.
- Tags: **[R]** read in the repo (file:line) | **[M]** measured on the owner's tablet by an earlier session (file cited) | **[E]** estimate | **[N]** not verified: the named check decides.
- Aliases: `GA` = geckoapp/src/main/kotlin/app/kestrel/browser | `APP` = app/src/main/kotlin/app/kestrel/browser | `CORE` = core/src/main/kotlin/app/kestrel/core | `EXT` = geckoapp/src/main/assets/kestrel-shields | `WT` = web-tests

---

## START HERE - what you must do, in this order (exact)

This file IS the owner's request. Do not ask the owner to repeat anything in it.

1. **SAVE THIS FILE FIRST.** Exact repo-relative path: `research/REVIEW-EXTERNAL-3.md` (a new file; no other file uses the name; do not rename it, the TASKS rows point at it). Use the Write tool with that relative path and write the whole text unchanged. If you received it as an upload, read all of it once first, then write it. It is ASCII, has no absolute home path and holds no secret (the repo is public). Save it on the branch you were assigned (CLAUDE.md names it; today `ccr-a57c163e-mtq8js`); never force-push, no pull request.
2. **WRITE THE REQUEST INTO `TASKS.md` BEFORE ANY CODE** (repo rule): append the block of section 13 (the lines INSIDE its code fence, not the fence itself) at the END of `TASKS.md`, unchanged (it quotes the owner's words, then one box per row, ids `RX3-...`). Then edit the line that starts `- [ ] OWNER LIC-1` (it names "the default of a labelled YouTube-ads switch") and append: ` - YouTube part DECIDED by the owner 2026-11-14 (INBOX): YouTube ads blocked and skipped, default ON; the project-licence part stays open.`
3. **CHECKPOINT:** `bash tools/ckpt.sh "saved research/REVIEW-EXTERNAL-3.md (v2) and wrote the RX3 rows into TASKS.md" "<your previous Do-this-next: finish AY2-AY5 and ship alpha40>, then START RX3-BK1 (Back lab) from research/REVIEW-EXTERNAL-3.md section 3"`.
4. **FINISH WHAT IS IN FLIGHT FIRST.** The work in `CHECKPOINT.md` (AY1-AY5 and alpha40) is already coded; do not leave it half done. While the emulator or a build runs, spend the waiting time on rows that need no emulator (pure JVM / web tests): RX3-B1, RX3-G2, RX3-G3, RX3-V1 core rule, RX3-D1 failing tests, RX3-Y1 scriptlet engine.
5. **THEN WORK THE WAVES of section 0.4, in that order.** Per row: `bash tools/claim.sh RX3-<id>`; write the failing test first; code; `bash tools/ckpt.sh "did" "next"` after every step; prove it with the proof named in the row; tick with `bash tools/claim.sh RX3-<id> --done "<the test or run that proves it>"`. A row whose proof needs the tablet stays `[~]` ("coded, waiting for the owner") with the smallest owner step (one button, one Copy).
6. **WRITE RESEARCH THE MOMENT IT EXISTS**, never only in chat. Where each note goes: section 14 (for example `research/BACK_SCROLL.md`, `research/YOUTUBE_ADS.md`). Numbers from a lab or the tablet go into that file with device, build and date.
7. **SHIP IN GROUPS** (section 0.4) with the usual routine in `HANDOFF.md` section 2 (`bash ship.sh gecko "..."`, dispatch `android.yml`, record the release). Give the owner the Release link as plain text on its own line. Answer EVERY owner message; re-list the open items at the end of each reply.
8. **WHAT TO SAY TO THE OWNER right after step 3** (plain words, 4 lines): "Saved the review as research/REVIEW-EXTERNAL-3.md and wrote its rows into TASKS.md. I finish alpha40 first, then do your Back-button scroll request, then YouTube ad skipping and blocking, then the rest in the order in the file. Nothing in it has run on your tablet yet; I will say plainly what was proven on the emulator and what was not."
9. **RULES THAT ALWAYS APPLY:** follow CLAUDE.md and HANDOFF.md; no cookie, token, Authorization header or full signed URL in any Health line, Diagnostics text or report; private tabs never keep reports, snapshots or stored headers; injected JS stays ES5; no coroutines; no native code; Kotlin framework Views only; never add a `push:` trigger to a workflow; never commit an APK or a key; text you write into the repo stays ASCII with no absolute home path; use the GitHub MCP tools and `add_repo`, not network commands the hooks block; list repos you read stay OUTSIDE the repo tree and are never executed.
10. **IF THE FILE IS WRONG:** line numbers drift - search by symbol. If an `[N]` check fails, drop that row, say so in OUTBOX and tick it `DROPPED: <reason>`. Put corrections at the end of the saved file under a heading `Corrections` so later sessions benefit.

Reading budget: read START HERE, the TL;DR, sections 0-2 now; read a Part (3 to 9) only when you take one of its rows, and only that row's subsection. A session that resumes mid-wave needs `tools/resume.sh`, the ladder row it was on and that row's subsection.

---

## Owner decisions on record (2026-11-14, INBOX)

- "Yes to all four": (a) add the extra tracker lists with a Settings switch (rows B2); (b) stop pop-ups and forced redirects more strictly, a legitimate window sometimes needs one tap on the blocked icon (G5, G6); (c) pause the glow animation while a video plays (S1); (d) skip ads inside video players by default (G7).
- "include YouTube ad skipping and blocking": YouTube ads are blocked AND skipped, default ON (Part Y). This closes the YouTube part of `OWNER LIC-1`.
- Back button request, owner's words: "when I press back to go to the last web page, it automatically goes back to the last scroll position I was at on that web page, and it should load immediately because it should be cached" (Part BK).
- Still open, one question for the owner (ask it once, with the next release): BK5 - may Back be made instant for pages that tell the browser not to keep them (`Cache-Control: no-store`) on sites the owner picks, which keeps a copy of those pages on the tablet?

---

## TL;DR - what matters most

1. **Back button (owner's request):** the engine's back/forward cache makes Back instant when it can, but pages still loading when left, `no-store` pages and video-player pages are never kept, and for those the app shows a saved picture that goes away at first paint - not when the page has scrolled back to its place. Nothing guarantees the scroll position. Fix: measure first (BK1), then keep a scroll position per history entry, keep the picture up until the page is painted AND back at its place, and ask the page to scroll if the engine did not (BK2).
2. **YouTube (owner's request):** the app runs only the older half of the lists' YouTube answer. Of the 34 list rules that apply to `www.youtube.com` the compiler drops 9 (six `trusted-edit-inbound-object`, two `trusted-prevent-dom-bypass`, one 3.5 KB injected script that exceeds the length caps); of the 11 for `m.youtube.com` (the site this tablet gets by default) it drops 1. Fix: finish the scriptlets and caps for trusted lists only (B1 first), add a built-in ad skipper, and build a lab (Y1-Y3).
3. Three holes in the pop-up/redirect guard itself: missing on the FIRST page after a cold start (G1), the "real tap" evidence can be forged by the page (G2), any parent page can make the extension click inside cross-origin iframes (G3).
4. False and ad videos in the Save-video list: a player's metadata report marks ads, previews and muted loops "real" (V1); unverified finds count (V2); ad runs in HLS are removed only after download (V5).
5. A video download dies after 30-120 s of dead upstream while Wi-Fi stays up (D1).
6. Speed, RAM, hotspot: pause the glow during video (S1), budget the page-wide token observer (S3), count and trim the extension's per-request listeners (S2), the blocker averaged 723 us per request on the tablet (S4), back/forward cache follows the live-tab budget (M1), frugal prefs on the capped hotspot (H1).

---

## 0. Conventions and waves

0.1 Row status words: **NEW** = not found in any earlier review or in TASKS; **KNOWN-UNQUEUED** = an earlier document proposed it, no TASKS box; **EVIDENCE** = a queued row (EY-nn, AS1...) that this file only adds evidence to, do not build it twice; **OWNER-ASKED** = the owner wrote it.
0.2 Every `[E]` is a guess: measure it (section 10) before building on it. Every `[N]` is a hypothesis with a named check: run the check first.
0.3 Merge hygiene: the other work in flight touches `GA/BrowserActivity.kt` (AY1/AY2), `EXT/content.js` (AY2 open-space block, lines 311-347), `APP/ui/MiniNav.kt`. Rows G5, G6, BK2 and S1 edit other parts of `BrowserActivity.kt`; keep each edit small and `ckpt` after each; `tools/sync.sh` merges the rest.
0.4 **Waves.** Wave 0 = AY1-AY5, ship alpha40 (already coded). **Wave A (the owner's asks and what they depend on)** = BK1, BK2, B1, Y1, Y2, Y3, G7, G2, G3, G1. Suggested releases: alpha41 = BK1+BK2+B1+G2+G3+G1; alpha42 = Y1+Y2+Y3+G7. **Wave B** = V1, V2, V5, V3, D1, G4, G5, G6, S1, B2. **Wave C** (each gated by a measurement) = BK3, BK4, M1, H1, S2, S3, S4, S6, S5, V4, V6, V7, D2, D3, B3, Y4, Y5, then BK5 after the owner answers.
0.5 Dependencies: B1 before Y1 (long and `trusted-*` rules only from trusted lists); G2 before G5; V5 (early look) before V4; BK1 numbers before BK3; Y1+Y3 before Y2 is declared done.

---

## 1. The asks in one screen

| Ask | State today [R] | Real gap | Rows |
| :- | :- | :- | :- |
| Back: last scroll position, instant | bfcache (6 pages, 1 h) answers most Back presses; a 480 px picture covers reloads after 150 ms and goes at first paint | scroll position not owned by the app; picture ends at paint not at scroll; many pages cannot enter the cache; hit rate never measured | BK1-BK5 |
| YouTube ad blocking and skipping | list rules run by our scriptlet library, switch default ON, never tested on real YouTube | 9 of 34 `www` rules and 1 of 11 `m` rules dropped; no client-side skipper; no lab | Y1-Y5, G7 |
| 1 Speed, loading, playback | decode, draw, scroll measured fine [M]; lean UI code | extension work on two main threads; glow redraws during video; blocker 723 us/request [M] | S1-S6 |
| 2 Finder, fewer false/ad entries | strong finding | weak classifying: ads/previews become "real"; playlists always count; ad runs removed only after download | V1-V7 |
| 3 Download manager | strong engine | HLS fails after ~30-120 s of dead upstream; multi-connection files after ~2 min | D1-D3 |
| 4 RAM | adaptive tab budget, heap 14/128 MB [M] | bfcache fixed at 6 while one kill takes all tabs | M1, M2 |
| 5 Blocking, security | strong engine and lists | extra lists never applied; `trusted-*` from any list; guard holes G1-G3 | B1-B3, G1-G3 |
| 6 First press plays | AP1 assist after a refusal | reactive; first page unguarded; app wall weak; late jumps allowed | G1-G7 |
| 7 Hotspot | Fast Wi-Fi profile built; plain hotspot spends bandwidth on speculative bytes | outages (D1), frugal profile (H1) | H1, D1, S6 |

---

## 2. The ladder (owner's asks first, then value over risk)

| ID | Wave | Pri | Eff | What | Where [R] | Status |
| :- | :- | :- | :- | :- | :- | :- |
| BK1 | A | P0 | M | Back lab: measure hit / reload / network, ms to paint, scroll before and after, why a page is not kept | GA/BrowserActivity.kt:991-1012; EXT/content.js; Diagnostics | OWNER-ASKED |
| BK2 | A | P0 | M | Scroll per history entry; cover stays until painted AND back at its place; page asked to scroll if the engine did not; picture at 0 ms for known-uncached targets | BrowserActivity.kt:979-1024, 2170-2272; APP/web/Tab.kt:61; CORE/session/LastLook.kt:97 | OWNER-ASKED |
| B1 | A | P1 | S | `trusted-*` scriptlets and long arguments only from trusted lists | CORE/filter/RuleParser.kt:210-214; ScriptletRules.kt:25-30, 50, 57-58; APP/shield/FilterUpdater.kt:25-37 | KNOWN-UNQUEUED |
| Y1 | A | P1 | M-L | Finish the YouTube answer of the lists: caps for trusted lists, JSON edit engine + `json-edit*`, `trusted-edit-inbound-object`, `trusted-prevent-dom-bypass` | ScriptletRules.kt; EXT/scriptlets-main.js:746-762; section 4 | OWNER-ASKED |
| Y2 | A | P1 | M | Built-in YouTube ad skipper and hider (own content script for YouTube hosts) | new EXT/youtube.js + manifest.json | OWNER-ASKED |
| Y3 | A | P1 | S-M | YouTube lab (mock + optional live check) and Diagnostics counters | tools/sitelab/ | OWNER-ASKED |
| G7 | A | P1 | M | Skip ads inside other video players (generic, lazy observer) | new, EXT/content.js | OWNER-ASKED (d) |
| G2 | A | P0 | S | Tap evidence recorded by the isolated content script | EXT/content.js:226-239; guard-main.js:27-30,126-128; background.js:275-283; BrowserActivity.kt:2049-2056 | NEW |
| G3 | A | P0 | M | Remove the cross-frame postMessage click channel | EXT/guard-main.js:253-308 | NEW |
| G1 | A | P0 | S-M | Guard on the FIRST page after a cold start | EXT/background.js:285-289, 309-313; manifest.json | NEW |
| V1 | B | P0 | S | A metadata report must not promote ads/previews/loops | EXT/content.js:182-206; CORE/media/MediaList.kt:143-172 | NEW |
| V2 | B | P1 | S | Count an entry only when verified | MediaList.kt:161-172; GA/web/ShieldsBridge.kt:128-143; content.js:243-285 | NEW |
| V3 | B | P1 | M | Playing-element and duration-jump signals | content.js; MediaList; MediaRank.kt | NEW |
| V5 | B | P1 | M | Early look at playlists; ad runs skipped before download | CORE/media/Hls.kt:133-176; StreamEngine.kt:1019-1022; MediaRank.kt:74 | NEW |
| D1 | B | P1 | M | Outage wait for HLS and multi-connection files | StreamEngine.kt:1069-1127, 1328; Engine.kt:743-776 | NEW |
| G4 | B | P1 | M | Neutralize fake play layers before the press | EXT/guard-main.js:159-296 | NEW |
| G5 | B | P1 | M | App-level pop-up wall uses tap evidence (approved) | BrowserActivity.kt:2091-2100; TabDelegates.kt:58 | NEW (needs G2) |
| G6 | B | P1 | S | Scripted jump to a listed ad host stopped at any delay (approved) | CORE/guard/JumpGuard.kt:27-32; BrowserActivity.kt:2072-2085 | NEW |
| S1 | B | P1 | S | Hold the glow pulse while a video plays (approved) | APP/ui/Neon.kt:48,108; BrowserActivity.kt:2026 | NEW |
| B2 | B | P1 | S-M | AdGuard Tracking Protection + Hagezi Light, default ON with a switch (approved) | FilterUpdater.kt:25-37; ADBLOCK_ENGINE.md s6 | KNOWN-UNQUEUED |
| BK3 | C | P1 | M | Raise the instant-hit rate where the lab says it can be raised | gecko-prefs.txt:22-23, 29-31 | OWNER-ASKED |
| BK4 | C | P1 | S | More pictures when RAM allows; picture of the scrolled view kept fresh | APP/ui/BackPictures.kt | OWNER-ASKED |
| M1 | C | P1 | S | bfcache size follows the live-tab budget (never below the Back needs) | gecko-prefs.txt:22-23; CORE/tabs/LiveBudget.kt | NEW |
| H1 | C | P1 | S-M | "Frugal" profile for the plain capped hotspot | CORE/net/NetProfile.kt; GA/GeckoEngine.kt:311-331 | NEW |
| S2 | C | P1 | S | Extension listener diet + counters | EXT/background.js:197-204, 672-686, 731-765 | NEW |
| S3 | C | P1 | S | Time budget for the token observer | EXT/content.js:36-68 | NEW |
| S4 | C | P2 | S | Blocker cost: average x requests, cold/warm split | EXT/engine.js:259-299, 346-382, 475-484 | EVIDENCE EY-30 |
| S6 | C | P2 | S | Lazy-load margin is a bandwidth spend: A/B | gecko-prefs.txt:28-31 | NEW |
| S5 | C | P2 | S | Answer "no cosmetic rules for this frame host" locally | background.js:548-567; research/PERF_JS.md | KNOWN-UNQUEUED |
| V4 | C | P2 | M | MSE (blob:) players matched to playlists | content.js:196-203; GA/web/VideoFlow.kt | NEW |
| V6 | C | P2 | S | Soft ad/decor words | MediaRank.kt:27, 88-100 | NEW |
| V7 | C | P2 | S | False-positive corpus and metric | tools/sitelab/vidlab_srv.py | EVIDENCE EY-21 |
| D2 | C | P2 | S | Window size / overlap by link speed | GA/net/GeckoFetcher.kt:23-24; Engine.kt:527-561 | EVIDENCE EY-26/27 |
| D3 | C | P3 | S | HLS start-up | StreamEngine.kt:565-577, 686, 730 | NEW |
| B3 | C | P2 | L | Signed list bundle (SECURITY B20) | research/SECURITY.md | KNOWN |
| Y4 | C | P2 | S | Refresh the YouTube list data when YouTube opens | FilterUpdater.kt | NEW |
| Y5 | C | P2 | S | Settings text and Diagnostics line for YouTube | SettingsView.kt | NEW |
| M2 | C | - | - | AS1 (stored libraries) and AS2 (per-process PSS) stay the top RAM/storage rows | TASKS AS1, AS2 | QUEUED |
| BK5 | C | P2 | M | `no-store` relaxation for owner-picked sites | EXT/background.js | OWNER QUESTION |

---

## 3. Part BK - Back: same scroll place, instantly (owner's request)

Owner's words: "when I press back to go to the last web page, it automatically goes back to the last scroll position I was at on that web page, and it should load immediately because it should be cached".

### 3.1 How Back works today [R]
- Back = `GA/BrowserActivity.goBack` (line 991): `armBackPicture` (994-1012) then `session.goBack()`. The engine's back/forward cache (bfcache; `browser.sessionhistory.max_total_viewers = 6`, `contentViewerTimeout = 3600`, gecko-prefs.txt:22-23) restores a kept page instantly with its exact scroll place (SPEED3.md 6.2).
- When the engine has to LOAD the page again, `backPictureShow` (979-989) puts a 480 px picture of the page as it was on screen when left (`BackPictures`, at most 4, RAM only, filled by `leavingPage` 1015 -> `captureThumb(forBack=true)` 1309) over the page 150 ms after Back (`showRestoring` 2170). It is removed by `firstPaint` (2269-2272: `hideRestoring("first-paint")`), i.e. as soon as the engine paints ANYTHING of the page.
- The scroll place of a reloaded page is whatever the engine restores from its own history entry. The app does not check it: `Tab.scrollY` (APP/web/Tab.kt:61) is one number for the current page and `pageStarted` (817) sets it to 0 at every page start. There is no per-entry memory and no check after Back.
- The cold-start "last look" already has the missing rule: `LastLookRules.hideNow(paintSeen, savedScrollY, currentScrollY, stopped, msSincePaint)` (CORE/session/LastLook.kt:97) lets the picture go only AFTER the first paint, and then only when the live page is where the picture was (the picture's place is 0, or the live place is within 4 px), or the load has ended (`stopped`), or 1.5 s have passed since the paint; the callers add a 20 s cap. The Back picture does not use it: it goes at the first paint.
- A hibernated or revived tab restores history AND scroll from the engine's `SessionState` [V, SPEED3.md K2]: not the problem.

### 3.2 What the engine can and cannot keep [R, research/SPEED3.md 2.1]
GeckoView 160's bfcache blockers: unload/beforeunload listeners (allowed here: `allow_unload_listeners` is true), **pending requests (REQUEST) and a page that is still loading (PAGE_LOADING)**, camera/mic/WebRTC/lock/WebTransport use, **MSE/EME content (the video players of movie sites)**, VR; and **`Cache-Control: no-store`: no preference lets such a page in** (a 2026 web search agrees: Firefox still refuses `no-store` pages, Mozilla bug 338542). A memory-pressure level 15 or more makes the engine empty the cache of every tab (research/SCROLL.md).
Likely reasons Back reloads on this tablet [E, not measured]: on a 5 Mbit/s hotspot the page left is often still loading or has pending requests (lazy-load margin 1500 px, trackers, long-lived requests), sites that send `no-store` (mail, banking, many dynamic pages), video-player pages, memory flushes. Honest limit for the owner: for `no-store` pages the browser cannot keep the page; the best it can do is show the exact saved view at once and put the loaded page back at the same place (BK2); BK5 is the optional way around.

### BK1 - Back lab: measure before tuning (P0, M)
- **Log per Back and Forward press** (Health line `back` + one Diagnostics line `Back: N presses, X % kept (instant), Y % reloaded from the cache, Z % from the network, median ms to first paint, scroll kept in W %`): kind (kept / reload), ms from the press to the target address showing and to first paint, the left page's `tab.loading`/`progress` at leave time (known in `leavingPage`), scroll before leaving and at +0.5 s / +2 s after, and what the target answered.
- **Signals, all cheap:** `content.js` (top frame) on `pageshow` sends `{k:'bf', p, ps: persisted?1:0, nt: navigation type from performance.getEntriesByType('navigation')[0].type ('back_forward'), ts: transferSize (0 = served from cache)}` once; the extension notes `cache-control` of the main-frame response in the handler it already has (background.js:731-765, `main_frame` is in its types) and sends a message only when it contains `no-store`; in-flight request count per tab = (existing `onBeforeRequest` count) minus (existing `onCompleted` / `onErrorOccurred`), reported when the next main-frame request starts. No new listener.
- **Proof:** unit test of the classifier; emulator lab pages (section 10): `/back-static`, `/back-nostore`, `/back-dynamic`, `/back-loading`, `/back-mse`; the owner's one Diagnostics copy after a day of browsing gives the real mix.
- Research note: `research/BACK_SCROLL.md` (table of cases and numbers).

### BK2 - the scroll place is the app's business (P0, M)
1. **Per-entry scroll memory.** `Tab.scrollByUrl`: a small LRU (32) of `urlKey -> y` (urlKey = `BackPictures.keyOf`, the address without `#fragment`), written from `lookScrolled` (2229, the page's own scroll reports: content.js:208-224, debounced 300 ms and once after load) and again at `leavingPage` (the value the owner left the page at). RAM only; private tabs too (never written to disk).
2. **Arm with the target's place.** `armBackPicture` already finds the target address (`st[i].uri`); look up `scrollByUrl[keyOf(url)]` and keep it in `BackArm` and in `tab.backTargetY` (with the URL and a 10 s expiry).
3. **The cover stays until the page is painted AND back at its place.** Reuse `LastLookRules.hideNow`: `backPainted()` in `firstPaint` (2269) records the paint time; `lookScrolled` and a timer call `backCheck()`; hide when `hideNow(painted, savedY, tab.scrollY, false, msSincePaint)` (pass `stopped = false` exactly as `lookCheck` does, so that the end of the load alone cannot hide the cover before the place is reached; the 1.5 s bound after the paint is the exit), and never later than 20 s in all (`showRestoring` already has the 20 s timeout). Result: no flash of the top of the page, and no cover that hides while the page sits at y = 0.
4. **If the engine did not restore the place, the page is asked to.** `content.js`, top frame: when `performance.getEntriesByType('navigation')[0].type === 'back_forward'` and the page was not restored from the cache (`pageshow.persisted` false), on `load` ask `runtime.sendMessage({k:'bk', p: location.href})`; `ShieldsBridge` answers (new case beside `"scroll"` at line 96, worker thread, plain string) with `tab.backTargetY` when the URL matches, else "". The script then scrolls: `window.scrollTo(0, y)` at 0, 250, 700, 1500 and 3000 ms, each time only if `|scrollY - y| > 4` and the document is tall enough (or it is the last try), and stops at once when the owner touches the page (`touchstart`, `wheel`, `keydown` since load) or when the place is right. A page that sets `history.scrollRestoration = 'manual'` is handled the same way (the place is checked, not assumed). Infinite feeds get the five tries while they grow.
5. **0 ms picture for targets known not to be kept.** At leave time the app knows `tab.loading` and (BK1) `no-store`; when the left page was still loading, had pending requests, sent `no-store` or held a video player, arm the picture at once instead of after 150 ms (the cache cannot have it).
- **Proof:** JVM `BackScrollTest` for the pure decision (hide/stay table; LRU; expiry); WT/content.test.js for the pull and the retries (a document that grows after 600 ms; a user touch stops it); emulator lab pages: reload case lands within 4 px of the saved place, `cover-hide ... y=a/b` Health lines show it, no screenshot at +100/+300/+600 ms shows the top of the page; kept case unchanged (`pageshow.persisted` true, exact place).
- **Do not:** restore when the owner has touched the page; touch pages in other frames; keep the picture longer than the caps.

### BK3 - raise the instant-hit rate where the lab says it can be raised (P1, M)
Decide from BK1 numbers, one lever at a time, keep a lever only if the hit rate rises and nothing looks broken:
- bfcache size: never below what Back needs; with M1 the cap may shrink under memory pressure but keeps at least 4 while the tab in front has history (viewers are shared by all tabs).
- Fewer pending requests when the page is left: S6 (lazy-load margin) and the extra lists (B2) both shorten the "still loading" window on the hotspot.
- Do not wipe the cache on a quick app switch: keep `onTrimMemory` handling as it is (BrowserActivity.kt:604, `LiveBudget.afterTrim`), only log when a trim of 15 or more empties the cache (BK1 counts it).
- Experiment, default off, only if BK1 shows `PAGE_LOADING`/`REQUEST` as the main cause: when a link is tapped at 90 % progress or more, stop the remaining loads of the page being left so that the engine may keep it. Risk: images that were still loading stay missing after Back. Keep only if the lab shows more kept pages and no broken page.
- `unload` listeners need no action (allowed); MSE/EME and `no-store` cannot be changed (BK5 for the second).

### BK4 - pictures (P1, S)
`BackPictures.MAX` (APP/ui/BackPictures.kt) is 4 at about 750 KB each. With `MemAvailable` over 700 MB keep 8 (6 MB), back to 4 under pressure; take the leave picture even when the page is still loading (already so); also retake the picture for the page in front 700 ms after a scroll settles when the picture is older than 30 s, so a long read followed by a quick tap is represented by the latest view. Pictures stay RAM only and are dropped with the tab, at trim 15+, and on clear-data.

### BK5 - `no-store` pages on sites the owner picks (P2, M, owner question open)
Many dynamic pages send `Cache-Control: no-store`, which keeps them out of the engine's cache and out of the disk cache. The only way to make Back instant for them is to change that header for chosen sites: a blocking `onHeadersReceived` for `main_frame` that rewrites `no-store` to `no-cache` (still revalidated, but bfcache-eligible and storable). Costs: a copy of the page stays on the tablet (unencrypted storage) and Back after logging out can show the page as it was. So: off by default, per-site list "Make Back instant on this site" chosen by the owner, never for a page with a password field, never in a private tab, wiped by clear-data. Ask the owner once; do not build before the answer.

---

## 4. Part Y - YouTube ad blocking and skipping (owner-approved 2026-11-14)

### 4.1 State today [R]
- The "YouTube pack" is the uBO lists' own YouTube scriptlet rules, run by our scriptlet library when Settings "Fix ad and anti-adblock scripts" is on (default ON, APP/data/Prefs.kt:32); Shields off for a site undoes it; the `quick-fixes` list is refreshed daily (FilterUpdater.kt:35). Decision text: research/ADBLOCK_ENGINE.md 14.3. It was never run against real YouTube and never on the tablet.
- This tablet asks for the MOBILE site by default (`USER_AGENT_MODE_MOBILE` unless the tab is "desktop": BrowserActivity.kt:897-898), so YouTube serves `m.youtube.com`; a tab in desktop mode gets `www.youtube.com`. Both matter.
- **Numbers from the real lists** (uAssets `a8f394f`, 2026-11-14; recipe in Appendix A): the lists the app downloads hold 8,720 scriptlet lines; our compiler rejects 337 of them (45 names). For `www.youtube.com` 34 rules apply and 25 are accepted; for `m.youtube.com` 11 apply and 10 are accepted. The rejected YouTube rules:
  - 6 x `trusted-edit-inbound-object` (quick-fixes.txt lines 26-31): they rewrite the objects the page passes to `JSON.stringify` when it builds player requests (for example `playbackContext.contentPlaybackContext+={"isInlinePlaybackNoAd":true}`, `clientScreen: CHANNEL`), selected by markers in the client's `userAgent` string ("channel", "yahi", "inline", "oaeb").
  - 2 x `trusted-prevent-dom-bypass` (lines 86-87, `Node.prototype.appendChild, fetch|Request`): stop the page from getting an untouched `fetch`/`Request` through a fresh frame, which would bypass every other fix.
  - 1 x `rpnt` (line 25) of 3,490 characters: over our caps (`MAX_ARG` 500, `MAX_TOTAL` 1500, ScriptletRules.kt:26-27). It injects a script into the page's own script that tags `ytcfg ... client.userAgent` with the markers above and, when the player stalls with an empty buffer and a `0x0` picture (YouTube's fake-buffering enforcement), rotates the marker and reloads with `#reloadxhr`. [E, from reading the rule text]
  - 1 x `json-edit-fetch-response` on `m.youtube.com` (filters.txt line 49): removes `adSlotRenderer` entries from the mobile feed answer.
  What IS accepted and works as designed: `json-prune`, `json-prune-fetch/xhr-response`, `trusted-replace-xhr/fetch-response` (`"adPlacements"` -> `"no_ads"`), `set` of `ytInitialPlayerResponse.adPlacements|playerAds|adSlots`, `nano-stb`, `rmnt`, `aeld`, `acs`. So today we strip ads from the data the player receives but we do not run the lists' answer to YouTube's enforcement.
- Per-host caps: `MAX_CALLS` 40 and `MAX_PAYLOAD` 16,000 (ScriptletRules.kt:29-30). `www.youtube.com` would use 34 calls and 7,060 characters once everything is accepted: under the caps today, but one list update from the call cap.
- Web check (2026, thin sources, treat as leads): YouTube's enforcement continues (fake buffering, warnings after a few videos when signed in); uBlock Origin on Firefox is reported as the most resilient; recent uBO release notes list a new `json-edit` family and an improved `trusted-prevent-dom-bypass`, which matches the rules above.

### Y1 - match the lists' current YouTube answer (P1, M-L; needs B1)
1. **Caps for trusted lists only** (after B1): `MAX_ARG` 4096, `MAX_TOTAL` 8192, `MAX_CALLS` 64, `MAX_PAYLOAD` 32,000 for `ubo-*` sources; the old caps stay for every other list. A long `rpnt` is page-code injection from a list, so it is only acceptable from the lists whose maintainers also write the scriptlet semantics.
2. **One JSON edit engine** in `EXT/scriptlets-main.js` (ES5, bounded work, no `eval`, no `new Function`): a path language with `..name`, `.name`, `.*`, `[-]`, `[?.prop]`, `[?.prop*="text"]`, `[?.prop=="v"]`, `[?.prop=/re/]`, and actions delete (default), `+=` (merge an object), `=` (set), `=repl({"regex":...,"replacement":...})`. Write it from the public uBO documentation of the scriptlets, not from uBO code (the repo's clean-room rule). Scriptlets on top of it: `json-edit`, `json-edit-fetch-response`, `json-edit-xhr-response`, `trusted-json-edit`, `trusted-json-edit-fetch-response`, `trusted-json-edit-xhr-response` (about 43 list lines in all) and `trusted-edit-inbound-object` (hooks a function such as `JSON.stringify`, edits argument N with the same language before it runs; 6 YouTube lines).
3. **`trusted-prevent-dom-bypass`** (30 list lines, 2 YouTube): wrap `Node.prototype.appendChild` (and the siblings the rule names) so that an inserted frame or script cannot hand the page an untouched copy of `fetch`/`Request`; log and count every prevented bypass.
4. **Account for what is still unsupported:** extend the existing `BuildReport` skip counters so Diagnostics prints "scriptlet lines accepted X of Y; YouTube www a of b, m c of d; unsupported names: ..." (the next biggest names today: `trusted-replace-argument` 42, `xml-prune` 18, `trusted-rpnt` 16, `trusted-create-html` 16, `trusted-set` 15, `trusted-prevent-xhr` 15, `trusted-replace-outbound-text` 13, `refresh-defuser` 12, `trusted-set-attr` 10, `trusted-prevent-fetch` 8, `trusted-suppress-native-method` 8). Implement more of them in descending order after YouTube (AW2 in TASKS is the same row; its old counts are out of date).
- **Proof:** `ScriptletRulesTest`: copy the real YouTube lines (data, from the list repo) into a test resource and assert each is accepted; web tests per scriptlet with synthetic player payloads (a player response with `adPlacements`, a feed with `adSlotRenderer`, a `JSON.stringify` call with the marked user agent, an `appendChild` of an iframe); emulator lab (Y3).
- **Do not:** accept any `trusted-*` or long rule from a list that is not trusted; copy uBO code; run the injected script anywhere but the named hosts (`youtube.com`, `youtube-nocookie.com`, `youtubekids.com`, `music.youtube.com`, `tv.youtube.com`).

### Y2 - a built-in skipper that does not depend on list data (P1, M)
The lists change every few hours when YouTube changes; our daily refresh and our partial scriptlet coverage will lag. A client-side skipper works in both site modes and gives a safety net. Put it in its own content script `EXT/youtube.js`, registered in `manifest.json` `content_scripts` for `*://*.youtube.com/*`, `*://*.youtube-nocookie.com/*`, `*://youtubekids.com/*`, `all_frames`, `document_end` (static, so it also runs on the first page after a cold start, and costs nothing on other sites).
- **Trigger:** no polling while no ad. Find the player once (`#movie_player` or `.html5-video-player`; retry at +1 s, +3 s, +8 s and on the site's navigation events), then observe ONLY that element's `class` attribute. When the class list has `ad-showing` or `ad-interrupting` [N: verify on the mobile site], start a bounded 250 ms loop (at most 15 s) that stops when the class goes.
- **Actions while an ad shows:** remember the video's `muted`, `volume`, `playbackRate` once; `muted = true`; `playbackRate = 16` (inside try; the engine may refuse values [N]); if `duration` is finite and under 180 s set `currentTime = duration - 0.1`; click the first visible skip button (`.ytp-skip-ad-button`, `.ytp-ad-skip-button`, `.ytp-ad-skip-button-modern`, `.ytp-ad-skip-button-container button`); close an overlay ad (`.ytp-ad-overlay-close-button`). When the ad class goes: restore the remembered values unless the owner changed them meanwhile.
- **Enforcement dialogs:** when the page shows its "ad blockers are not allowed" dialog (`ytd-enforcement-message-view-model` or a paper dialog containing that text; selectors [N]), remove it and its backdrop, unlock page scrolling and call `play()` on the main video.
- **Failure handling:** five ads in a row that still show after 15 s switch the skipper off for that page (Diagnostics only); it never touches playback when no ad class is present.
- **Counters (local only, no network):** ads seen, fast-forwarded, skip buttons clicked, overlays closed, dialogs removed, last success time, last failure reason; shown in Diagnostics "YouTube ads" and in Health lines (host only).
- **Setting:** one new row "Skip ads inside video players (YouTube and others)", default ON, text: what it does, "YouTube's terms forbid it; YouTube may show warnings or limit playback, more likely when you are signed in". Shields off for a site, or the row off, disables it. The existing "Fix ad and anti-adblock scripts" row keeps controlling the list rules.
- **Proof:** web tests with a mock player (class toggled around a `<video>` that plays a 12 s file, a skip button that appears after 5 s, an overlay, a dialog): the ad ends within 2 s, values restored, nothing happens without the class; emulator lab (Y3); the owner's Diagnostics line after real use.
- Related generic row **G7**: the same idea for other video players (JW Player, Video.js, IMA-based players). Do not run an observer on every page: start observing a player container's `class` only after a `<video>` in it has fired `play` (capture listener in content.js), look for the ad-state words `jw-flag-ads`, `vjs-ad-playing`, `vjs-ad-loading`, `ima-ad-container`, `plyr--ad`, and apply the same actions; never on the YouTube hosts (Y2 owns them).

### Y3 - a lab, because real YouTube cannot be run in CI (P1, S-M)
- `tools/sitelab/ytlab.py` (extend the adlab pattern): pages `/ytplayer-ad` (mock `#movie_player.html5-video-player` with a `<video>`, ad state toggling, skip button), `/ytenforce` (the dialog), `/ytjson` (a `fetch` of a `/youtubei/v1/player` stand-in whose answer holds `adPlacements`, `adSlots`; the lab rule file holds the REAL list lines for YouTube as fixtures), `/ytedit` (a `JSON.stringify` call with the marked user agent and an `appendChild` of an iframe) with expected before/after, run on the emulator against `control` pages that must stay untouched.
- Optional live check, network permitting (the sandbox proxy may refuse it): a Playwright script `tools/sitelab/yt_live.js` that opens a public video page with the mobile and the desktop user agent in headless Chromium, injects `scriptlets-main.js` and the YouTube rule lines as the extension would, and records: `ytInitialPlayerResponse.adPlacements` present before and absent after, whether an ad state appears within 20 s and whether the skipper ends it, any enforcement dialog. Chromium is not Gecko: it checks rule logic and selectors only. If YouTube cannot be reached say so in `research/YOUTUBE_ADS.md` and rely on the mock and the owner's report.
- Results and the selector table (verified or not) go to `research/YOUTUBE_ADS.md`.

### Y4 - fresher YouTube data (P2, S)
The `quick-fixes` list declares an 8-hour life. The app refreshes lists once a day (FilterUpdater, conditional GET: a 304 costs a few hundred bytes). Add: when the first YouTube page of a day starts, or the data is older than 8 hours, do the conditional GET of `quick-fixes` (and `filters` only if its ETag moved) and compile off the UI thread; swap atomically without a restart; show "YouTube rules from <date>" in Diagnostics. Needs the hotspot rule kept: no other background network.

### Y5 - words (P2, S)
The Settings row text of Y2, one plain sentence in the answer to the owner (it may still show warnings or play less smoothly, signing in raises the chance, one switch turns it off), the Diagnostics line, and an honest line in `research/YOUTUBE_ADS.md` that this is an arms race and the owner's report decides what is next. Do not promise.

---

## 5. Part G - the first press plays (ask 6) and the guard's own hardening (ask 5)

State [R]: three layers (EXT/guard-main.js in the page world; `JumpGuard` + `judgeJump` before a same-tab jump; `refusePopup` for new windows). AP1 added the assist (guard-main.js:159-308): after a refusal that follows a real press on a player it calls `play()` on the real video or presses through the layer, up to 5 rounds; 33 web tests cover it (WT/guard.test.js). The design is sound; the rows close holes around it. The owner approved a stricter wall (G5, G6) on 2026-11-14.

### G1 - the guard must be there for the very first page (P0)
- **Evidence [R].** `manifest.json` declares only `content.js` statically. `guard-main.js` and `scriptlets-main.js` are registered AT RUN TIME by `browser.contentScripts.register` from the background page (background.js:285-289 and 309-313; `guardState = 'page guard on'` only after the promise resolves). A document that starts before that has no `window.open` guard and no assist. AP1's own note: "the very first page after a cold start can load before the extension is active (seen on the emulator: that page opened a pop-up tab and had no guard)". Opening the app from a link, or restoring the last page, is exactly that case.
- **Change.** Try in this order, stop at the first that works on GeckoView 160: (A) declare both scripts in `manifest.json` `content_scripts` with `"world": "MAIN"`, `run_at document_start`, `all_frames`, `match_about_blank`, `match_origin_as_fallback` [N: Firefox 128+ documents `world` for manifest content scripts; check that this MV2 manifest accepts it]; (B) `browser.scripting.registerContentScripts` with `persistAcrossSessions: true` (needs the `scripting` permission) [N]; (C) larger and cleaner: the static `content.js` installs the `window.open` / `form.submit` replacements itself with `exportFunction` on `window.wrappedJSObject` and runs the guard logic in the isolated world (this also removes G2 and G3; WT tests need a shim). Keep `contentScripts.register` only for the per-host scriptlet calls.
- **Proof.** Emulator lab `/fakeplay` opened as the FIRST page after force-stop + a link launch: Diagnostics "page guard on" before the first document, the pop-up refused and counted. Add a check to `tools/test_handoff.sh` that fails if `manifest.json` stops listing the guard script.
- **Do not.** Delay a normal launch to wait for the extension.

### G2 - tap evidence must not be forgeable by the page (P0)
- **Evidence [R].** The jump guard trusts "the last real tap and the link it was on". That report is made in the PAGE world: guard-main.js:27-30 and 126-128 dispatch `new CustomEvent('__kst_pop_v1', {detail: 'tap|<href>'})`; content.js:234-236 forwards ANY such event as `{k:'pop', d: detail}`; background.js:275-283 passes `tap` and `vmenu`; `GA/BrowserActivity.onPopupBlocked` (2049, the `tap` branch at 2052) stores `tab.lastTapAt` / `lastTapHref`. A page script can dispatch the same event; `tap|<its ad url>` followed by `location = <ad url>` makes `judgeJump` answer ALLOW (`JumpGuard.decide`, last line).
- **Change.** content.js (isolated world) records taps itself: capture-phase `click` and `touchend`, `if (!e.isTrusted) return`, nearest `<a>` href, then `runtime.sendMessage({k:'pop', d:'tap|' + href})`. Whitelist what the page-world relay may forward (`open`, `click`, `overlay`, `form`, `redirect`); drop `tap` and `vmenu` arriving through the DOM event (`vmenu` is already sent directly, content.js:287-309; AY2's `os` message, 311-347, is already sent only from trusted events). Remove `note('tap', ...)` from guard-main.js (keep its local `onTap`).
- **Proof.** WT/content.test.js: a page script dispatching `__kst_pop_v1` with `tap|https://ad.test/` produces NO `tap` message; a trusted click produces exactly one with the right href. Emulator: a lab page that fakes the tap, then jumps: STOP. **Dependency:** G5.

### G3 - remove the cross-frame click channel (P0)
- **Evidence [R].** guard-main.js:299-308: every frame listens for `message` events from `window.parent` whose data starts with `__kst_assist_v1|` and calls `pressThrough(x, y)` (253-274), which dispatches `pointerdown, mousedown, pointerup, mouseup, click` on `document.elementFromPoint(x, y)` unless that element is a cross-site or `_blank` link. The parent is exactly the party the embedded page must not trust: any page can click buttons, same-site links and script-handled elements inside ANY cross-origin iframe it embeds, with no user action. (A nonce would not help: any secret in the MAIN world is readable by the page.)
- **Change.** Move the cross-frame hop out of the page world: the parent's content.js (isolated) decides the assist from ITS OWN trusted press record (G2) plus the guard's refusal notice, sends `{k:'assist', x, y, frameSrc}` to the background page, which finds the child in the frame table it keeps (`frameParent`, `mediaFrames`, background.js:625-666) and calls `browser.tabs.sendMessage(tabId, msg, {frameId})`; the child's content.js runs `startMedia` / `pressThrough` with DOM APIs. Same-frame assist can stay.
- **Proof.** WT: a parent page posts the old tag into an iframe holding a click counter: 0 clicks; the 13 AP1 cases still pass.

### G4 - neutralize fake play layers before the press (P1)
- **Evidence [R].** The assist is refusal-driven: `scheduleAssist` (228-231) runs only after `note()` recorded a refusal (27-30); the one proactive check, `isOverlay` (69-84), looks only at `<a>`. So the FIRST press always goes to the layer, the ad is refused, the assist presses through: "plays on the second try".
- **Change.** On `touchstart` (capture, passive) and `pointerdown`: when the finger lands inside a player rectangle (`playerAt`, 184-197) and the topmost element is a fake layer, set `pointer-events:none !important` on it at once; the click that follows ~100 ms later is hit-tested again and reaches the player. A fake layer = positioned `fixed|absolute`, NOT inside the player's own container (the player's own transparent controls and posters stay), not a `<video>`, under 24 characters of text, no `img/svg/button/input/video/canvas` child, and invisible (opacity under 0.05, or transparent with no visible child) or a cross-origin `<iframe>` laid over the player. After the first refusal in a document treat later presses the same way.
- **Proof.** WT: `position:fixed;opacity:0` layer (and a transparent-iframe variant) over a `<video>`: ONE real click plays it, `window.open` refused; negatives: the player's own play button, a cookie banner with text, a visible modal are untouched. Emulator `/fakeplay`.

### G5 - the app-level pop-up wall uses the tap evidence (P1, approved, needs G2)
- **Evidence [R].** `BrowserActivity.refusePopup` (2091-2100): a new window is refused only when there was no touch for 2.5 s or a `$popup` rule matches; any window that reaches the engine within 2.5 s of ANY touch (the play press) is allowed. It is the wall for what the page guard misses (G1; an iframe race: a script creates an iframe and calls `iframe.contentWindow.open` before the guard is injected, `match_about_blank` for content.js is false).
- **Change.** A pure `CORE/guard/PopupGuard.decide(sameSite, touchAgeMs, tapArrived, tapAgeMs, tapHref, uri, authHost, guardReady)` mirroring `allowOpen` (guard-main.js:87-97): ALLOW same-site; ALLOW when the tap report names exactly this address (a `target=_blank` link); ALLOW the sign-in/payment hosts of guard-main.js:25 (`AUTH`, a Kotlin copy with a test that keeps the two equal); otherwise STOP and show the blocked icon (AR1: one tap opens it). WAIT at most 150 ms for the tap report like `JumpGuard`. When the page never answered `guard`/`cfg` (guard not ready) cross-site windows stop.
- **Proof.** JVM `PopupGuardTest` (incl. OAuth hosts); emulator lab: iframe-race page. **Do not** ship before G2, or without the AUTH mirror (sign-in pop-ups would break).

### G6 - a scripted jump to a listed ad host, at any delay (P1, approved)
- **Evidence [R].** `JumpGuard.decide` returns ALLOW when the last touch is older than 2500 ms (JumpGuard.kt:29; the test at JumpGuardTest.kt:26 pins "a timer redirect long after the last touch" as ALLOW because SSO jumps have no tap). So "play, wait 5 s, the page jumps to an ad" passes.
- **Change.** Add `adHost` = the destination host is blocked by the lists as a third-party request (`fs.check("https://" + host + "/", pageHost, RequestType.SCRIPT)`: host rules only; not `RequestType.DOCUMENT`, which deliberately never applies to ad hosts, `dangerousDocument` BrowserActivity.kt:2360). Rule: `adHost && !(tapAge < 2500 && tapHref == uri)` -> STOP (blocked icon, one tap opens it). One hash lookup. SSO jumps go to identity providers, which are not on ad lists.
- **Proof.** `JumpGuardTest` +4 cases; lab `/slowjump` (`setTimeout(() => location = adHost, 5000)`).

### G7 - skip ads inside other video players (P1, approved): see Y2 (generic part).

---

## 6. Part V - the finder: fewer false and ad entries (ask 2)

How an entry reaches the list today [R]: the extension's non-blocking `onHeadersReceived` (background.js:731-765) reports responses whose type or extension says media (plain files >= 100 KB); "maybe" probes read the first bytes of disguised ones (703-729); content.js scans the DOM (243-285: `<video>/<audio>/<source>`, og:video, up to 600 links, inline JSON) and reports `dom` entries; `loadedmetadata` of any element sends `vid` (182-206). `ShieldsBridge.onMedia` (GA/web/ShieldsBridge.kt:128-143) re-judges with `MediaSniff.classify` and drops `dom` entries whose URL the ad lists block. `MediaList` keeps 48 ranked entries; `usable()` decides what counts for the dock badge; `MediaRank.rank` splits the sheet into "main" and "other".

### V1 - a metadata report must not make an ad or a preview "real" (P0)
- **Evidence [R].** `MediaList.applyInfo` (MediaList.kt:143-154) sets `touched = true` for every URL a page element reported. `usableUnsorted` (161-172) never hides a touched entry and EY-17's exemptions (`touched || duration >= 20 s || height >= 480`) never hide long or sharp ones. content.js `report()` (196-203) fires for EVERY `<video>`/`<audio>` that loads metadata: hover previews, muted looping hero videos, GIF-style loops, pre-roll ads (15-30 s, often 720p) all pass. `MediaRank.junk` ignores `touched`, so the sheet may demote them, but the badge count and the list cap (`value()`: +80, never evicted) already treat them as real.
- **Change.** (1) content.js sends facts with the report: `muted, loop, autoplay, controls`, rendered width/height, share inside the viewport, visibility, `adState` (ancestors up to 8 levels whose class/id match the ad-state words of Y2/G7 or `ad`/`ads`/`advert` as whole tokens). (2) Pure `CORE/media/VideoRole.decide(facts, durationMs)` -> `PLAYER | DECOR | AD`: DECOR = (loop && muted && !controls) or (muted && autoplay && !controls && duration <= 30 s) or area under 160x90; AD = adState. (3) `applyInfo` sets `touched` only for PLAYER; DECOR/AD set flags that override the exemptions and sort the entry into "Other files" (never deleted: a wrong guess costs one tap).
- **Proof.** JVM `VideoRoleTest` (table) + `MediaListTest` (a muted 25 s 720p loop stays out of the badge; a played 90 minute file stays in); WT/content.test.js; lab pages `/hover-previews`, `/hero-loop`, `/preroll-main`.

### V2 - verify before counting (P1)
- **Evidence [R].** `usableUnsorted` treats UNKNOWN length (`-1`) as usable (`c.length in 0 until MIN_FILE` is false for -1). Entries found in markup (links to `.mp4`, strings in inline scripts, og tags) have no length until the sheet opens (VideoFlow.show asks a one-byte Range request then). A page with an animated preview link or an ad config string already shows the badge.
- **Change.** `MediaCandidate.verified` = seen on the wire with a media type and (plain files) length >= 100 KB, OR a real `<video>`/`<source>` element, visible, area >= 160x90, not DECOR, OR reported by a player, OR touched by the owner. `usableCount()` counts verified entries only; unverified stay under "Other files". For up to 3 unverified entries on a page with NO verified one run the one-byte Range check at idle after the page settled (never while loading).
- **Proof.** JVM `MediaListTest`; emulator lab page with only `<a href=teaser.mp4>`.

### V3 - which element actually plays (P1)
- **Evidence [R].** Nothing listens for `playing`; `vid` is sent once per (src, height, duration).
- **Change.** content.js: capture `playing`; after 3 s of real playback send `{k:'vid', ev:'played', ...facts}`. App: the played entry with the largest visible area and the longest duration is `primary` and sorts first in `MediaRank.byRank`. Duration jump: if the SAME element later reports a source at least 3 times longer than an earlier one that played under 90 s, the earlier file is an ad.
- **Proof.** JVM + WT (ad -> film sequence); lab `/preroll-main`.

### V4 - blob: (MSE) players (P2)
- **Evidence [R].** content.js:197-198 drops every `src` not starting with `http`, so players that feed a `blob:` MediaSource (hls.js, shaka, dash.js: most movie-site embeds) report nothing about the stream they play. `MediaRank.byRank` (MediaRank.kt:74) puts every HLS/DASH candidate in `main` and `usableUnsorted` (164) always counts them: an ad playlist, a preview playlist and the film share the top.
- **Change.** For `blob:` elements send `{u:'blob', d, h, w}` with the facts of V1; `MediaList.applyStreamInfo(durationMs, height)` marks `touched` the playlist whose parsed duration is within 1.5 s (or 3 %) of the element's and whose variants include that height; a playlist under 60 s next to one of 5 minutes or more goes to "Other". Needs the V5 early look.
- **Proof.** JVM with `Hls`/`Dash` fixtures; lab: two playlists (ad 15 s + film 5 min).

### V5 - early look at playlists; ad runs skipped before they are fetched (P1)
- **Evidence [R].** (a) Live, DRM and refused streams cannot be saved, yet an HLS candidate always counts for the badge. (b) `Hls.parseMedia` (Hls.kt:133-176) has no handling for `#EXT-X-CUE-OUT`, `CUE-IN`, `#EXT-OATCLS-SCTE35`, `#EXT-X-DATERANGE`, `#EXT-X-ASSET`, `#EXT-X-SPLICEPOINT` (only the tags in its `when`, lines 133-168; a repo-wide search for CUE-OUT and SCTE finds nothing). (c) Ad runs are dropped today only after their first segment is downloaded and the MP4 writer sees a different codec setup (`skipRestOfRun`, StreamEngine.kt:1019-1022 fills `job.skipped`); same-encoder server-side ads pass untouched.
- **Change.** (1) `CORE/media/AdRuns.kt` (pure): split segments into runs at discontinuities; mark a run as ad when it sits between `CUE-OUT`/`CUE-IN` (or carries `SCTE35-OUT` or an interstitial DATERANGE), OR its segment URLs sit on another registrable domain (`Psl`) / another directory pattern than the runs holding at least 60 % of the playing time AND it is at most 120 s. Never mark the longest run; never mark when no run dominates. (2) `StreamEngine.prepare` pre-seeds `job.skipped` for ad-run segments (the existing mechanism); the row note says "Left out 1:30 of advertising (2 breaks)". (3) Early look: for the first 3 newly sniffed playlists of a page (after the page settled, not while loading) read the playlist (a few KB) and set `durationMs`, `endList`, `drm`, `adRuns`; live/DRM/under-20 s playlists stop counting for the badge.
- **Proof.** JVM `AdRunsTest` (marker run; foreign-host run; a legitimate multi-host CDN of one registrable domain stays; a two-part film without a dominant run stays); `StreamEngineTest` (the ad run is never requested); lab `/hls-ad-run`. At 3 Mbit/s a 30 s ad is about 11 MB of a 5 Mbit/s link not downloaded.

### V6 - soft words (P2)
`MediaRank.HINT_WORDS` (MediaRank.kt:27) lacks `vast, vpaid, imasdk, adunit, ad_type, adid, creative, bumper, intro, idle, loop, bg, hero, background, snippet, trailer`. Use them only as soft signals (they count when another signal agrees); "intro" and "trailer" can be wanted.

### V7 - a corpus and a number (P2)
Extend the EY-21 finder lab with the false-positive cases of V1-V5 and record precision (real videos shown / entries shown) per lab page in `research/FINDER_LAB.md`; a change that lowers recall on the true-positive pages is rejected.

---

## 7. Part D - the download manager (ask 3)

State [R]: `Engine` (single files) plans Range windows (`Planner`, 512 KiB..8 MiB), steals the tail of the slowest segment, grows connections by hill climb, retires an "extra" refused connection alone, checks tails when no validator exists, waits out an outage with ONE connection (30-minute give-up), writes with `pwrite`, checkpoints every 5 s. `StreamEngine` fetches parts with up to 3 workers and drains in order into an MP4 writer with a journal. Defaults on this low-RAM device: 2 downloads at once, 3 connections each (APP/download/DownloadController.kt:146). Good work; do not rewrite it.

### D1 - an HLS download dies on a short signal gap; a multi-connection file on a longer one (P1)
- **Evidence [R].** `StreamEngine.fetchLoop` (StreamEngine.kt:1069-1127): a part that fails 6 times in a row (`MAX_TRIES = 6`, line 1328; waits 1, 2, 4, 8, 16 s via `backoff`, 1166) throws `Failed(Reason.NETWORK)`; the worker's `catch (e: Failed)` (924) sets `job.failure` and stops the whole job. A DNS failure (the phone lost data: the hotspot's resolver answers at once) takes about 31 s for the six tries; connection timeouts (`FetchRequest` defaults 15 s connect, 20 s read, Model.kt:180-181) about 2 minutes. The only offline guard is `net.online` (lines 662, 705, 740, 1074), and the app deliberately does NOT use `NET_CAPABILITY_VALIDATED` (ARCHITECTURE.md section 7: a working hotspot lacks it), so `net.online` stays true exactly in the hotspot-blip case. `StreamEngine` has no `waitOutOutage`. Single files: `Engine.waitOutOutage` (Engine.kt:766-776) returns false when `job.alive > 1`, so a download with 2-3 connections that all fail 8 times (about 2 minutes) goes to FAILED, not WAITING_NETWORK; the test that proves the wait (EngineTest.kt:407) runs with `parallel = false`. The partial data is kept and Retry resumes, but an unattended download should simply wait.
- **Change.** (1) Outage = every worker of the job failed at the transport level (no HTTP status) and no byte arrived for 8 s while `net.online` is true -> WAITING_NETWORK with the existing ladder (`outageBaseMs` 10 s doubling to 60 s, give up after 30 minutes as STALLED, partial kept). (2) In `Engine.waitOutOutage` replace `job.alive > 1` by "no sibling moved bytes recently" (reuse `retireExtra`'s evidence); stop all workers. (3) Port the state to `StreamEngine`: on the sixth transport failure set `stopState = WAITING_NETWORK` and `resumeAt` with the ladder; the existing requeue path (`onNetwork`, StreamEngine.kt:468-472) plus a timer resumes; staged parts stay. (4) HTTP statuses (401/403/410/404/5xx) keep their handling; a fresh download to a dead host still fails fast.
- **Proof (failing test first).** `EngineTest`: two connections, `s.down = true` longer than the ladder, expect WAITING_NETWORK, then up and a byte-identical file. `StreamEngineTest`: same with a fake stream server (no outage test exists there today). Emulator: network off for 90 s mid-download.

### D2 - window size and overlap by link speed (P2, evidence for EY-26/27)
The Gecko fetcher asks 4 MiB windows (GA/net/GeckoFetcher.kt:23-24) because the reply queue has no back pressure; each window is a fresh request, and the idle time between windows is measured (`Engine.windowGaps`, 527-530). On the capped hotspot a 4 MiB window lasts 7 s and a 150 ms gap costs 2 %; behind Fast Wi-Fi (MB/s) the same gap is 15-20 %. Only if the Download lab (EY-27) shows it: under the Fast Wi-Fi profile raise the Gecko window to 8-16 MiB (check 3 x 16 MiB against the live-tab budget) or open the next Range request at 75 % of the window.

### D3 - HLS start-up (P3)
`prepare()` (StreamEngine.kt:686-715) fetches segment 0 serially (`decideMux`, 730-741), creates the sink and the init section, and only then does `supervise` spawn workers (565-577). Segments 1..n do not depend on the decision: spawn workers first, let the drain wait. Saves about one segment time. Do it after D1/V5.

---

## 8. Part B - blocking and security (ask 5)

### B1 - `trusted-*` scriptlets and long arguments only from trusted lists (P1; do before Y1)
- **Evidence [R].** `ScriptletRules` accepts `trusted-replace-fetch-response`, `trusted-replace-xhr-response`, `trusted-set-cookie`, `trusted-set-local-storage-item` (ScriptletRules.kt:50, 57-58) from ANY list line: `RuleParser.parseCosmetic` (RuleParser.kt:210-214) passes only the text inside `+js(...)`; `FilterSetBuilder.addAbpList(name, category, lines)` has no trust flag. The sources (FilterUpdater.kt:25-37) include EasyList, EasyPrivacy and a single-maintainer host (`secure.fanboy.co.nz`). The library rewrites response bodies by regex and sets cookies/storage (scriptlets-main.js:622, 640-665). uBO honors `trusted-*` only from its own lists; ADBLOCK_ENGINE.md line 78 already recommends it; never enforced. SECURITY.md T3 (poisoned list) predates scriptlets: a list that can rewrite what a bank's API answers is worse than one that hides a banner. The updater uses `HttpURLConnection` with the tablet's 2021 CA store.
- **Change.** `ListSource.trusted` (true for the `ubo-*` sources, false for EasyList, EasyPrivacy, fanboy), a `trusted` parameter on `addAbpList`, and `ScriptletRules.encode(inner, trusted)` rejecting names that start with `trusted-` (and `trusted-rpfr`) when untrusted; the same flag gates the larger caps of Y1. The snapshot script and the `:tools` CLI pass the flag by list file name.
- **Measure first.** Count stored `trusted-*` calls per list with `BuildReport`. If the cookie-notice rules that make Fanboy's list useful are `trusted-set-cookie`, gating removes consent-banner suppression: then allow those two names for that list with the value limits `setCookie` already has (scriptlets-main.js:621) or convert to `set-cookie`; say so in OUTBOX, do not silently lose a feature.
- **Proof.** `ScriptletRulesTest` (untrusted line skipped, trusted line stored); `DefaultListsRegressionTest` counts.

### B2 - two recommended lists were never applied (P1, approved: default ON with a Settings switch)
- **Evidence [R].** ADBLOCK_ENGINE.md section 6 (22 real pages): the bundled set blocks 78.5 % of requests and 36.9 % of bytes (99.3k host + 20.9k pattern rules, 4.2 MiB `.kfs`); "+ quick-fixes + abuse + AdGuard Tracking + Hagezi Light" = 180.1k + 30.1k rules, 6.0 MiB, 82.7 % / 37.7 %. Quick-fixes and removeparam were applied; `FilterSources.DEFAULT` (FilterUpdater.kt:25-37) has neither AdGuard Tracking Protection nor Hagezi; `docs/FILTER-LISTS.md` lists neither; TASKS has no box.
- **What it buys.** Mostly cookie-sync and tracker REQUESTS (+4.2 points), not bytes (+0.8): fewer DNS/TCP/TLS handshakes on a carrier-routed link, plus privacy. Cost: about +0.6 MiB (Hagezi Light) and +0.5 MiB (AdGuard Tracking) gzip per update; hosts cost 12 bytes each; engine image from 2.6 MB to about 3.5 MB [E].
- **Risk.** False positives on logged-in pages were not checked (ADBLOCK_ENGINE.md names it in its own "not verified" list). The owner accepted this on 2026-11-14.
- **Change.** Add both as separate sources (verify each URL returns the expected list, size and licence first [N]; both lists are GPL-3.0 per the research), with a Settings row "Extra tracker lists" default ON; keep the uBO `unbreak` exceptions winning (same set); extend `DefaultListsRegressionTest` with the owner's sites (doraby, pelisjuanita login, Proton Mail, Google, Wikipedia, YouTube, banks the owner uses: ask); show the extra-list block count in the Shields panel so a broken site is diagnosed by one tap (Shields off for the site already exists); record both in `docs/FILTER-LISTS.md`.
- **Pop-up networks.** Pick the pop-up list by evidence: collect the hosts of the "Blocked a pop-up (...)" icons from Diagnostics > guard events for two weeks, test them offline against EasyList, uBO annoyances, AdGuard Popups and Hagezi "Pop-Up Ads", add the list with the best hit rate.

### B3 - signed list bundle (P2, large)
SECURITY.md B20 (CI builds the compiled lists, signs them with an ECDSA P-256 key whose public half is in the APK) removes the dependence on the 2021 CA store and any single list host and makes B1 less critical. It was P2 when lists could only hide things; with scriptlets and the larger YouTube caps it deserves more. Needs the owner (key handling; SEC-1 already waits for them).

---

## 9. Part S / M / H - speed, RAM, hotspot (asks 1, 4, 7)

Measured on the tablet [M] (TABLET_DIAG_2026-11-13.md, VIDEO_SMOOTHNESS.md): hardware decoders for H.264, H.265 and VP9, 0 of 150 pictures dropped; 64-65 compositor frames a second, no scroll gap over 100 ms; heap 14/128 MB; 450-600 MB available with 4 of 6 tabs live; blocker image 2641 KB loaded in 2974 ms at start; page loads 1.5-5.2 s. Decode, draw and scroll are fine. What is left is work on the two busiest threads: the extension's background page runs in the PARENT process main thread (`extensionsProcessEnabled(false)`, GeckoEngine.kt:104); content scripts share the page process main thread (`dom.ipc.processCount = 1`).

### S1 - hold the glow while a video plays (P1, approved)
- **Evidence [R].** `Neon` ticks every 100 ms (Neon.kt:48) and is animated unless `busy || touching || away || covered` (108); `busy` is set only from `heavyLoad || keyboard` (BrowserActivity.kt:2026). Nothing in `GA` knows a video plays (grep "MediaSession": none); an inline video (the common case) keeps the pulse redrawing the window 10 times a second next to the decoder and the compositor. On the emulator the animation alone kept the parent process near 100 % of a core (VIDEO_SMOOTHNESS.md 2c); on the tablet nobody measured the parent process during playback [N].
- **Change.** `GeckoSession.setMediaSessionDelegate` per session (`MediaSession.Delegate.onActivated/onPlay/onPause/onStop/onDeactivated`; check with `javap` on the AAR as earlier rounds did) -> `tab.mediaPlaying`; `Neon.busy(...)` also while the front tab plays video. The same flag can hold the post-load thumbnail capture and the CPU meter's busy-thread look while a video plays.
- **Proof.** Unit test of the busy rule; emulator Diagnostics "CPU by process" `parent` with and without a playing video.

### S2 - extension listener diet (P1; measure with counters first)
- **Evidence [R].** Every request of every page runs through five listeners in the parent main thread: blocking `onBeforeRequest` (background.js:533-536, needed), non-blocking `onErrorOccurred` and `onCompleted` for ALL types (197-204; only to feed Diagnostics "Page report"), `onBeforeSendHeaders` for media/xhr/other/object with the request headers (672-686; only to remember Referer/Origin of a possible video), `onHeadersReceived` with response headers for media/xhr/other/object/frames (731-765). PERF_JS.md reviewed an older 335-line background.js; the last three were added later. On an API-heavy single-page app (Proton Mail, the owner's typing-lag report) that is hundreds of dispatches a minute on the thread that also serves the network.
- **Change.** (1) Counters first: per listener call count and total microseconds into the `st` stats (reportStats, background.js:148-152; Diagnostics "Blocker" line). (2) Restrict `onErrorOccurred`/`onCompleted` `types` to `main_frame, sub_frame, script, stylesheet, image, font`. (3) `onBeforeSendHeaders`: only for `media` and for XHR/other whose URL matches `MEDIA_EXT` (a URL filter is cheap), or derive Referer/Origin from `documentUrl`/`originUrl` and let the app's header ladder `MediaHeaders.sets` fall back as it already does. (4) EY-34 (the "page may be broken" chip) reads the same data: keep the listeners with the restricted types.
- **Proof.** The counters; WT/background.test.js updated; owner A/B on Proton Mail (Blocker line + typing test on `tools/sitelab/fieldlab.py`).

### S3 - the class/id token observer needs a budget (P1)
- **Evidence [R].** content.js:36-68 (D2): on every top document whose CSS starts with the token marker a `MutationObserver` watches `{childList, subtree}` of the whole document; each callback walks up to 200 records x 50 nodes and runs `querySelectorAll('[id],[class]')` (up to 300 nodes) on every added element. It stops only after 60 batches or 8000 tokens (`off`), so on a page that keeps adding nodes with already-seen class names it never stops and costs page-main-thread time for ever. The procedural observer next to it has a budget (`spent > 1500`, 40 runs, content.js:123-133); this one has none. PERF_JS.md found "no MutationObserver in the Gecko build" in older code.
- **Change.** The same discipline: add up the time spent in the callback and disconnect after 300 ms in total or 20 s after `DOMContentLoaded`, skip while `document.hidden`, ignore records whose target is a text/contenteditable region, coalesce with `requestIdleCallback`.
- **Proof.** WT/content.test.js: a page that churns 5000 nodes stays under the budget and sends every token found before the stop; A/B on Proton Mail with Shields off for the site.

### S4 - the blocker's own cost (P2; evidence for EY-30)
- **Evidence [M/R].** Tablet: 648 requests judged, average 723 us, slowest 11 ms (TABLET_DIAG s4, alpha27). Design target for the Kotlin engine: under 50 us on 100k rules (ARCHITECTURE.md s6); PERF_JS guessed 25-50 us on the A53; desktop V8 measured 5.2 us. 723 us x 150 requests = about 110 ms of parent-main-thread JS per page. The sample is small and includes cold JIT, so the steady state is unknown. EY-30 plans a histogram but acts only "if p95 > 2 ms": the average times the request count is the right trigger, and the histogram needs a cold/warm split.
- **Where the time can go [R, engine.js].** `makeCtx` lower-cases the whole URL (up to 2048 chars) and builds substrings (259-299); `scanPatterns` hashes every alphanumeric token incl. the query string (350-372); 4-gram probes at every URL position for token-less rules (373-380, capped at 3000 evaluations); the token-less list for every request (381); a host-blocked request still runs the whole pattern scan to look for exceptions (475-484).
- **Change (after the histogram).** Rank buckets by evaluations over a real URL corpus with the existing Node harness, then cut in order: uBO-style bad-token list at build time (ADBLOCK_ENGINE.md P1), skip URL tokens longer than the longest indexed token, skip the 4-gram scan for URLs that already matched a host rule and have no pattern exceptions for their tokens; `engine.test.js` stays at 100 % equal verdicts.

### S5 - cosmetic question per frame (P2)
PERF_JS.md found: every new iframe host costs one native `css` message (a UI-thread hop) although about 99 % of hosts have no site rules; its fix is one more KEX1 section with the 28,914 site-group hashes (231 KB) so the extension answers "nothing here" locally (background.js:548-567; ExtImage.kt:22 "only append"). No TASKS box. Do it after S2-S4 if the counters show many `css` messages per page.

### S6 - lazy-load margin and image look-ahead spend bandwidth (P2)
`dom.lazy-loading.margin.top/bottom = 1500` (default 600) and `layout.framevisibility.numscrollportheights = 2` (gecko-prefs.txt:29-31) were set for smooth scrolling on an "unmetered" link. At the hotspot's cap of about 625 KB/s the margin starts every image within 1500 px at LOAD time: one to two extra screens competing with scripts and above-the-fold pictures, and more pending requests when the page is left (which also keeps it out of the back cache, BK3). Decode look-ahead costs CPU, not bytes. A/B 600 / 1000 / 1500 on three real pages on the plain hotspot (Diagnostics > Page loads); keep 1500 only where it wins; see H1.

### M1 - the back/forward cache follows the live-tab budget (P1)
- **Evidence [R].** `max_total_viewers = 6` for up to an hour (gecko-prefs.txt:22-23) is fixed, while `LiveBudget` already shrinks live tabs with free memory, and `processCount = 1` means one Android kill takes every tab down (`sessionGone`, BrowserActivity.kt:2143). Six cached pages plus 4 live tabs on 450-600 MB available [M] is the configuration most likely to trigger that kill; a kill reloads every page, far more than the viewers save. The Back request (Part BK) needs the cache, so the rule is "never below what Back needs", not "small".
- **Change.** Set the pref live from the same rule: viewers = clamp(budget - 1, 4, 6) when `MemAvailable` is over 700 MB, 4 under 400 MB; re-evaluate on tab switch and on `onTrimMemory`. [N: confirm the pref is observed live in GeckoView 160 with the `debug.kestrel.pref1` rig and the Diagnostics read-back; if it only applies at start, set it at engine start and drop the live part.]
- **Proof.** Unit test of the mapping; emulator memory-pressure run (`gecko.kill` counter).

### M2 - what stays the top RAM and storage rows
AS1 (stored libraries: installed size 230 MB -> about 158 MB; +65 MB download; owner decision) and AS2 (per-process PSS line) are open and correct; re-base every RAM claim on the AS2 numbers once they exist. The 512 MB disk-cache cap on a tablet that had 1.6 GB free is EY-23's "adaptive cap".

### H1 - a frugal profile for the plain capped hotspot (P1)
- **Evidence [R].** "Unmetered" was decided on 2026-11-12 and several settings now spend bandwidth for smoothness: `network.prefetch-next = true`, `media.cache_readahead_limit = 60` s, lazy margin 1500 px (S6), `dom.prefetch_dns_for_anchor_https_document` (lab, default on, Prefs.kt:79). Unmetered is not unlimited: the cap is about 5 Mbit/s and the pipe is shared with the download yield (150 KB/s) and the Courier. Speculative bytes the owner never uses delay the bytes the owner asked for. On the plain hotspot DoH-first and HTTP/3 are ON (Prefs.kt:57-58).
- **Change.** A third state in `NetProfile` next to "Fast Wi-Fi tunnel": "capped hotspot" = no VPN AND measured throughput of the last page loads under about 1.5 MB/s (DataMeter / TrafficStats exist). In it: lazy margin 800, `network.prefetch-next` false, `media.cache_readahead_limit` 30 s; everything else unchanged. Outside it the current values stay.
- **Proof.** Page loads A/B on the real tablet (Diagnostics), 3 pages x 5 runs, median; keep a row only if it is not slower and not choppier. `NetProfileTest` for the state machine. **Do not** touch the Fast Wi-Fi values (unit-tested, waiting for the owner's three measurements).
- Signal gaps are D1. Settled, do not reopen: DoH/HTTP-3 A/B (EY-33), DNS cache lengths, `speculative-parallel-limit`, `max-connections` (SPEED4 "Considered and NOT done"), request pacing and TCP Fast Open (GECKO_TUNING_2 3.4-3.5).

---

## 10. Proof and measurement plan

| What | How | Where the number goes |
| :- | :- | :- |
| Back cases (BK1, BK2) | emulator lab pages `/back-static` (tall static page), `/back-nostore` (Cache-Control no-store), `/back-dynamic` (content added by script 600 ms after load), `/back-loading` (a request still open when the link is tapped), `/back-mse` (a page with a MediaSource); each: link -> Back; read `pageshow.persisted`, Health `back` / `cover-hide ... y=a/b`, screenshots at +100/+300/+600 ms | research/BACK_SCROLL.md |
| YouTube (Y1-Y3) | JVM tests with the real list lines as fixtures; WT per scriptlet; lab pages `/ytplayer-ad`, `/ytenforce`, `/ytjson`, `/ytedit`; optional live check | research/YOUTUBE_ADS.md |
| Guard on the first page (G1) | emulator: force-stop, launch with a link to `/fakeplay`; Diagnostics "page guard" line | research/GUARD_FIRST_PAGE.md |
| Forged tap / cross-frame click (G2, G3) | WT negative tests | WT/content.test.js, guard.test.js |
| Fake layer (G4) | WT + emulator `/fakeplay` | WT/guard.test.js |
| False-positive precision (V1-V3, V7) | lab pages `/hover-previews`, `/hero-loop`, `/preroll-main`, `/teaser-link`; count badge + rows | research/FINDER_LAB.md |
| HLS ad run (V5) | JVM fixtures + lab `/hls-ad-run` | CORE/media tests |
| Outage (D1) | JVM fake server that goes down longer than the ladder (1 connection, 2 connections, a stream); emulator network off 90 s | EngineTest, StreamEngineTest |
| Extension cost (S2-S4) | counters in the Blocker line; blocker histogram cold vs warm; Proton Mail typing lab with Shields on/off | research/EXT_COST.md |
| Glow during video (S1) | Diagnostics "CPU by process" `parent` with/without playback | research/VIDEO_SMOOTHNESS.md |
| Bandwidth prefs (S6, H1) | Diagnostics > Page loads, 3 pages x 5 runs, median and spread | research/NET_AB.md |
| Lists (B2) | DefaultListsRegressionTest + owner site list + popup-host replay | docs/FILTER-LISTS.md |

Rules for numbers: write `[E]` for guesses; record device, build, date; the owner's smallest step is always "Menu > Diagnostics > Copy and paste". Ask the owner for nothing new except: one Diagnostics copy after a day of use (Back lab), one while a video plays (S1), one after a Proton Mail session (S2/S3), the three Fast Wi-Fi measurements of SPEED4 section 5, and the one BK5 question.

---

## 11. Do not repeat - already in the tree or already queued (verified at `83803299`)

- Done: last-look snapshot and cold-start timeline (EY-1, EY-2), refused-part retry and header ladder for streams (EY-7, EY-8), MediaList by rank (EY-17), pop-up/redirect guard (X6), one-press assist after a refusal (AP1), blocked-page icon (AR1), scriptlets + stand-ins + early delivery + YouTube pack v1 (AW1), D1 in-extension engine, D2 token index, procedural cosmetics, embeds wait for a tap (EX-10), query stripping, Gecko ETP without the CONTENT category, per-site Shields-off including the engine's own protection, the Back picture (SPEED3 6.2), download UI, storage tidy, update flow, AY1 (no slow-page prompt), AY2/AY4 (long press on open space, floating panel; coded).
- Queued: EY-3..6, EY-9..16, EY-18..37 (TASKS lines 344-379), AS1, AS2, AW2 (superseded by Y1), AY3/AY5. This file adds evidence only to EY-21 (V7), EY-23 (M2), EY-26/27 (D2), EY-30 (S4), EY-34 (S2), and EY-6 (cache after restore) is the cold-start twin of BK.
- Considered and rejected earlier: declarativeNetRequest, bundling uBO, rebuilding the engine, Spectre/JIT/GC pref changes, `check_doc_frequency`, forcing delazification.
- Verified OK, leave alone: the download engine's planning, steal, hill climb and resume safety; the guard's JS logic and its 33 web tests; `JumpGuard`'s design; `LiveBudget`; `engine.js` as a faithful port with differential tests; scroll, decode and draw numbers on the tablet.

## 12. Limits of this report

- Nothing was run: no JVM test, no web test, no emulator, no tablet. Every `[R]` claim is a reading of code at `83803299`; the Gecko-specific ones marked `[N]` (G1 manifest `world`, M1 live pref, S1 MediaSession on this engine, the playbackRate limit and every YouTube selector in Y2) need the named check before work starts.
- The YouTube numbers are from one read-only copy of uAssets of 2026-11-14 and change every few hours; Appendix A regenerates them. YouTube was not reached from this review; whether the skipper and the rules work on the real site is unknown until Y3 or the owner's report. YouTube may warn, throttle or limit playback whatever the browser does.
- The Back hit rate in the owner's real use is unmeasured (BK1). The tablet numbers quoted are from alpha27; alpha28-39 changed the engine settings and the video path since.
- The WebView build (`:app`, Kestrel Light) was not reviewed beyond shared code.
- Open question for the owner: BK5 only.

---

## 13. The copy-paste TASKS block (append at the END of TASKS.md)

```
## Owner request 2026-11-14 (outside review round 3, read-only; answered later the same day) - research/REVIEW-EXTERNAL-3.md - seven asks + YouTube + Back
- [ ] RX3 "Without interfering with the other claude account working on this repo, do a read only review of the browser app so far. Look for: 1) ways to improve the coding for speed and efficiency of the app and web page loading and video playback 2) ways to improve video finding on web pages for the video downloader and reduce false videos and advertisements showing up in the downloads list 3) ways to improve or speed up the download manager 4) ways to decrease ram usage and make the app more lightweight without sacrificing any speed or function 5) ways to improve ad blocking, tracker blocking, and security without making websites break 6) ways to improve true video finding on websites so the video plays the first time I press play instead of redirecting or showing advertisements or pop ups 7) ways to improve the hotspot connection settings and speed and efficiency in the browser app" -> then the owner answered: "Yes to all four, and include YouTube ad skipping and blocking." (the four: extra tracker lists with a Settings switch, stricter pop-up/redirect wall, pause the glow while a video plays, skip ads inside video players by default) and: "add a request for the other Claude session to make it so when I press back to go to the last web page, it automatically goes back to the last scroll position I was at on that web page, and it should load immediately because it should be cached" - report: research/REVIEW-EXTERNAL-3.md (START HERE at its top); Wave A = BK1, BK2, B1, Y1, Y2, Y3, G7, G2, G3, G1; Wave B = V1, V2, V5, V3, D1, G4, G5, G6, S1, B2; Wave C = the rest, each gated by its measurement
  - [ ] RX3-BK1 Back lab: Diagnostics line + Health events per Back/Forward press (kept / reload / network, ms to paint, scroll before/after, why not kept); content.js pageshow + navigation type + transferSize, no-store from the main-frame headers, in-flight count; lab pages /back-static /back-nostore /back-dynamic /back-loading /back-mse; research/BACK_SCROLL.md
  - [ ] RX3-BK2 Back keeps the scroll place: Tab.scrollByUrl LRU; armBackPicture carries the target's y; the cover hides only when painted AND at its place (reuse LastLookRules.hideNow, 1.5 s / 20 s bounds); content.js asks the app for the place after a back_forward reload and scrolls (retries 0/250/700/1500/3000 ms, stops on touch); 0 ms picture for targets known not to be kept; JVM + web tests + emulator proof
  - [ ] RX3-BK3 raise the instant-hit rate where BK1 says it can be raised (bfcache size floor, fewer pending loads, optional stop-at-90% experiment default off)
  - [ ] RX3-BK4 BackPictures: 8 pictures when MemAvailable > 700 MB, retake the picture after a scroll settles
  - [ ] RX3-BK5 (OWNER QUESTION, do not build before the answer) Back instant for no-store pages on sites the owner picks (header rewrite no-store -> no-cache; off by default; never password pages or private tabs)
  - [ ] RX3-B1 ListSource.trusted + trusted-* scriptlets and long arguments only from trusted lists; count per list first; keep cookie-banner suppression
  - [ ] RX3-Y1 YouTube answer of the lists: raised caps for trusted lists (MAX_ARG 4096, MAX_TOTAL 8192, MAX_CALLS 64, MAX_PAYLOAD 32000), JSON edit engine + json-edit*, trusted-edit-inbound-object, trusted-prevent-dom-bypass; real YouTube list lines as test fixtures; Diagnostics accepted-of-total per YouTube host
  - [ ] RX3-Y2 built-in YouTube ad skipper and hider in its own content script (EXT/youtube.js, static manifest entry for the YouTube hosts), observer only on the player class, bounded loop, restore mute/rate, skip button, overlay, enforcement dialog, counters, ONE new Settings row "Skip ads inside video players (YouTube and others)" default ON
  - [ ] RX3-Y3 YouTube lab (tools/sitelab/ytlab.py mock pages with the real rule lines) + optional live Playwright check + research/YOUTUBE_ADS.md with a selector table (verified or not)
  - [ ] RX3-Y4 refresh quick-fixes when the first YouTube page of the day starts and the data is older than 8 h; Diagnostics "YouTube rules from <date>"
  - [ ] RX3-Y5 Settings text, Diagnostics "YouTube ads" line, plain answer to the owner (may warn or limit playback)
  - [ ] RX3-G7 skip ads inside other video players (generic: observe a player's class only after a video in it fired play; ad-state words; same actions as Y2)
  - [ ] RX3-G2 tap evidence recorded by content.js with isTrusted listeners; the DOM-event relay may forward only open/click/overlay/form/redirect; web tests for a forged tap
  - [ ] RX3-G3 remove the postMessage assist channel (cross-frame click injection); assist hop through content.js + background tabs.sendMessage; the 13 AP1 web tests still pass
  - [ ] RX3-G1 guard-main.js and scriptlets-main.js active on the very first page after a cold start (manifest world MAIN, or scripting API with persistAcrossSessions, or exportFunction from content.js); proof /fakeplay as first page after force-stop; handoff test that the manifest lists the guard
  - [ ] RX3-G4 neutralize invisible fake layers over a player before the press (outside the player's subtree); web tests incl. negatives
  - [ ] RX3-G5 core PopupGuard (tap evidence, AUTH hosts mirrored, WAIT 150 ms) used by BrowserActivity.refusePopup; strict while the guard is not ready; needs G2
  - [ ] RX3-G6 scripted jump to a listed ad host stopped after any delay unless the tap was on that very link; JumpGuardTest cases; /slowjump lab
  - [ ] RX3-V1 core VideoRole (PLAYER/DECOR/AD) from element facts; applyInfo touches only PLAYER; flags override the display exemptions; MediaListTest + lab pages
  - [ ] RX3-V2 verified flag: badge counts only wire-verified or visible-element or player-reported entries; at most 3 one-byte checks per page with no other evidence
  - [ ] RX3-V3 playing-element signal and duration-jump ad rule; primary video first in MediaRank
  - [ ] RX3-V4 blob:/MSE players matched to playlists by duration and height; playlists under 60 s next to a 5 min one go to Other
  - [ ] RX3-V5 early look at new playlists (live/DRM/short/CUE markers) + core AdRuns (markers, foreign registrable domain, dominant run) pre-seeding job.skipped; note "Left out m:ss of advertising"
  - [ ] RX3-V6 soft ad/decor words and VAST/IMA hints in MediaRank (soft signals only)
  - [ ] RX3-V7 false-positive corpus and precision metric in the finder lab (research/FINDER_LAB.md)
  - [ ] RX3-D1 outage wait for StreamEngine and for multi-connection Engine downloads (WAITING_NETWORK ladder, 30 min STALLED, partial kept); failing tests first
  - [ ] RX3-D2 window size / next-window overlap by measured link speed (after EY-27 numbers)
  - [ ] RX3-D3 HLS start-up: workers start while the mux decision is made
  - [ ] RX3-B2 AdGuard Tracking Protection + Hagezi Light as separate sources, Settings row "Extra tracker lists" default ON; regression test with the owner's sites; pick pop-up lists by replaying blocked-popup hosts; docs/FILTER-LISTS.md
  - [ ] RX3-B3 signed list bundle (SECURITY B20), priority raised
  - [ ] RX3-S1 hold the glow pulse while a video plays (GeckoSession MediaSession delegate); CPU line before/after
  - [ ] RX3-S2 extension listener counters, then restrict onErrorOccurred/onCompleted types and the onBeforeSendHeaders scope
  - [ ] RX3-S3 time budget and stop for the class/id token MutationObserver (content.js D2); web test; Proton Mail A/B
  - [ ] RX3-S4 blocker histogram with cold/warm split; trigger on average x requests; bucket report; then the cuts in section 9
  - [ ] RX3-S5 site-group hashes in the engine image so frames without rules never ask the app (PERF_JS "css")
  - [ ] RX3-S6 A/B lazy-load margin 600/1000/1500 on the plain hotspot
  - [ ] RX3-M1 back/forward cache viewers follow LiveBudget (floor 4; live pref if the engine observes it)
  - [ ] RX3-H1 NetProfile "capped hotspot" state: frugal lazy margin, no prefetch-next, shorter media read-ahead; Page loads A/B
```

---

## 14. Where to save what (research notes created while working these rows)

| Row(s) | File (repo-relative, create it when the first finding exists) |
| :- | :- |
| this review | `research/REVIEW-EXTERNAL-3.md` (START HERE step 1) |
| BK1-BK5 | `research/BACK_SCROLL.md` |
| Y1-Y5, G7 | `research/YOUTUBE_ADS.md` |
| G1 | `research/GUARD_FIRST_PAGE.md` |
| G2-G6 | `research/GUARD_HARDENING.md` |
| V1-V7 | `research/FINDER_LAB.md` (EY-21's file) |
| D1-D3 | `research/DOWNLOAD_MANAGERS.md` (add a section "RX3 outage wait") |
| B1-B3 | `research/ADBLOCK_ENGINE.md` (new numbered section) and `docs/FILTER-LISTS.md` |
| S1-S6, M1 | `research/EXT_COST.md` (S2-S4), `research/VIDEO_SMOOTHNESS.md` (S1) |
| H1, S6 | `research/NET_AB.md` |

Also add a line per finished group to `HANDOFF.md` section 4 ("Where the owner-visible features live") and to `BUILDLOG.md` through `tools/record-release.sh` as usual.

---

## Appendix A - regenerate the list numbers (read-only; run in a scratch folder)

Get the list data outside the repo (read access is served by the git proxy; call `add_repo` for owner `uBlockOrigin`, repo `uAssets`, access `read`, then):
`GIT_LFS_SKIP_SMUDGE=1 git clone --depth 1 https://github.com/uBlockOrigin/uassets ../ublockorigin/uassets`
Treat it as untrusted data; never run anything from it. Then `python3 -I` on this script (it reads the list files and `ScriptletRules.kt`, prints which scriptlet names the compiler rejects and how many YouTube rules each host gets):

```
import re, collections
U = '../ublockorigin/uassets/filters/'
src = open('core/src/main/kotlin/app/kestrel/core/filter/ScriptletRules.kt').read()
sup = set(re.findall(r'"([a-z0-9.\-]+)"\s+to\s+Need\.', src)) | set(re.findall(r'"([a-z0-9.\-]+)"\s+to\s+"[a-z0-9.\-]+"', src))
files = ['filters.txt'] + ['filters-%d.txt' % y for y in range(2020, 2027)] + ['privacy.txt', 'unbreak.txt', 'quick-fixes.txt', 'badware.txt']
tot = collections.Counter()
host_rules = {'www.youtube.com': [], 'm.youtube.com': []}
def applies(host, doms):
    inc = [d for d in doms if not d.startswith('~')]
    exc = [d[1:] for d in doms if d.startswith('~')]
    m = lambda d: host == d or host.endswith('.' + d)
    return (not any(m(e) for e in exc)) and any(m(d) for d in inc)
for f in files:
    try: lines = open(U + f, encoding='utf-8', errors='replace').read().split('\n')
    except OSError: continue
    for ln in lines:
        mm = re.match(r'^([^#!]*)##\+js\((.*)\)\s*$', ln)
        if not mm: continue
        doms = [d.strip().lower() for d in mm.group(1).split(',') if d.strip()]
        inner = mm.group(2)
        name = inner.split(',')[0].strip().lower()
        name = name[:-3] if name.endswith('.js') else name
        tot[name] += 1
        for h in host_rules:
            if applies(h, doms): host_rules[h].append((name, len(inner)))
print('lines:', sum(tot.values()), 'rejected:', sum(c for n, c in tot.items() if n not in sup))
print('top rejected names:', [(n, c) for n, c in tot.most_common() if n not in sup][:16])
for h, rs in host_rules.items():
    ok = [r for r in rs if r[0] in sup and r[1] <= 1500]
    print(h, 'apply', len(rs), 'accepted', len(ok), 'chars all/accepted', sum(r[1] for r in rs), sum(r[1] for r in ok))
```

Results at `a8f394f` (2026-11-14): lines 8,720; rejected 337 (45 names); top rejected: `trusted-replace-argument` 42, `trusted-prevent-dom-bypass` 30, `xml-prune` 18, `trusted-rpnt` 16, `trusted-create-html` 16, `trusted-set` 15, `trusted-prevent-xhr` 15, `trusted-json-edit-fetch-response` 14, `trusted-replace-outbound-text` 13, `refresh-defuser` 12, `trusted-json-edit` 12, `trusted-set-attr` 10, `json-edit` 9, `trusted-prevent-fetch` 8, `trusted-suppress-native-method` 8, `trusted-json-edit-xhr-response` 8. `www.youtube.com`: 34 apply, 25 accepted (payload 7,060 of 16,000 characters allowed); `m.youtube.com`: 11 apply, 10 accepted. Ten rules in the files exceed 1500 characters (one YouTube `rpnt` of 3,490, one `zefoy.com` `rpnt` of 2,008, `trusted-create-html` and `trusted-rpnt` rules of a game site and a comics site).

## Corrections

(none yet - add them here)
