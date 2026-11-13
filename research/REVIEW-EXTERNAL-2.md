# REVIEW-EXTERNAL-2 - second outside review of Kestrel G (repo-only round)

Written for the other Claude account. Save it as `research/REVIEW-EXTERNAL-2.md`, then work from section 12 (the TASKS block).

- Read: branch `ccr-a57c163e-mtq8js`, HEAD `a395248` (ckpt 1648), alpha31 shipped (versionCode 130). Your AL1-AL6 work is in progress; nothing here repeats it.
- Scope (owner, 2026-11-13): speed, RAM handling, page functionality for every kind of page, download manager, video finder, the "link expired" failure of video downloads (can it resume or reconnect by itself?), and showing the last page instantly after closing and reopening the browser.
- Method: ONLY the repo was read (code, `research/`, `TASKS.md`). No web, no engine download, no tablet. Round-1 facts are reused only where a repo file records them.
- Tags: **[R]** read in the repo (file:line, valid for a395248; re-check line numbers after pulling) | **[E]** estimate or reasoning, not measured | **[N]** not verified: the named test decides.
- Path aliases: `GA` = geckoapp/src/main/kotlin/app/kestrel/browser | `APP` = app/src/main/kotlin/app/kestrel/browser | `CORE` = core/src/main/kotlin/app/kestrel/core | `EXT` = geckoapp/src/main/assets/kestrel-shields

---

## 0. Instructions for the receiving Claude

1. Follow `CLAUDE.md` and `HANDOFF.md` exactly as for round 1. Put the owner's asks and every row you take on into `TASKS.md` as `[ ]` boxes BEFORE any code (copy-paste block: section 12; the id prefix `EY-` was grepped in `TASKS.md` at a395248: no collision).
2. One row at a time. Each row has a proof (section 9). Ship only what is proven; a row whose proof needs the tablet stays "coded, waiting for the owner" like your earlier rows, with the smallest possible owner step (one button, one Copy).
3. Every `[E]` number in this file is a guess. Replace it with a measurement from section 9 and write the measurement into `research/` before building on it. Every `[N]` is a hypothesis with a test: run the test first; if it fails, drop the row and say so in OUTBOX.
4. Suggested order: Wave 1 = EY-2, EY-1, EY-7, EY-8, EY-22 (step 1 only), EY-17. Wave 2 = EY-9, EY-10, EY-11, EY-12, EY-15, EY-16, EY-18, EY-23, EY-24, EY-37 (only after its lab page shows the controls are dead). Wave 3 = everything else. End each wave with `tools/ship.sh` and an OUTBOX paragraph. Each EY row is its own checkpoint commit (`tools/ckpt.sh`), so a cut-off session loses at most one row.
5. Do not repeat anything in section 10. Do not touch AL1-AL6 files while they are mid-flight (`MiniTracker`, `DownloadPopup`, `EpisodeNames`, `MediaNames`): add your changes after they land.
6. Safety rules that apply to every row: no cookie, token, Authorization or full signed URL in any `Health` line, log, Diagnostics text or copied report (log host and status only); private tabs never produce snapshots, stored headers or records; stored request headers are scrubbed at terminal states as `Rec.headers` already is (CORE/download/Model.kt:57 [R]); injected JS stays ES5; no coroutines; no native code; Kotlin framework Views only.
7. Text of this repo's reports must stay ASCII (hooks block other characters, absolute home paths, and some command words). Keep to that in everything you write back.
8. Reading budget. This file is long on purpose (about 20,000 words) so that each row can be built from its own subsection without guessing. Read sections 0, 1, 2, 9, 10 and 12 now. Read a part only when you start one of its rows, and then only that row's subsection: EY-1..6 are in section 3, EY-7..14 in section 4, EY-15..21 in section 5, EY-22..28 in section 6, EY-29..33 in section 7, EY-34..37 in section 8. A session that resumes mid-wave needs only `tools/resume.sh`, the ladder row it was on, and that row's subsection.
9. Every subsection says what to build, where (file and line, valid for a395248), how to prove it, and what NOT to do. If a line number no longer matches, search for the symbol written next to it; do not guess a new place.

---

## 1. The five asks in one screen

| Ask | What is wrong today [R] | The fix (rows) |
| :- | :- | :- |
| **Show the last page instantly on reopen** | A swipe-away kills the whole process (`ProcessLife.maybeExit`, GA/BrowserActivity.kt:342) so every reopen is a cold start: the engine is created after the first frame (205-214), the page is fetched again through `restoreState` (675-702). Until it paints, the owner sees a 240 px thumbnail stretched about 2.5x, dimmed to 30 % brightness, under a chip (APP/ui/RestoreOverlay.kt:58-80; `showRestoring` dim 0xB3, BrowserActivity.kt:1976). The thumbnail is captured only when idle for 15 s, never at pause (274-284), and lives in `cacheDir` (APP/ui/ThumbStore.kt:40), which Android may purge. | A sharp, full-size "last look" snapshot taken at pause and when scrolling settles, stored in `filesDir`, drawn 1:1 and undimmed in the very first frame, removed only when the live page has painted and its scroll position is back. Then shorten the cold path (timeline, first-frame diet, engine file prefetch, optional "stay ready"). **EY-1..EY-6** |
| **"Link expired" on video downloads: resume or reconnect by itself** | One refused part (401/403/410) fails the WHOLE stream job (CORE/media/StreamEngine.kt:1138-1142 -> worker 898 -> supervise 569). Resumes reuse the saved part list (prepare 638-641). Parts never try another header set. Single files get one re-ask of the clicked address (CORE/download/Engine.kt:585-590). No "refresh" action exists; Retry re-asks the same dead address. | A recovery ladder: retry (it is often not expiry), rotate headers, re-read the playlist and swap part addresses in place keeping every staged part, refresh before expiry and after a network change, and when the playlist itself is refused: a "needs fresh link" state that matches the next sniffed copy of the same video and continues by itself. **EY-7..EY-14** |
| **Video finder: find as many sources as possible** | The extension watches network requests only. Nothing in the page hooks `fetch`, XHR, `MediaSource`, `createObjectURL` or media element `src` (EXT/guard-main.js hooks only popups). JSON and HTML answers are dropped (EXT/background.js:540-551 and 585, `MEDIA_CT`/`maybeMedia`). The DOM scan misses shadow DOM, `data-*` attributes, external scripts and late-injected players, runs twice per page, and is not repeated after Back from the page cache. The list holds 24 entries and drops the OLDEST (CORE/media/MediaList.kt:101-102), which is usually the real video. Subtitles are parsed and never offered. | A MAIN-world media hook, DOM scan v2 plus a `performance` resource sweep, rank-based eviction, an empty-state "look for videos" helper, DASH multi-period and WebM, subtitles. **EY-15..EY-21** |
| **Download manager** | Files from servers that send no strong ETag and no Last-Modified get ONE connection, no Pause, no wait-out of an outage and a restart from zero after failure (Planner.kt:16-17, Model.kt:85, Engine.kt:132/139/183/271/757/887), although the tail-compare that makes resuming safe already exists (Engine.kt:557-560). No space check for streams. Checkpoint `fsync` runs on the control thread (Engine.kt:842). | Open the gate with the existing tail-compare, then parallel segments with a seam compare; space preflight and "make room"; abort sockets when the network changes; IO path; measurements. **EY-22..EY-28** |
| **Speed, RAM, pages that load and work** | Engine is lean already (see section 10). Open: cold start order, memory governor timing, request-path cost, DoH/H3 on the hotspot, pages that break under Total Cookie Protection / bounce-tracking protection / STRICT ETP (GA/GeckoEngine.kt:88-99). Downloads started by the engine close the first response and re-request it (GA/web/TabDelegates.kt:199-207): one-time links, POST and `blob:` downloads cannot work. | Memory governor, request-cost histogram, pref A/B batch, baseline profile, a "page may be broken" chip fed by the page report, a compatibility audit, a 20-page matrix, and answers for the date, colour and share prompts that nothing answers today. **EY-29..EY-37**, and EY-14 |

---

## 2. The ladder (value over risk, within each group in build order)

Columns: effect is the owner-visible gain; risk is the chance of breaking something that works; evidence is how sure the problem and the gain are. "Tablet" = the proof needs the owner's tablet.

| Id | Ask | What | Main files | Effect | Risk | Evidence | Tablet |
| :- | :- | :- | :- | :- | :- | :- | :- |
| EY-1 | L | Sharp last-look snapshot: capture at pause and when scrolling settles, store in `filesDir`, show 1:1 undimmed in the first frame, hide at first paint AND scroll restored | BrowserActivity.kt (onCreate, earlyCover, onPause, firstPaint), RestoreOverlay.kt, new `LastLook` store | The page the owner left is on screen at once, readable | low-med (privacy wipe paths, size mismatch) | cause [R], gain [E] | yes (timing) |
| EY-2 | L | Cold-start timeline: Health marks + one copyable Diagnostics line (first frame, snapshot drawn, runtime ready, session opened, first paint, page stop) | BrowserActivity, GeckoEngine, Diagnostics | Gate for EY-1, 3, 4, 5, 6 | none | [R] marks exist (207/212) | yes |
| EY-3 | L | First-frame diet: move `DownloadController.init`, `UpdateManager.init`, `BrowserDb`, non-essential `Shields.init` work after the first frame | BrowserActivity.kt:181-236 | Earlier first frame | low | [E] | yes |
| EY-4 | L | Engine file prefetch: read the engine library and archive into the page cache on a low-priority thread from `GeckoApp.onCreate` | GA/GeckoApp.kt | Shorter engine start | low | [E] | yes |
| EY-5 | L | "Stay ready after closing" (Off / 5 / 15 min): delay `ProcessLife` exit; kill on trim; never with clear-on-exit | ProcessLife.kt, GeckoApp, Settings | Reopen skips the engine start | med (RAM) | [E] | yes |
| EY-6 | L | Cache-first history load: test whether the restored page paints from the HTTP cache offline; if not, find the lever | BrowserActivity.ensureSession | Page live without network wait | med | [N] | yes |
| EY-7 | X | Refused part is transient first: 3 tries with backoff, one worker while retrying, reset on any success | StreamEngine.statusFailure/fetchTo/worker/claim | Many "expired" downloads simply continue | low | cause [R], gain [E] | no |
| EY-8 | X | Carry header ladder, master address, frame, rendition fingerprint into `StreamSource`/`StreamRec`; rotate headers on refusal | StreamEngine.kt:36-101, VideoFlow.pickStream, MediaHeaders | Hotlink/Referer refusals mid-download recover | low-med (stored format) | [R] | no |
| EY-9 | X | Refresh the playlist in place: swap part/key/init addresses by index, keep staged parts; refresh before resume when stale | StreamEngine.prepare/worker/drain | Token-per-playlist sites finish | med | design [E] | no |
| EY-10 | X | Expiry awareness: read expiry hints; refresh at start, resume and 60 s before; treat a network change as "links may be bound to the old address" | StreamEngine, Engine, DownloadController.startNetWatch | Queue wait / pause no longer ages links to death | low | [E] | no |
| EY-11 | X | "Needs fresh link" flow: reuse NEEDS_DECISION + LINK_EXPIRED + new REFRESH action; auto-match the next sniffed copy by fingerprint; assisted page open with muted play; optional hidden refresh | Model.kt, StreamEngine, VideoFlow.seen/maybe, BrowserActivity, Planner.failure | The download continues by itself after a fresh link appears | med | design [E] | partly |
| EY-12 | X | Single files: `refreshSource` (M10 adopt) + per-connection `useOriginal` | CORE/download/Engine.kt:52,325,336,574,585-590 | Same for direct MP4 links | low-med | [R] gap | no |
| EY-13 | X | Copy what the browser sent (Cookie) for cookie-gated CDNs | background.js:522-536, ShieldsBridge, MediaCandidate, Rec.headers | Fixes refusals caused by partitioned cookies | med (privacy handling) | hypothesis [N] | yes (lab first) |
| EY-14 | X | Keep the first response for engine-started downloads (one-time links, POST, `blob:`/`data:`) | TabDelegates.onExternalResponse:199-207, Engine | Downloads that cannot be re-requested work | med-high | [R] gap, behaviour [N] | yes |
| EY-15 | V | MAIN-world media hook: `fetch`/XHR bodies, media `src`/`play()`, `MediaSource`/`createObjectURL`, `Audio()` | new `EXT/media-hook.js`, background.js:283 registration, content.js | Finds sources behind JSON APIs and blob players | med (site compatibility) | gaps [R], yield [E] | no |
| EY-16 | V | DOM scan v2: shadow roots, `data-*`, JSON-LD / JSON scripts, `<track>`, `link rel=alternate`, observer, `pageshow` rescan, `performance` resource sweep | EXT/content.js:199-243 | Late, hidden and Back-restored players found | low | gaps [R] | no |
| EY-17 | V | List by rank not age (cap 24 -> 48), refill the 12-per-page `maybe` quota by time, protect played/long-pressed/stream entries | MediaList.kt:57-105, background.js:544 | Real video no longer pushed out by previews | low | [R] | no |
| EY-18 | V | Empty-state helper: "Look for videos on this page" = rescan + muted `play()` + wait 3 s | VideoFlow, content.js, menu | Pages that load nothing until play | low | [E] | no |
| EY-19 | V | Stream coverage: DASH multi-period, WebM representations, `EXT-X-DEFINE`/`SKIP`, keep master address | Dash.kt, Hls.kt, StreamEngine | Ad-spliced and WebM streams savable | med | [R] gaps | no |
| EY-20 | V | Subtitle companion files (HLS SUBTITLES, DASH text, `<track>`, sniffed `.vtt`/`.srt`) | Hls.kt, Dash.kt, VideoFlow, panel | Subtitles next to the video | low-med | [R] gap | no |
| EY-21 | V | Finder lab: 13 cases in sitelab + web-tests for hook regexes | tools/sitelab/vidlab_srv.py, web-tests | Proof and regression guard | none | [R] base exists | no |
| EY-22 | D | Validator-less files: step 1 open Pause / outage wait / resume with the tail-compare gate; step 2 parallel segments with seam compare | Model.kt:85, Planner.kt:16-17, Engine.kt:132/139/183/271/757/887/889 | Big files from CDNs without validators survive hiccups and use all connections | step 1 low, step 2 med | [R] | no |
| EY-23 | D | Space preflight for files and streams, "Make room" action, adaptive disk-cache cap | StreamEngine.prepare, Engine, MediaStoreSinks, GeckoPrefs | No failure at 80 % on a nearly full tablet | low | [R] storage 1.62 GB | yes |
| EY-24 | D | Abort in-flight sockets on a default-network change | DownloadController.startNetWatch:371-378, Engine/StreamEngine.onNetwork | Up to 15 s saved per hotspot flap | low | [E] | yes |
| EY-25 | D | IO path: `fdatasync`, no preallocation under 8 MiB, sync off the control thread | Engine.checkpoint:842, MediaStoreSinks | Smoother UI during big downloads | low | [E] | yes |
| EY-26 | D | M5 rest: bounded probe window for kind 0, slow-connection recycle, end-game steal | DOWNLOAD_MANAGERS.md M5 | Faster finish of the last 5 % | low-med | [R] open item | yes |
| EY-27 | D | Download lab: 1/2/3/4 connections on hotspot and Fast Wi-Fi, TLS and hash throughput | Diagnostics | Right defaults from numbers | none | [R] open measurement | yes |
| EY-28 | D | T0 raw-socket fake server tests (validator-less, token expiry, 403 bursts, resets) | core tests | Regression guard | none | [R] open item | no |
| EY-29 | S | Memory governor: periodic `MemAvailable` check; hibernate the oldest hidden tab after 120 s when low; react at RUNNING_LOW with hysteresis | BrowserActivity.kt:399-408, 796-830, LiveBudget | Fewer content-process kills | med | [E] | yes |
| EY-30 | S | Request-path cost histogram (p50/p95/p99) and per-(host,type) decision cache | EXT/engine.js, background.js | Faster page start on a latency-bound link | med | mean only [R] (723 us) | yes |
| EY-31 | S | Pref A/B batch (list in 7.4), each gated by a measurement and `tools/check-gecko-prefs.sh` | gecko-prefs.txt | Small gains | low | [E]/[N] | yes |
| EY-32 | S | Baseline profile for the app's own code; `BrowserDb` process singleton | build files, BrowserActivity.kt:202 | Earlier first frame | low | open in TASKS line 31/33 | yes |
| EY-33 | S | DoH and HTTP/3 A/B on the hotspot and Fast Wi-Fi profiles | GeckoEngine.kt:115, NetProfile | Faster new-host connects if one of them hurts | low | [N] | yes |
| EY-34 | P | "Page may be broken" chip from the page report (`rep`): suggest Shields off / desktop site for this site | ShieldsBridge `rep`, TabDelegates, UI | Broken pages fixed in one tap | low | data exists [R] | no |
| EY-35 | P | Compatibility audit: Diagnostics read-back of cookie behaviour / ETP / bounce tracking / fission, plus sitelab pages (third-party cookie login, payment iframe, OAuth popup) | GeckoEngine.readBackPrefs:185-225, sitelab | Know what breaks before the owner finds it | none | [R] settings, effect [N] | no |
| EY-36 | P | Page matrix: 20 page types, pass/fail table each alpha on the emulator | research/PAGE_MATRIX.md, sitelab mirrors | Pages "load properly" is checked, not hoped | none | method | no |
| EY-37 | P | Answer the prompts nobody answers: date/time and colour pickers, `navigator.share` (framework dialogs only); lab page first to confirm they are dead | GA/web/TabDelegates.kt (prompt delegate; permission handler 286-312), new dialog classes in APP/ui | Forms with date fields, colour inputs and share buttons work at all | low | no handler anywhere in the repo [R], behaviour [N] | no |

Groups: L last page, X expired link, V video finder, D download manager, S speed/RAM, P pages.

---

## 3. Part A - the last page, instantly (EY-1 .. EY-6)

### 3.1 What happens today on a cold reopen [R]

1. The owner swipes the app away (or backs out of the last screen). `onDestroy` closes every session and calls `ProcessLife.maybeExit()` (GA/BrowserActivity.kt:340-342); 2 s later `exitIfIdle` kills the process and the engine's sibling processes unless a download or update still needs it (APP/ProcessLife.kt). Android may also kill the cached process on its own on this 2 GB tablet. Either way the next open is a cold start. (Pressing Home and coming back is a warm start: no reload, `onResume` 247-262.)
2. `GeckoApp.onCreate` (GA/GeckoApp.kt:7-24) sets flags, `ProcessLife.init`, starts `Engine.start` (filter set, 4 MB, background thread).
3. `BrowserActivity.onCreate` (181-236): window background, prefs, `coldStartClear`, `Shields.init`, `buildUi`, `DownloadController.init`, `UpdateManager.init`, `BrowserDb`. Then: engine creation is posted AFTER the first frame (`postFrameCallback`, 205-214); `earlyCover()` (508-516) shows a dark panel with the chip "Opening <host>" and NO picture; a worker reads `session.txt`, the saved per-tab engine state and the 240 px thumbnail (223-234); `restore()` (518-543) builds the tabs; `show(sel)` leads to `ensureSession` (675-702): `GeckoSession.open`, `restoreState(state)` (history + scroll) or `loadUri`; `coverRevive` (708-711) -> `showRestoring` (1976-1983) puts the thumbnail up, dimmed.
4. The live page replaces the cover at `onFirstContentfulPaint` (TabDelegates.kt:196 -> `firstPaint` 1993 -> `hideRestoring`), or at page stop (1917), or after the timeout (25 s early cover, 515; 20 s revive, 1976).
5. Between 3 and 4 the page is fetched again over the hotspot (DNS, TCP, TLS, HTML, CSS, JS). Owner numbers: 1.5-5.2 s for the pages measured (research/DORABY.md, TABLET_DIAG_2026-11-13.md). How long the ENGINE start takes is not recorded in any file I read [N]; `Health` marks `runtime-create` and `runtime-ready` exist (207, 212) so one Health dump gives it (EY-2).

### 3.2 Why it does not feel like "the last page is there" [R]

- The picture is 240 px wide (APP/ui/ThumbStore.kt:15) drawn at the full 600 px width (RestoreOverlay.kt:63-68): about 2.5x enlargement, blurry; then a 70 % black rectangle (`dimAlpha = 0xB3`, lines 22 and 69) and a chip. Text is unreadable.
- It is captured at three moments only: when leaving by Back (BrowserActivity.kt:781), when the tab grid opens (1041), and when idle (1926: not loading, not typing, grid closed, more than 15 s since the last capture, more than 3 s since the last touch). NOT at pause (274-284). After 10 s of reading and a swipe-away, the stored picture is from before.
- It lives in `cacheDir/thumbs` (ThumbStore.kt:40). Android may delete cache directories when space is low; the tablet has had 1.62 GB free of 27.75 GB (research/TABLET_DIAG_2026-11-13.md), and `MediaStoreSinks` asks Android to free space with `allocateBytes` for big downloads [E].
- It carries no scroll position, so even a sharp copy would not match what the engine shows once `restoreState` has scrolled.

### 3.3 EY-1 - the "last look" snapshot

**What the owner gets:** the browser opens showing the page exactly as it was left, sharp, in the first frame; a thin line shows the live page is loading; the snapshot disappears when the live page shows the same thing.

**Store** - new `APP/ui/LastLook.kt` (shared UI module, engine-free):

```
filesDir/lastlook/<tabId>.webp   lossy WebP quality 70-75, the page area at FULL size (about 600 x 900 px), decoded as RGB_565 (1.2 MB RAM, only while shown)
filesDir/lastlook/<tabId>.json   {"v":1,"url":"...","sy":<scrollY px>,"w":<px>,"h":<px>,"at":<epoch ms>,"eng":"<Flavor.engineVersion>","pal":<skin id>,"dark":<webDark pref>,"desk":<desktop site>}
```

- Write order: image to a temp name, rename; then the json (same ladder as `TabStateStore.write`, TabStateStore.kt:64-76). The json existing means the image exists.
- Never in `cacheDir`. Never for private tabs. Never for `about:`, `data:`, the new-tab page or an error page. Keep only the selected tab plus the 2 most recently used (about 100-300 KB total [E]); `retain(ids)` like `TabStateStore.retain` (BrowserActivity.kt:1755).
- Use only when ALL match: the session's selected tab id and url (same check as `stateFromDisk` 721), `eng` equals the running engine version (same rule as `TabStateStore.useEngine`), `pal`/`dark`/`desk` equal, and the saved size equals the overlay size (see "Show").
- Wipe in every place thumbnails and tab state are wiped today: `coldStartClear` (BrowserActivity.kt:349-358), clear-browsing-data (1702), tab close (855). Add a Settings switch "Show the last page right away" (default on) next to the privacy items.
- Optional hardening: skip the capture when the page has a visible `input[type=password]` (content script flag at settle time).
- Backup: `android:allowBackup="false"` in both manifests (app/src/main/AndroidManifest.xml:34, geckoapp/src/main/AndroidManifest.xml:40) and `res/xml/data_extraction_rules.xml` excludes the `file` domain from cloud backup and device transfer, so a snapshot in `filesDir` never leaves the tablet [R]. Keep it so: never write it to external storage, never offer it as a shareable file. It shows what the Recents screen already shows, no more.

**Capture** - same call as `captureThumb` (1066-1086): `geckoView.capturePixels()`, then encode on `Io.exec`:

| Trigger | Rule |
| :- | :- |
| `onPause` | first line of `onPause`, before `saveSessionNow()`: capture the current regular tab if it shows an http(s) page, is not loading and `look.at` is older than 1.5 s or the scroll changed. The compositor read-back must finish in a few hundred ms: the owner needs longer than that to swipe the app away from Recents [E]. |
| Settled | 700 ms after the last scroll change, at most once per 4 s, not while loading, typing, in full screen video or with the grid open. This keeps the file fresh when Android kills the process without any callback. Scroll events: `GeckoSession.ScrollDelegate.onScrollChanged` (UI thread; already used in GA/web/ScrollTest.kt:34-35 [R], not set on normal tabs). Implementation: the delegate stores `scrollY` and a timestamp only; one timer re-arms itself while events keep coming (no per-event allocation). |
| Page settled | 1.5 s after `onPageStop` of a new page. |

Cost [E]: read-back 8-20 ms, WebP encode 15-40 ms on a worker (A53), 40-100 KB written, at most every 4 s while reading, nothing when idle. Verify with the Health timeline (EY-2) and the 64-65 fps scroll figure in TABLET_DIAG (the delegate must not cost a frame).

**Show** - extend `RestoreOverlay` with a "look" mode (RestoreOverlay.kt:42-50 `show`):

1. In `onCreate`, where `earlyCover()` already reads the session synchronously (508-516), also read `<selectedId>.json` and decode the WebP on the UI thread (15-30 ms [E]; log it as a Health mark). Draw it in the FIRST frame: bitmap 1:1 at the top-left of the page area, no dim, no chip, no text.
2. If the overlay width differs from the saved width by more than 2 px (rotation, split screen), draw it scaled to width (still sharp, undimmed) when the aspect is within 5 %, else fall back to today's cover.
3. A 2 dp progress line along the top edge of the page area (reuse the page `progress` view, BrowserActivity.kt:594); no animation, no text (the owner's no-animation rule).
4. After 4 s without first paint, add a slim chip at the BOTTOM of the page area: "Saved view - still loading". Honest, and out of the way. After the cap (12 s in look mode) hide the overlay and let the engine's own state or error page show.
5. `coverRevive` (708-711, a hibernated tab coming back) and `sessionGone` -> `showRestoring` (GA/BrowserActivity.kt:1949-1975 and 1976-1983: the front tab comes back from its saved state after the content process died or was killed) use the same look when it exists and matches, keeping their own message chip (the owner should still be told that the page was reloaded after a crash); otherwise the thumbnail as today.

**Hide rule** (replaces "first contentful paint only", `firstPaint` 1993):

```
hide when  FCP seen  AND  ( savedScrollY == 0
                            OR |currentScrollY - savedScrollY| <= 4 px
                            OR page stopped
                            OR 1500 ms after FCP )
```

The 1500 ms bound exists because FCP can arrive before the engine has scrolled to the restored position, and a hide at that moment shows the top of the page for a moment, then a jump [N: check on the emulator with a long page]. No fade: the repo's rule is instant swaps (RestoreOverlay.kt:16-17). A sharp snapshot swapped for the same pixels is invisible.

**Touch:** the overlay keeps swallowing page touches (`isClickable`, RestoreOverlay.kt:34); toolbar, tabs, menu and dock stay live. A swallowed tap on the snapshot makes the progress line brighter (a cue, never a toast). State this to the owner plainly: the snapshot cannot be scrolled; it is replaced by the live page as soon as that has painted.

**Edge cases to test:** rotation between close and open; engine version change after an update; skin change; page taller than the screen with scroll at 60 %; a page that redirects on load (url differs: drop the snapshot at `onPageStart` when the new url differs from `look.url` ignoring fragment); airplane mode (snapshot shows, line stays, chip after 4 s, error page after the cap); disk full (no crash, no partial file); 12 tabs (disk cap holds); a tab opened from a link while the app starts (`launch != null`, 539: no snapshot, as today).

**Do not** show the snapshot for a tab the owner did not leave in front, and do not keep it after the page has been closed.

### 3.4 EY-2 - the cold-start timeline (do this FIRST)

Everything else in Part A is judged by it. Anchor at process start (`SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()`, API 24+) and log these marks with `Health.log("start", name, ms)`:

`app-create` (GeckoApp.onCreate) - `activity-create` - `first-frame` (the Choreographer callback at 205) - `look-drawn` (first `onDraw` with a bitmap) - `runtime-create` / `runtime-ready` (207/212) - `session-open` (`ensureSession`) - `first-paint` - `page-stop`.

Add one Diagnostics line the owner can copy: `cold start (ms since process start): frame 310 | look 340 | runtime 1180 | open 1210 | paint 2650 | stop 3900`, and a second line for warm starts. Five cold starts, median. Record in `research/COLD_START.md` before and after each of EY-1, 3, 4, 5.

### 3.5 EY-3 - first-frame diet [E]

`onCreate` does all of this before the first frame: `coldStartClear`, `Shields.init`, two `Pal`s, `buildUi`, `DownloadController.init` (restores interrupted downloads), `UpdateManager.init`, listeners, `BrowserDb(this)` (lines 183-202). The cover cannot draw before they finish. Move `DownloadController.init`, `UpdateManager.init`, `BrowserDb` and any part of `Shields.init` the first frame does not need into the post-frame block at 205-214 (or a worker), ONE AT A TIME, judged by the `first-frame` mark. `coldStartClear` must stay first (it decides what `restore` may read). Open items K8/K13 in research/SPEED3.md are the same idea.

### 3.6 EY-4 - prefetch the engine's files [E]

Native libraries are extracted to the app's library folder (`jniLibs { useLegacyPackaging = true }`, geckoapp/build.gradle.kts:68 [R]), so the engine library is a real file. From `GeckoApp.onCreate`, UI process only (the guard at GeckoApp.kt:21-23 shows how), start a background-priority thread that asks the kernel to read it ahead: open the file and call `Os.posix_fadvise(fd, 0, 0, OsConstants.POSIX_FADV_WILLNEED)`; do nothing when `Health.memAvailableMb()` is below about 400. The same for the engine's resource archive if it is a separate file [N: find it in the APK/library folder]. Gain: shorter `runtime-create -> runtime-ready` [E: 200-600 ms on eMMC]. Judge by the EY-2 marks, A/B with and without; remove if no gain. Risk: low (a hint; page cache is reclaimable).

### 3.7 EY-5 - "Stay ready after closing" (optional, off by default) [E]

`ProcessLife.maybeExit(delayMs = 2000)` is a deliberate choice: the swiped-away app must not hold the engine's memory (ProcessLife.kt header comment). Offer a setting: Off (default) / 5 / 15 minutes. When on: `onDestroy` calls `maybeExit(N * 60_000)`; `GeckoApp` registers `ComponentCallbacks2.onTrimMemory` and exits at once when the UI is gone and the level is `TRIM_MEMORY_BACKGROUND` or worse; `ProcessLife.cancel()` (already at onCreate:184) keeps the process when the owner returns in time. What it buys: the reopen skips the engine start (runtime and content process alive; DNS/TLS/HTTP cache warm). What it does NOT buy: live pages (sessions are closed in `onDestroy`, line 340), so the page still reloads behind the snapshot. Rule: never combine with "clear browsing data when the app is closed" (`coldStartClear` runs only at the next COLD start, BrowserActivity.kt:345-359): when that setting is on, ignore this one. A later, bigger step (process-level tab registry so a new Activity re-attaches live sessions) is possible but touches the 2.4k-line Activity: do not start it before EY-2 shows how often a warm process exists at reopen.

### 3.8 EY-6 - does the restored page paint from the HTTP cache? [N]

Test, 2 minutes on the emulator: open a news page, wait for load, switch Wi-Fi off in the emulator, swipe the app away, reopen. Page painted from cache = history loads already prefer the cache and the only cost left is engine start + render: stop here. Error page = history loads revalidate or the cache was purged (the Gecko disk cache is 512 MB, gecko-prefs.txt:36; where it lives and whether Android's cache purge can reach it is [N]). Then the fix is a per-load cache preference for the restore load only (there is no public cache flag on `loadUri`; a temporary `browser.cache.check_doc_frequency` change around `restoreState` is the lever to try, raced against the pref's asynchronous delivery: measure before trusting it). Do not change that pref globally: it would show stale pages.

### 3.9 What to tell the owner (after EY-1 ships)

"The page you left is on screen as soon as the browser opens, sharp. It cannot be scrolled until the live page has loaded under it (a line at the top shows progress). If the network is off you keep seeing it with a small note, then the normal error page."

---

## 4. Part B - "link expired": resume or reconnect by itself (EY-7 .. EY-14)

### 4.1 Every place the message comes from [R]

| # | Where | Trigger | What follows |
| :- | :- | :- | :- |
| 1 | CORE/media/StreamEngine.kt:1138-1142 `statusFailure`, thrown from `readInto` (1100), `readText` (766), key fetch (1011) | any 401, 403 or 410 on a part, playlist, key or init | `Failed(LINK_EXPIRED)` |
| 2 | StreamEngine.kt:898 `worker`, 569 `supervise` | a `Failed` from ANY worker | `job.failure` set, `job.stop = true`, every open response aborted: ONE refused part fails the whole job and the other workers' good bytes are not used further |
| 3 | StreamEngine.kt:638-641 `prepare` | resume or restart | the saved `rec.playlistText` is reused: the part addresses (with their old tokens) are replayed; `readText` is called only when no text is saved |
| 4 | StreamEngine.kt:1095, 764, 1009 | every request | one fixed header set, `job.rec.headers`; `MediaHeaders.sets` (the ladder that finds the playlist, VideoFlow.kt:268-282) is never used during the download |
| 5 | StreamEngine.kt:36-57 `StreamSource` | enqueue | no master address (`pickStream` passes the VARIANT playlist address, VideoFlow.kt:406-420), no `frame`, no `viaHeaders` ladder, no rendition fingerprint: the engine cannot find "the same video" again |
| 6 | CORE/download/HttpSemantics.kt:108, Engine.kt:585-590 `openWindow` | 401/403/404/410 on a file window | ONE re-ask of the clicked address (`reResolved`, `useOriginal`), then `fail(LINK_EXPIRED)` (404: HTTP). Works for links that redirect to a fresh signed address; not for a signed address itself |
| 7 | CORE/download/Planner.kt:174-176 | display | "The link expired. Open the page again." No button. `Action.RETRY` re-asks the same dead address |
| 8 | GA/web/TabDelegates.kt:199-207 `onExternalResponse` | a download started by the engine (attachment, unknown type) | the first response is CLOSED and the same address is requested again by the manager: a link that works once, a POST and a `blob:`/`data:` address cannot work this way |

### 4.2 Why links die in practice

| Cause | Evidence | Fix row |
| :- | :- | :- |
| Time-limited signature (`expires`, `exp`, `hdnts`, `X-Amz-Expires`, signed JWT) outlived by queue wait, pause, WAITING_NETWORK or a slow link | [E] common CDN practice; the code replays saved addresses ([R] row 3) | EY-9, EY-10 |
| Link bound to the client address (`ip=` style); the hotspot reconnects or the Fast Wi-Fi tunnel is switched on or off mid-download | [E]; the app has both a hotspot and an optional tunnel ([R] SPEED4.md) | EY-10, EY-24 |
| Token allows only a few simultaneous connections; three workers plus the page's own player trip it, the answer is 403 | [E]; the job runs up to 3 workers outside the connection budget (StreamEngine.kt:554) | EY-7 |
| Referer / Origin / Cookie gate: part refused when the headers differ from the player's | [R] the ladder exists only at find time (row 4) | EY-8, EY-13 |
| Cookie set inside an embedded player frame is partitioned by the top-level site: `cookieBehavior = ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS` (GA/GeckoEngine.kt:94-95 [R]); the manager's fetcher (kind 1, Gecko's network stack with no page context) may not send it | [N] hypothesis: the lab case in 4.12 decides | EY-13 |
| One-time link spent by the engine's first request, then re-requested by the manager (row 8) | [R] behaviour in code; effect [N] | EY-14 |
| Playlist regenerated per request with fresh tokens; the saved text is stale | [E] | EY-9 |
| CDN edge hiccup: a single 403/410 and fine on the next try | [E] | EY-7 |

### 4.3 The recovery ladder

```
a part is refused (401 / 403 / 410; for streams also 404 once something was fetched)
 L0  retry the same part: 3 tries, jittered 1.5 s / 4 s / 9 s, ONE worker while it retries. Any successful part resets the count.
 L1  other headers: next set of the saved ladder (viaHeaders first). A set that works becomes rec.headers.
 L2  fresh playlist: re-read the playlist (then the master) with the working headers. Same structure? swap the part, key and init addresses by index,
     keep every staged part, carry on. No message to the owner.
 L3  the playlist itself is refused, or it is another stream: state "needs a fresh link".
       auto: the next sniffed copy of the same video (fingerprint match) is adopted and the job continues by itself;
       assisted: one tap opens the source page; the page's player is nudged (muted play); the same matching does the rest;
       optional: a hidden refresh in a background session (phase 2).
 L4  no timeout: the job stays in the list with a Refresh button until the owner cancels it.
before all of this: if the link's expiry is known to be past (or the network changed since the link was made), start at L2 without spending requests on L0/L1.
```

### 4.4 EY-7 - L0, "refused" is transient first (StreamEngine)

Sketch (not compiled):

```kotlin
private class Denied(val status: Int) : RuntimeException(null, null, false, false)

private fun statusFailure(status: Int, resp: FetchResponse? = null): RuntimeException = when {
    status == 401 || status == 403 || status == 410 -> Denied(status)      // was Failed(Reason.LINK_EXPIRED, status)
    // 404 and the rest of 4xx stay Failed(HTTP) EXCEPT 404 on a part once any part of this job succeeded: Denied(404)
    ...
}

// worker(): read the media per claim (it is captured once per worker today, lines 879-883) and handle Denied
val idx = claim(job, n) ?: break
try {
    val seg = job.mediaNow().segments[idx]
    fetchTo(job, fetcher, seg.url, seg.from, ..., File(dir, "$idx.seg"), count = true)
    job.denyStreak = 0
    ...
} catch (e: Denied) {
    synchronized(lock) { job.claimed.clear(idx) }
    if (denyOrRecover(job, e.status)) continue          // L0 delay, then L1, then L2; single-flight
    throw Failed(Reason.LINK_EXPIRED, e.status)
}
```

- `denyOrRecover`: `denyStreak++`; while `denyStreak <= 3` sleep a jittered backoff and set `job.recovering = true` so `claim()` makes the other workers wait (a wait loop inside `claim` that polls every 200 ms and honours `job.stop`; returning null would END the worker, line 886). After the streak: L1, then L2. Honour `Retry-After` when present (`statusFailure` already reads it for 5xx).
- `Failed(LINK_EXPIRED)` must not abort the job any more by itself: in `supervise` (574-583) a `Failed(LINK_EXPIRED)` becomes `NEEDS_DECISION` with `reason = LINK_EXPIRED` (the state, codes and `Action` already exist: Model.kt:8, 20, 119; `needDecision` Engine.kt:776), staged parts kept, `rec.next` kept. That is L3.
- The other workers' responses are aborted only for L3 now, not at the first refusal.
- Tests (core, fake `Fetcher`): one part answers 403 once -> job completes, no `LINK_EXPIRED`; 403 for every part until the playlist is re-read -> completes after L2; the number of requests per part index is at most 2 (staged parts are not fetched again).

### 4.5 EY-8 - carry what the download needs to recover (VideoFlow -> StreamSource -> StreamRec)

Add to `StreamSource` (36-57) and `StreamRec` (59-101, including the `Rec` copy at 100-101 and the stored form; old records must still load): `masterUrl` (the candidate's address, `MediaCandidate.url`), `frame` (the document that asked), `headerSets` (the ladder: the list `MediaHeaders.sets(c, tab.url)` already returns, 6 maps at most, small), `fp` (fingerprint, 4.8), `madeAt`, `expiresAt` (0 = unknown). `pickStream` (VideoFlow.kt:406-420) fills them from the candidate and from `StreamPick`; `StreamPick.headers` stays the first choice. Single files do the same through `Source`/`Rec` (Rec.headers exists, Model.kt:57).

### 4.6 EY-9 - L2, refresh the playlist in place

```
refreshPlaylist(job):
  for each header set (working one first): text = readText(rec.masterUrl ?: rec.url)    // master first when a master exists, then re-pick the same rendition
  new = Hls.parse / Dash.parse(text)             // Dash: the same repId
  if !sameStructure(old, new) return false
  job.media = new  (under lock);  rec.playlistText = text;  persist(rec)
  return true

sameStructure(old, new):
  - same segment count, same container, same init/no-init, same key method
  - every duration within 50 ms; every file name (last path component, query ignored) equal       // tokens may sit in the path: compare names only
  - keys: fetch the new key; if its bytes differ from the key already used, do NOT swap (staged parts are stored encrypted and decrypted at append time)
  - discontinuities equal (server-side ad insertion changes them: a different structure means "another stream" -> L3)
```

Notes: `Hls.Seg`, `Hls.Key`, `Hls.Init` and `Hls.Media` are immutable (Hls.kt:54-61), so build a new `Media` and publish it as `job.media`; `worker()` and `drain()` must read it per use instead of capturing it once (StreamEngine.kt:882, 891, 552). `prepare` (634-661) re-reads the playlist first when `rec.playlistText` is older than 10 minutes, when the last stop was `LINK_EXPIRED`, or when `expiresAt` is past; if that read fails it still tries the saved text (parts may not need new tokens).

### 4.7 EY-10 - expiry awareness and network changes

- `ExpiryHints.parse(url, headers)` in core: query `expires`, `exp`, `e`, `Expires`, `X-Amz-Date` + `X-Amz-Expires`, `hdnts=...exp=...`, `st`/`end` pairs, a JWT `exp` claim in a `token`-like value, `Expires` in `Cache-Control`/`Expires` response headers. Unknown = 0. Pure function, easy to test.
- Use: at job start and at resume, if `expiresAt - now < 90 s` go to L2 first. While running, refresh at `expiresAt - 60 s` (a periodic check in the supervisor loop, 250 ms tick, StreamEngine.kt:556-568).
- Network change: `DownloadController.startNetWatch` (371-378) already tells the engines (`onNetwork`). Record `job.netEpoch`; a refusal after the epoch changed skips L0/L1 (the link may be bound to the old address) and, with EY-24, aborts the sockets so the workers re-dial at once.
- Queue wait: when a job leaves the queue after more than a few minutes, treat it as "resume" (check expiry before the first request).

### 4.8 EY-11 - L3, "needs fresh link" without losing anything

State: reuse `NEEDS_DECISION` + `Reason.LINK_EXPIRED` (no new persisted state code); new `Action.REFRESH`; the row text changes from "The link expired. Open the page again." (Planner.kt:176) to "Link expired - tap Refresh. Everything downloaded so far is kept." Notification and the AL2 banner use the same words. Do not touch `MiniTracker`/`DownloadPopup` until AL2 has landed; add the text through the existing `Planner.failure`/`Format` functions.

**Fingerprint** (`StreamSource.fp`, computed when the download is enqueued, pure function in core):

```
host + path of the master (query dropped) | host + path of the variant | segment count | total duration (ms) | container | key method |
rendition key: height / bandwidth / label for HLS, repId for DASH | page address without fragment | frame address
files:  total length (rec.total) | strong ETag if any | file name from the address or Content-Disposition
match score: master path equal (3) + variant path equal (2) + count equal (2) + duration within 1 s or 1 % (2) + rendition equal (2) + page equal (1); adopt at >= 8 of 12; two candidates >= 8: take the higher; a tie asks the owner
```

**Auto-match** (`GA/web/RefreshWatcher.kt`): `VideoFlow.seen` (77-80) and `maybe` (88-115) call `watcher.offer(tab, candidate)` after `t.media.add(c)`. The watcher keeps the list of jobs waiting for a link (ask `DownloadController` for jobs in NEEDS_DECISION + LINK_EXPIRED; keep it in memory, refresh on `onChanged`). For a playlist candidate it reads the text with `DownloadController.fetchTextStatus` (224) and the candidate's `viaHeaders`/header sets, parses, scores; for a file candidate it compares `length` with `rec.total` and the name. On a match: `DownloadController.refreshSource(id, newUrl, newText, headers)` -> streams: set `rec.url`/`masterUrl`/`playlistText`/`headers`, clear the failure, `requeue(job, wipe = false)`; the staged parts are reused if `sameStructure` holds, else the structure changed (offer Restart). The banner says "Link refreshed - download continues". Never adopt a candidate from a tab that is not on the job's page (or at least its site) and never from a private tab for a non-private job.

**Assisted open:** `Action.REFRESH` -> `BrowserActivity.openForRefresh(job)`: show the tab already on `rec.pageUrl`, else open one (respect `LiveBudget`); banner: "Press play on the video. The download continues by itself." A content-script nudge makes this one step less: muted `play()` on the largest paused `<video>` (muted autoplay is allowed in this app, research/VIDEO_AUDIT.md section 1 [R]); never click page elements. There is no app-to-extension push channel today: `ShieldsBridge` (GA/web/ShieldsBridge.kt:50) is a `WebExtension.MessageDelegate` that only answers messages the extension sends. Add `onConnect` with a port the extension opens once at start (`runtime.connectNative`), or let the nudge ride on the answer to the next `cfg` message; decide when you read the class.

**Optional phase 2 - hidden refresh:** open `rec.pageUrl` in a background `GeckoSession` that is not attached to the view, activate it, nudge, and close it after the match or after 20 s. Only when the live-tab budget allows (`LiveBudget`), never for a private job, never when the page asked for a popup. Cost: one more page in the single content process [E 30-150 MB]. Skip it until the first two paths are proven.

### 4.9 EY-12 - single files

`Engine.refreshSource(id, newUrl, headers)` is M10's `adopt` (research/DOWNLOAD_MANAGERS.md, open): allowed only in PAUSED/FAILED/NEEDS_DECISION/WAITING; sets `rec.url = newUrl`, `rec.finalUrl = null`, `rec.headers = headers`, `job.reResolved = false`, `job.useOriginal = true`, then `requeue(job, wipe = false)`. Safety is already built in: the resume request carries `If-Match`/`If-Unmodified-Since` or, with no validator, the 4 KiB tail compare (Engine.kt:556-560 [R]), and a different file ends in `FILE_CHANGED` + NEEDS_DECISION (573, 579). Also make `useOriginal` per connection: today one worker's refusal flips it for every worker (52, 574, 588). Apply L0 first: a throttled 403 must not spend the one re-ask.

### 4.10 EY-13 - copy what the browser sent (Cookie) [N, lab first]

Hypothesis: with `ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS` a cookie set while the page's embedded player frame talks to a CDN belongs to the page's partition; the manager's request has no page context and does not carry it, so a CDN that gates parts on that cookie answers 403 mid-download. Test BEFORE any code: extend the lab (4.12) with a CDN that sets a cookie in the iframe and refuses parts without it; play in the page (works), download with the manager (expected: 403). If it fails as predicted: the extension already sees every request's headers (background.js:522-536 reads only Referer and Origin; the `Cookie` header is available to `onBeforeSendHeaders` for hosts in the manifest [N: confirm in 160]); pass `Cookie` for that candidate only, store it in `rec.headers` (scrubbed at terminal states like today), send it with the kind-0 fetcher (`GeckoFetcher` drops the Cookie header on purpose, GeckoFetcher.kt, and Gecko's jar would add its own) or extend `GeckoFetcher` for an explicit value. Privacy rules: never log header values; never copy cookies of other hosts; never for private jobs into any persistent store beyond the existing private-job rule (`rec.priv` jobs are not persisted, Model.kt:84).

### 4.11 EY-14 - keep the first response for engine-started downloads [N]

`onExternalResponse` closes the body and lets the manager request the address again (TabDelegates.kt:203-206, comment included). Consequences [R]: a one-time or short-lived link is already spent; a POST download becomes a GET; `blob:` and `data:` addresses (TASKS.md line 33 backlog "blob: downloads") cannot be fetched at all by `UrlConnectionFetcher` (BAD_URL). Proposal: pass the open `WebResponse.body` stream into the manager as the FIRST connection of the job (a `PrefetchedBody` on `Source`): it starts at byte 0, needs no re-request, and is the only connection for links that cannot be re-requested; extra ranged connections are opened only when the server advertised ranges and a second request is answered (a refusal of the second connection is not an error: keep the first). `[N]` to test on the emulator: whether the body stream may be read after `onExternalResponse` returns, on another thread, and for how long Gecko keeps the channel alive (the comment says the channel starts suspended and resumes on the first read). If it cannot be read later, read it inside the callback into the sink's first window on a worker and return at once. Test pages: one-time token link, POST form download, `blob:` created by script, `data:` link.

### 4.12 Proof for Part B

Extend `tools/sitelab/vidlab_srv.py` (it already has a movie page, an embedded player page, a CDN that wants the player's Referer, disguised `.txt` playlists, `.jpg` parts with a PNG header, AES-128 [R]). New routes:

| Route | Behaviour | Expected after the rows |
| :- | :- | :- |
| `/hls/flaky/` | part 5 answers 403 once | completes, no message (EY-7) |
| `/hls/tokens/` | parts need `?t=` from the newest playlist; a playlist older than 20 s is refused for parts | completes via L2 (EY-9) |
| `/hls/dead/` | parts and playlist refused after 30 s | NEEDS_DECISION "Link expired - tap Refresh"; opening the page and playing continues the job (EY-11) |
| `/hls/cookie/` | the iframe page sets a cookie for the CDN host; parts need it | hypothesis check (EY-13) |
| `/file/signed.mp4` | `Range` capable, address valid 20 s, validator present / absent variants | refresh via `refreshSource` (EY-12); no-validator variant keeps the partial (EY-22) |
| `/file/once.bin` | answers 200 once, then 410 | downloads fully through the first response (EY-14) |

Unit tests in `core` (`StreamEngineTest.kt` and a new `EngineRefreshTest.kt`) with a fake `Fetcher` cover L0, L2, the structure check (shifted segments by ad insertion must be refused), key mismatch, `ExpiryHints.parse`, fingerprint scoring. The emulator run records Health lines (host and status only) and the request count per part index.

---

## 5. Part C - the video finder: find as many sources as possible (EY-15 .. EY-21)

### 5.1 How a source is found today [R]

| Channel | Where | What it sees | Limits |
| :- | :- | :- | :- |
| Network sniffer | EXT/background.js:567-600 `onHeadersReceived` | Responses of type media / xhr / other / object / sub_frame / main_frame with status 200 or 206, judged by content type (`MEDIA_CT`, 462) or address (`MEDIA_EXT`, 460) | Skips segments (`SEGMENT`), plain files under 100 KB, everything that is not 200/206. JSON and HTML answers are never looked at. One report per address per frame (`dedupeKey`), 300 per tab |
| Disguised playlists | background.js:544-565 `maybeMedia`, APP side GA/web/VideoFlow.kt:88-115 `maybe` | Responses with no useful type (text/plain, octet-stream, empty) or a "picture"/"stylesheet" fetched by a script; the app reads the first 4 KiB with the header ladder | `MAYBE_PER_PAGE = 12`; the counter is cleared only by a main-frame request (line 379), so a single-page site never gets new probes; `NOT_MAYBE` excludes `.json` and `.html` |
| Markup scan | EXT/content.js:201-243 | `<video>`, `<audio>`, `source[src]`, og:video / twitter:player metas, the first 600 links, inline scripts (each under 200 000 chars, 300 000 chars in total), a media-address regex | At most 40 reports per document; two passes only (idle after DOMContentLoaded, 2.5 s after load); no MutationObserver |
| Player report | content.js:158-182 | `loadedmetadata` of a video/audio whose address starts with `http` -> picture size, length, label of the matching `<source>` | One capture listener on `document`; blob addresses are skipped, so players built on MediaSource never get a size or a label |
| Long press / element | content.js ~245-267, GA/web/VideoFlow.kt (`longPress`) | The element under the finger and its own address (added to the list even when the sniffer never saw it) | Only elements with a source |

### 5.2 What it cannot see today [R unless tagged]

| Gap | Cause | Sites that do this | Row |
| :- | :- | :- | :- |
| The real address arrives as data: an API answers JSON such as `{"hls":"https://cdn.example/x/master.m3u8"}`, or HTML with the player config; the player then loads the playlist | The sniffer only reports the playlist when it is requested by the page with a recognisable type. When the page code builds the request from a signed value or runs it only after a tap, the address is first visible INSIDE the answer, which `MEDIA_CT` and `MAYBE_CT` ignore (background.js:540-551, 585) | movie/streaming sites (embed -> API -> playlist), social video, news sites | EY-15 |
| MediaSource players (blob address) | `vid` skips `blob:` (content.js `report`), so no size and no label join the sniffed playlist; nothing tells the owner why the list is empty | most modern players | EY-15 (hints), EY-18 |
| Dynamic assignment `video.src = x`, `new Audio(x)`, `source.src = x` | the markup scan runs twice; `loadedmetadata` only reports addresses that started playing | many custom players | EY-15, EY-16 |
| Videos inside shadow roots (web components) | `querySelectorAll` does not cross shadow boundaries; media events are not composed, so the document-level capture does not see a video inside a shadow root [N: lab case 3] | newer sites and design-system players | EY-16 |
| Attributes other than `src` | the scan reads only `src`, `currentSrc` and a few meta tags | `data-src`, `data-video`, `data-mp4`, `data-hls`, video.js `data-setup` JSON, lazy-loaded players | EY-16 |
| Players created late (after a tap, or in an infinite feed) | no observer; second pass 2.5 s after load | feeds, "click to load player" sites | EY-16 |
| Back from the page cache | the page is alive (bfcache keeps 6 pages for an hour: gecko-prefs.txt:22-23), nothing is requested again, and the scan does not run again; whether the app's list survives Back depends on whether `pageStarted` fires for a restore (BrowserActivity.kt:580 -> `clearTab`) [N: lab case 8] | every site, after Back | EY-16 |
| Single-page navigation | probe quota and "seen" sets reset only on a main-frame request | single-page sites | EY-17 |
| The 24-entry list drops the OLDEST entry | MediaList.kt:101-102 (`items.add(0, c)`, then the last is removed) | pages that load many previews/ads first, then the real video later or earlier | EY-17 |
| Short clips hidden next to a stream | `MIN_WITH_STREAM` (4 MB) in the display filter (MediaList.kt `usableUnsorted`) | pages with an ad stream and a short real clip | EY-17 |
| Subtitles | HLS `EXT-X-MEDIA` renditions are parsed (Hls.kt:112-114) and never offered; `<track>` and `.vtt` / `.srt` requests are not matched | all | EY-20 |
| DASH with several periods; WebM-only manifests | Dash.kt:61-65 returns UNSUPPORTED; `mp4Friendly` (Dash.kt:41, used GA/web/VideoFlow.kt:386-397) skips representations the MP4 writer cannot take | ad-spliced VOD, VP9/Opus-only manifests | EY-19 |
| After a cold start: the first page may load before the extension runs | the extension is installed asynchronously (GA/web/ShieldsBridge.kt:335 `ensureBuiltIn(...).accept`) while the first session loads [N: lab case 8b] | restored page right after start | EY-16 (resource observer), EY-2 marks |

### 5.3 EY-15 - a hook in the page that sees what the page's own code receives

Add `EXT/media-hook.js`, run in the page's own world, in every frame, at document start. Register it in the SAME call as the pop-up guard (background.js:283-286: `contentScripts.register({... world: 'MAIN'})`): the `js` list becomes `[{file:'guard-main.js'},{file:'media-hook.js'}]`. That call already works in the shipped engine (Diagnostics reads `guardState`); add `hookState` next to it.

**What it does** (every wrapper calls the original with the same arguments and returns its result unchanged; every line of ours sits in a try/catch; nothing is blocked, fetched or changed):

1. `window.fetch`: when the answer is OK, not opaque, and its type is JSON (any `...json`), `text/plain`, `text/html`/`text/xml`, an HLS or DASH type, and its length is known and at most 300 000 bytes (or the type is JSON), clone it and run the address regexes over the text. At most 30 scans per page.
2. `XMLHttpRequest`: on `load`, for `responseType` empty or `text` and a text-like type, the same scan over `responseText` (at most 300 000 chars, at most 30 scans).
3. Media element sources: wrap the `src` setters of `HTMLMediaElement` and `HTMLSourceElement`, and `HTMLMediaElement.prototype.play` (report the current address; note "blob" when the address is a blob). `play` also tells the extension "a player started": the content script schedules one more markup scan 1.5 s later.
4. Markers for the empty list: `MediaSource.prototype.addSourceBuffer` (note "mse" and the MIME string) and `Navigator.prototype.requestMediaKeySystemAccess` (note "eme" and the key system). They let the app say WHY a page offers nothing (a DRM player, or a player that loads pieces from an address it never revealed) instead of showing no menu entry (EY-18).
5. Optionally `Element.prototype.attachShadow` (keep a list of up to 200 created roots) so DOM scan v2 can look inside closed roots too. Skip it in the first version; open roots are reachable without it.

**Message path** (the same pattern as the guard): the hook dispatches a `CustomEvent` named `__kst_med_v1` on `document` whose `detail` is a plain STRING `kind|value`; content.js listens in the capture phase and forwards it. For `kind` = `url`, `src` or `play` forward it as the existing `k:'dom'` message (background.js:266-271 -> ShieldsBridge `onMedia`, GA/web/ShieldsBridge.kt:127: ad networks are dropped by the same lists): NO app change is needed for the addresses. The markers (`mse`, `eme`, `blob`) need one new message `k:'hint'` relayed by the background (about 5 lines) and a handler in the bridge that stores a small set on the tab (used by EY-18).

ES5 sketch (not compiled; keep ASCII):

```js
'use strict';
(function () {
  var EVT = '__kst_med_v1';
  try { if (window.__kstHook) return; Object.defineProperty(window, '__kstHook', { value: 1 }); } catch (e) { return; }
  var HINT = /\.(?:m3u8|mpd|mp4|webm|m4v|mov|mp3|m4a|aac|ogg|opus)/i;
  var ABS = /https?:\/\/[^\s"'<>()\\]+?\.(?:m3u8|mpd|mp4|webm|m4v|mov|mp3|m4a|aac|ogg|opus)(?:\?[^\s"'<>()\\]*)?/gi;
  var REL = /"((?:\/\/|\/)[^"\s\\]+?\.(?:m3u8|mpd|mp4|webm|m4v|mov|mp3|m4a|aac|ogg|opus)(?:\?[^"\s\\]*)?)"/gi;
  var TEXTY = /^(?:[a-z]+\/[a-z0-9.+-]*json|text\/(?:plain|html|xml)|application\/(?:x-mpegurl|vnd\.apple\.mpegurl|dash\+xml)|)$/;
  var MAX_BODY = 300000, MAX_SCANS = 30, MAX_SENT = 60, scans = 0, sent = 0, seen = {};
  function emit(kind, v) { try { document.dispatchEvent(new CustomEvent(EVT, { detail: kind + '|' + v })); } catch (e) {} }
  function addr(u, base) { try { return new URL(String(u).replace(/&amp;/g, '&'), base || location.href).href; } catch (e) { return ''; } }
  function report(kind, u, base) {
    var a = addr(u, base);
    if (!a || a.indexOf('http') !== 0 || seen[a] || sent >= MAX_SENT) return;
    seen[a] = 1; sent++; emit(kind, a);
  }
  function scanText(t, base) {
    if (!t || t.length > MAX_BODY || !HINT.test(t)) return;
    t = t.replace(/\\u002f/gi, '/').replace(/\\u0026/gi, '&').replace(/\\\//g, '/');
    var m; ABS.lastIndex = 0; while ((m = ABS.exec(t)) !== null) report('url', m[0], base);
    REL.lastIndex = 0; while ((m = REL.exec(t)) !== null) report('url', m[1], base);
  }
  if (typeof window.fetch === 'function') {
    var realFetch = window.fetch;
    window.fetch = function fetch(input, init) {
      var p = realFetch.apply(this, arguments);
      try {
        p.then(function (res) {
          try {
            if (scans >= MAX_SCANS || !res || !res.ok || res.type === 'opaque') return;
            var ct = String(res.headers.get('content-type') || '').toLowerCase().split(';')[0].replace(/\s+/g, '');
            if (!TEXTY.test(ct)) return;
            var len = parseInt(res.headers.get('content-length') || '0', 10) || 0;
            if (len > MAX_BODY || (len <= 0 && !/json$/.test(ct))) return;
            scans++;
            res.clone().text().then(function (t) { scanText(t, res.url); }, function () {});
          } catch (e) {}
        }, function () {});
      } catch (e) {}
      return p;
    };
  }
  var XP = window.XMLHttpRequest && XMLHttpRequest.prototype;
  if (XP && XP.send) {
    var realSend = XP.send;
    XP.send = function send() {
      try {
        var x = this;
        x.addEventListener('load', function () {
          try {
            if (scans >= MAX_SCANS || (x.responseType !== '' && x.responseType !== 'text')) return;
            var ct = String(x.getResponseHeader('content-type') || '').toLowerCase().split(';')[0].replace(/\s+/g, '');
            if (!TEXTY.test(ct)) return;
            scans++; scanText(x.responseText, x.responseURL);
          } catch (e) {}
        });
      } catch (e) {}
      return realSend.apply(this, arguments);
    };
  }
  function hookSrc(proto) {
    try {
      var d = Object.getOwnPropertyDescriptor(proto, 'src');
      if (!d || !d.set || !d.configurable) return;
      Object.defineProperty(proto, 'src', { configurable: true, enumerable: d.enumerable, get: d.get,
        set: function (v) { try { if (typeof v === 'string' && v.indexOf('blob:') !== 0) report('src', v); } catch (e) {} return d.set.call(this, v); } });
    } catch (e) {}
  }
  if (window.HTMLMediaElement) hookSrc(HTMLMediaElement.prototype);
  if (window.HTMLSourceElement) hookSrc(HTMLSourceElement.prototype);
  if (window.HTMLMediaElement) {
    var realPlay = HTMLMediaElement.prototype.play;
    HTMLMediaElement.prototype.play = function play() {
      try { var s = this.currentSrc || this.src || ''; if (s.indexOf('http') === 0) report('play', s); else emit('play', ''); } catch (e) {}
      return realPlay.apply(this, arguments);
    };
  }
  if (window.MediaSource && MediaSource.prototype.addSourceBuffer) {
    var realAdd = MediaSource.prototype.addSourceBuffer;
    MediaSource.prototype.addSourceBuffer = function addSourceBuffer(mime) { try { emit('mse', String(mime).slice(0, 80)); } catch (e) {} return realAdd.apply(this, arguments); };
  }
  if (window.Navigator && Navigator.prototype.requestMediaKeySystemAccess) {
    var realEme = Navigator.prototype.requestMediaKeySystemAccess;
    Navigator.prototype.requestMediaKeySystemAccess = function requestMediaKeySystemAccess(ks) { try { emit('eme', String(ks).slice(0, 40)); } catch (e) {} return realEme.apply(this, arguments); };
  }
})();
```

**Why this is the riskiest row of Part C, and how to contain it:** a site may inspect `fetch.toString()` or compare prototypes (anti-tamper). Keep the first version plain. Add a site opt-out list inside the file (registrable domains, empty at first) so a broken site is fixed by one small release, and surface the existing per-site "Shields off" switch through the "page may be broken" chip (EY-34). If the lab shows a real site that checks for native code, THEN add a `Function.prototype.toString` shim (a table of wrapper -> original that answers with the original's text, and lists the shim itself) - not before: a bug in that shim would affect every page. `res.clone()` tees the body: the 300 000-byte bound and the type filter keep the extra memory to a few hundred KB at most. Never clone media, image or octet-stream answers.

**Cost to measure** [E: below 5 ms per scan on this CPU, nothing when the page loads no JSON]: the scroll benchmark in TABLET_DIAG (64-65 fps, 0/150 dropped) on a script-heavy page with the hook on and off, and the Health marks `media hook scans=N reports=M` per page (host only). Ship only if scrolling does not change.

**Proof:** lab cases 1, 2, 4, 12, 13 in 5.8; web-tests: a headless-Chromium test like `web-tests/guard.test.js` that loads the hook in a page, checks that `fetch`, XHR, `play` and the `src` setters still behave (results identical, errors identical), that a second injection does nothing, and that the regexes find the three address shapes (absolute, protocol-relative, root-relative with the response URL as base).

### 5.4 EY-16 - the markup scan, second version (EXT/content.js:199-243)

All changes are inside the existing scan; keep the 40/60-report caps and the idle scheduling.

1. **Passes:** run on DOMContentLoaded (idle), 2.5 s after load (as today), on every `play` note from the hook (1.5 s later), on `pageshow` when `event.persisted` is true (clear `sent`, the cursors and the counter first: a restored page must re-report what the app forgot), and from a throttled MutationObserver: the callback only sets a flag and arms ONE timer of 3 s; the pass runs in `requestIdleCallback`; at most 3 observer passes, and the observer disconnects 60 s after load or after two passes that found nothing new. It never reads mutation records. [N] measure scrolling on a heavy page (feed, news) with the observer on; ship it only if the fps and dropped-frame numbers do not change.
2. **Shadow roots:** walk the elements once per pass (cap 3000), and for every open `shadowRoot` scan `video, audio, source[src]` inside it, recursively to depth 3.
3. **Attributes:** one selector for `[data-src],[data-video],[data-video-src],[data-mp4],[data-hls],[data-m3u8],[data-stream],[data-url],[data-file],[data-source],[data-config],[data-setup],[data-options]`, at most 200 elements; test each value with the extension test, and for JSON-looking values run the address regexes (video.js `data-setup` carries `{"sources":[{"src":"x.mp4"}]}`).
4. **`<track>`, `link rel=alternate|preload`:** `video track[src]` and `track[kind=subtitles|captions]` go to a new `k:'sub'` message (EY-20); `link[rel=alternate][type]` with a video or HLS/DASH type, and `link[rel=preload][as=fetch|video]` whose address passes the extension test go to `report`.
5. **Resource observer (the safety net):** `new PerformanceObserver(cb).observe({type:'resource', buffered:true})` at document start, in every frame (content.js already runs in all frames). For each entry whose `initiatorType` is not `img`, `css`, `script` or `link`, and whose `name` passes `MEDIA_EXT` but not `SEGMENT`, call `report`. It sees every address the document loaded through `fetch` or XHR, even when the network sniffer missed it (cached, answered before the listener, wrong type), and does not use the page's 250-entry timing buffer, which segment requests fill within seconds. [N] on engine 160: which `initiatorType` values appear for `<video src>` loads (Gecko may not list media-element loads at all: the markup scan covers those) and that `buffered:true` works for resource entries.
6. **Links:** read up to 1500 links (was 600) inside the idle budget (stop after 8 ms of work and continue in the next idle callback); include `[download]` attributes and anchors whose address has a media extension.
7. **Scripts:** keep the inline-script rules; also read `script[type="application/ld+json"]` for `contentUrl` (a media file; `embedUrl` is a PLAYER PAGE, not a media address: do not report it as media).

Everything is reported through the existing `report`/`k:'dom'` path, so ad networks are dropped by the app as today. Cost [E]: a pass over 3000 elements and 1500 links is a few milliseconds, once per pass.

### 5.5 EY-17 - keep the right entries (CORE/media/MediaList.kt, EXT/background.js)

1. **Rank instead of age.** In `MediaList.add`, after inserting the new entry, while `items.size > max` remove the entry with the lowest value, never the one just added:

```kotlin
private fun value(c: MediaCandidate, newestFirstIndex: Int): Int {
    var v = 0
    if (c.kind != MediaKind.FILE) v += 100               // playlists and manifests
    if (c.touched) v += 80                               // played (page report), long-pressed, opened in the sheet, downloaded
    if (c.length >= MIN_WITH_STREAM) v += 40 else if (c.length >= MIN_FILE) v += 10
    if (c.durationMs >= 60_000) v += 20
    if (c.viaHeaders != null) v += 5                     // its playlist was read: it is real
    return v * 100 - newestFirstIndex                    // equal value: the newer one stays
}
```

   Raise `max` from 24 to 48 (the list is small objects; the cost is the sorted view, which is cached by `version`). Add `var touched = false` to `MediaCandidate`, set by `applyInfo` (the page reported it), `longPress`, opening the sheet on it, and `startDownload`.
2. **Display filter:** keep `MIN_FILE` and `MIN_WITH_STREAM`, but never hide an entry that is `touched`, has `durationMs >= 20 000`, or has `knownHeight >= 480`.
3. **Probe quota:** background.js `maybeCount` becomes `{n, at}`; refill 4 probes every 20 s (never above 12) while the tab lives, and reset it on a main-frame request as today. No new permission, and it works for single-page sites.
4. **Test:** `MediaListTest`: 60 small previews then one 300 MB file -> the file stays; a played 3 MB clip next to an HLS stream stays visible; `touched` entries are never evicted.

### 5.6 EY-18 - "Look for videos on this page" (the empty state)

Today a page with nothing detected shows NO menu entry at all (`VideoFlow.available`, VideoFlow.kt, requires `usableCount() > 0`), so the owner cannot even ask. Add the entry to the page menu for every regular http(s) page, below the other entries.

Flow (`VideoFlow.lookAgain(tab)`):

1. Ask the extension to rescan that page. There is no app-to-extension channel today (`ShieldsBridge` implements only `onMessage`, ShieldsBridge.kt:68; `send()` in background.js:8 is `sendNativeMessage`, extension -> app only). Add the GeckoView-supported channel: implement `WebExtension.MessageDelegate.onConnect(port)` in `ShieldsBridge`, keep the port, and open it once from the background page with `browser.runtime.connectNative(NATIVE)` (the manifest already has `nativeMessaging`) [N: confirm `connectNative` for a built-in extension on engine 160 on the emulator first]. `lookAgain` posts `{k:'rescan', p: <page address>}`; the background finds the tab ids it knows for that page (`tabTop`) and calls `browser.tabs.sendMessage(tabId, {k:'rescan'})` (no extra permission needed for this).
2. The content script clears `sent`, runs the scan, and then nudges the player: the largest `<video>` that is paused and has a source or a blob gets `muted = true; play()`; when it started the playback itself it pauses again after 3 s and restores `muted`. It never clicks page elements and never touches other frames' players unless that frame's own content script receives the same message (it does: all frames).
3. After 3 s the app shows the sheet if candidates arrived. If not, it shows the empty-state sheet with the TRUTH from the hook's markers: `eme` -> "This video is protected (DRM) and cannot be saved." ; `mse` or `blob` and no address -> "This player loads the video in pieces from an address it does not reveal. Press play, wait a few seconds, then tap Look again."; nothing at all -> "No video found on this page. If a video is playing, press play and tap Look again."
4. A "Copy report" button copies a host-only line (`page host | candidates 0 | hints mse,eme | hook scans 12`) for the owner to paste to the maintainer; no addresses.

Proof: lab cases 2, 5, 12 (5.8); a unit test for the text chooser (hints -> message).

### 5.7 EY-19 and EY-20 - stream coverage and subtitles

**EY-19 (rank: after EY-15..EY-18; ask the owner whether the sites in use need it).**
- *Several periods (Dash.kt:61-65):* when the manifest has several periods, choose by duration: if one period is at least 60 % of the total, plan that one, and say so in the sheet ("This stream has several parts; saving the longest"); otherwise keep UNSUPPORTED. Ad periods are short; chapters split into equal periods are NOT safe to guess, so they stay unsupported until a real example exists [N: needs a sample manifest from the owner's sites].
- *WebM-only manifests:* the engine can plan WebM parts (Dash.kt:97) but there is no WebM/Matroska muxer, so a video + audio pair cannot be merged and `mp4Friendly` hides them. Offer "Save video only (no sound)" and "Save sound only" as two labelled downloads when a manifest has no MP4-friendly choice, instead of nothing. Do not write a Matroska muxer.
- *HLS tags:* `EXT-X-DEFINE` (variable substitution in addresses) and `EXT-X-SKIP` (delta updates) are not read (grep of Hls.kt finds neither). They are rare on VOD; implement `EXT-X-DEFINE` (about 30 lines) only after a real playlist shows it. Live stays refused (`Problem.LIVE`); recording a live stream is a different feature - ask the owner before building it.
- *Keep the master address:* see EY-8 (needed for refresh, and it lets the sheet list every rendition again after a failure).

**EY-20 (low rank).** Attach subtitles to the candidate they belong to: `MediaCandidate.subs` (url, label, language), filled from (a) HLS `EXT-X-MEDIA` renditions of TYPE SUBTITLES of the chosen master (the parser already returns them: Hls.kt:112-114), (b) `<track>` elements of the same document (`k:'sub'`), (c) sniffed `.vtt`, `.srt`, `.ass`, `.ttml` files of the same frame (add them to `MEDIA_EXT` as a separate `SUBTITLE` class, not as media). The sheet shows "Subtitles: English, Spanish" with "Also save subtitles" (on when present). Each subtitle downloads as a small file next to the video (`<video name>.<lang>.vtt`) through the normal file path. HLS subtitle playlists made of many `.vtt` segments need their repeated `WEBVTT` headers and `X-TIMESTAMP-MAP` lines stripped when joined: support only single-file subtitles and one-segment playlists first [N: timing of multi-segment joins is unproven].

### 5.8 EY-21 - the finder lab (proof for every row above)

Extend `tools/sitelab/vidlab_srv.py` and `vidlab_gen.sh` (they already serve a movie page, a player frame, a CDN that wants the player's Referer, AES-128 parts with image names, playlists named `.txt` [R]). New pages, each with the expected result BEFORE and AFTER the row:

| # | Page | Before | After |
| :- | :- | :- | :- |
| 1 | `/lab/json-api`: the page asks `/api/source?id=1` and the JSON holds the master playlist address; the player starts only after a tap | nothing listed until play | listed at load (EY-15) |
| 2 | `/lab/mse`: MediaSource player fed by fetches, manifest address in JSON | nothing, no explanation | manifest listed; if not, the empty-state text names "pieces from an address it does not reveal" (EY-15, EY-18) |
| 3 | `/lab/shadow`: `<x-player>` with an open shadow root holding a `<video src=...mp4>` | not found | found (EY-16) |
| 4 | `/lab/data-attrs`: `data-src`, `data-hls`, video.js `data-setup` | not found | found (EY-16) |
| 5 | `/lab/late-iframe`: a tap inserts an iframe holding the player 5 s after load | found only if the network sniffer saw it | found, and Look again works (EY-16, EY-18) |
| 6 | `/lab/spa`: 20 disguised probes on "page 1", `history.pushState`, the real disguised playlist on "page 2" | page 2 playlist never probed | probed (EY-17) |
| 7 | `/lab/previews`: 40 small clips first, the 300 MB video last (and the reverse order) | real video evicted or hidden | real video kept (EY-17) |
| 8 | `/lab/back`: video page, go to another page, Back, open the sheet; 8b: cold start with this page restored | [N] list empty after Back / after cold start | list present (EY-16 `pageshow`, resource observer) |
| 9 | `/lab/cookie-cdn`: parts need a cookie set by the player frame | download answers 403 | see EY-13 |
| 10 | `/lab/tracks`: `<track>` plus HLS SUBTITLES | none offered | offered (EY-20) |
| 11 | `/lab/two-periods`: DASH with two ad periods and one content period | UNSUPPORTED | longest period saved (EY-19) |
| 12 | `/lab/drm`: a page that calls `requestMediaKeySystemAccess('com.widevine.alpha')` and plays encrypted media | silent | "protected (DRM)" text (EY-15, EY-18) |
| 13 | `/lab/worker-fetch`: a dedicated Worker fetches the playlist and passes the text to the page, which feeds a MediaSource | [N] unknown: the sniffer has no tab filter (EXT/background.js:567-600 only drops stale documents), so the Worker's request may be listed under no tab, with the document as its page | listed if the app accepts it; otherwise record the limit (the MAIN-world hook does not run inside Workers) |

Each page gets a Diagnostics-visible result: after loading, the emulator run copies the Health lines `media seen`, `maybe-hit`, `hook scans=N reports=M` and the sheet's entry count. The host-only rule for logs applies.

### 5.9 What cannot be offered, and should be said so

DRM-protected streams (Widevine, FairPlay, PlayReady); live streams (as today); video built in the page from canvas, WebRTC or WebAssembly; players that sign every segment request with page script (the playlist alone is not enough) [E: rare]; streams over WebSocket or WebTransport; content behind a login the manager cannot repeat. For DRM and the "pieces" case the sheet must say so (EY-18) instead of showing nothing.

---

## 6. Part D - the download manager (EY-22 .. EY-28)

### 6.1 State, so nothing is built twice [R]

Done and shipped (TASKS.md X3, research/AUDIT-2026-11.md, research/DOWNLOAD_MANAGERS.md): M1 tail compare and Last-Modified compare, M2 outage ladder, M3 refused extra connection, M4 auto-resume after a kill, M5-lite two connections at once for files from 8 MiB with a validator, M7 content sanity, M8-lite settings, queue actions (Start now, move up/down: Engine.kt:884-892), the settings for speed limit, simultaneous downloads and connections. Open: **M5 rest, M6 digest, M9 IO path, M10 refresh, T0, G18 (reopen length check), G21 (default-network change), and the validator gate described next.** Facts: `Planner.cap` returns 1 unless ranges, validator and total are all present (Planner.kt:16-17); `Config(maxConnsTotal = if (low) 3 else 6 ...)` (DownloadController.kt:143); stream jobs spawn up to `maxConnsPerDownload` workers each (StreamEngine.kt:554) outside the file engine's budget.

### 6.2 EY-22 - files from servers that send no validator

**Today.** `Rec.resumable = ranges != 0 && total > 0 && hasValidator` (Model.kt:83-85). For a file without a strong ETag and without Last-Modified:
- no Pause button (the row's action list is built from `resumable`: Engine.kt:887/889), and `pause` does nothing (132);
- after a failure `resume` wipes the partial file and starts from zero (139, 271);
- `pauseAll` skips it (183);
- an outage fails fast instead of waiting (`waitOutOutage`, 757);
- only one connection ever (Planner.kt:16-17).

But the engine can already continue such a file safely in the normal case: `openWindow` starts the request up to 4 KiB BEFORE the cursor and compares those bytes with the file (Engine.kt:556-560, `sameTail` at 599-612), and a difference ends in NEEDS_DECISION(FILE_CHANGED) with the partial kept (573-579). So the gate is stricter than the safety the code already has.

**Step 1 (low risk, do first).** Add to `Rec`:

```kotlin
/** Pause, wait-out and resume make sense: the server may serve ranges, the length is known, and either a validator or a readable file lets the 4 KiB tail compare prove it is the same file. */
fun canContinue(sinkReadable: Boolean): Boolean = total > 0 && ranges != 0 && (hasValidator || (ranges == 1 && sinkReadable))
```

Use it at the seven sites above (replace `resumable`; keep `resumable` for `Planner.cap` until step 2). `ranges == 1` is set by the first 206 or by an `Accept-Ranges: bytes` reply [R: check where it is first set in `Engine.start`]. The sinks used for downloads (`MediaStoreSinks.MediaSink.readable = true`, MediaStoreSinks.kt:97; check `FileSinks`) are readable. Also add the G18 check while in there: `MediaStoreSinks.reopen` (MediaStoreSinks.kt:84-89) returns a sink without comparing the file's length with `rec.done`; fail the reopen (restart from zero, or NEEDS_DECISION) when `Os.fstat(fd).st_size < rec.done`.

**Residual risk, to be stated in the code comment and the OUTBOX:** a change beyond the compared window with the same length is not detected until a checksum exists (M6). Chrome accepts the same residual.

**Tests (core, `EngineTest` with the T0 fake server of EY-28, server sends no ETag and no Last-Modified):** pause at 30 % -> the row offers Pause; resume -> the first request starts at `cursor - 4096`, the output is identical to the source, the extra bytes are at most 4 KiB per window; an outage in the middle -> WAITING_NETWORK, not FAILED, and completion after the outage; `mutateAt(offset)` inside the last 4 KiB -> NEEDS_DECISION(FILE_CHANGED) with the partial kept; a 200 answer to the ranged request -> NO_RANGE handling as today; a file that is shorter after the pause -> restart, never zero-filled holes.

**Step 2 (medium risk, only if EY-27 proves it pays).** Let validator-less files use several connections. The danger: nothing proves a second connection reads the same file version as the first. Protect every seam: a new segment B that starts at offset X requests from `X - 4096`, keeps those 4096 bytes in memory, writes only from X on, and when the neighbour segment A (which ends at X) completes, compares A's last 4096 bytes (read back from the file) with B's kept bytes. Equal -> the seam is verified. Different -> NEEDS_DECISION(FILE_CHANGED). Cost: 4 KiB per extra connection. Only do step 2 when EY-27 shows 2 connections beat 1 by at least 25 % on the owner's hotspot or Fast Wi-Fi: round-1 reasoning says a shared radio and a carrier cap may give no gain [E].

### 6.3 EY-23 - never fail at 80 % for lack of space

**Today [R].** Files with a known length ask Android to reserve the space up front (`allocateBytes`, MediaStoreSinks.kt:70, with `posix_fallocate` as fallback at 74), so they fail early (NO_SPACE, text at Planner.kt:177). Streams do not: `sinks.create(rec.name, rec.mime, -1, ...)` (StreamEngine.kt:673) reserves nothing, an out-of-space error shows up in `fetchTo` (StreamEngine.kt:1054) after possibly hundreds of MB, and the staged parts already on disk keep their space. The tablet had 1.62 GB free of 27.75 GB (TABLET_DIAG_2026-11-13), and the engine's disk cache is fixed at 512 MB (gecko-prefs.txt:35-36).

**Fix.**
1. *Preflight for streams:* `need = estimate * 1.10 + 64 MiB`, where `estimate` is `rec.estimate` (set from `exactBytes` or the enqueue estimate: StreamEngine.kt:279, 659; if it is 0, use the average size of the first three parts times the part count). `have` = `StorageInfo.allocatable(ctx)` (APP/diag/StorageInfo.kt:39-43: free space plus what Android may clear, minus its reserve; API 26+). Put the arithmetic in `core` as a pure function (like `UpdateLogic.storagePlan`, UpdateLogic.kt:163) and pass `have` in through a small `SpaceGate` interface next to `NetGate`, so it is testable. Check at start, again after the first three parts, and every 60 s while running.
2. *Not enough:* state NEEDS_DECISION with `Reason.NO_SPACE`, kept staged parts, a new `Action.MAKE_ROOM` and Continue/Cancel. `MAKE_ROOM` opens one screen that shows the sizes of: the browser cache (`StorageInfo.cacheBytes`, the "Clear cache" button already exists at BrowserActivity.kt:1409-1414), the page snapshots and thumbnails, finished downloads (the existing list with sizes), and the app's own footprint (`StorageInfo.ownFootprint`). Never delete anything without the owner's tap.
3. *While running:* below 48 MiB of `have`, pause with PAUSED_SYSTEM and reason NO_SPACE instead of failing at the error; resume by itself when `have` is back above 256 MiB (check on the 1-second tick).
4. *Adaptive disk cache (optional, low priority):* the cache cap is a fixed value in the engine config (`browser.cache.disk.capacity = 512000`, smart size off). Choose it at engine start from free space (`clamp(free / 8, 64 MB, 512 MB)`) [N: how GeckoEngine writes the config file - if it copies the asset, substitute the line at copy time; the file's version number must change so Diagnostics keeps proving the file was applied].
5. *Tests:* the pure function with a fake `have`; an engine test where `have` drops during a stream (the job pauses, keeps its parts, resumes when space returns).

### 6.4 EY-24 - drop dead sockets when the default network changes (G21)

`DownloadController.startNetWatch` (DownloadController.kt:371-378) calls `engine.onNetwork()` and `streams.onNetwork()` on every default-network callback, and both act only on online/offline (Engine.kt:199-206, StreamEngine.kt:446-452). When the default network is REPLACED while staying online (the hotspot reconnects, the Fast Wi-Fi tunnel is switched on or off, a Wi-Fi roam), the open sockets keep waiting until the 15 s stall timer (`Config.stallMs`) cuts them [E: 15-20 s per event].

Fix: remember the default network's identity in `startNetWatch` (the `Network` object, compared with `==`, or `networkHandle`); in `onAvailable(network)` when it differs from the previous one, call `engine.onNetworkSwitched()` and `streams.onNetworkSwitched()`, which abort every worker's open response (`w.resp?.abort()` / `job.responses`), keep all cursors, count a `netEpoch` on the job (used by EY-10 to skip L0/L1 and refresh the playlist first), and let the workers' normal retry re-dial at once (no backoff for the first retry after a switch). Do NOT react to `onCapabilitiesChanged` alone: it fires for bandwidth estimates. A VPN toggle shows up as `onAvailable` for a different network, which is the case wanted.

Proof: emulator: start a large local download, `svc wifi disable` then `enable`, and record the seconds until bytes move again (Health line `net switch aborted=N resumed_after_ms=M`); unit test with a fake `NetGate` that reports a switch.

### 6.5 EY-25 - the IO path (M9, small, optional)

- `Engine.checkpoint` (Engine.kt:842-848) calls `sink.sync()` on the control thread, the same thread that serves pause, plan and exit events. Run `sync()` and `saveSnapshot` on a single-thread IO executor, one pending request per job (the `ckptPending` flag already exists at line 839), keeping the order "data first, metadata second" because the snapshot is taken under the lock before the sync is queued and written after it.
- Use `Os.fdatasync` when the file was preallocated and its size did not change; `fsync` at publish. Skip `allocateBytes` below 8 MiB (the call has a fixed cost).
- Skip the per-worker write buffer (M9's third item): the cursor-versus-written invariant is not worth the risk.
- Evidence [E]: an fsync on eMMC can take 100-800 ms; the control thread is not on the data path, so the visible effect is a late Pause or a late row update. Add a Health mark when a control-thread task waits more than 200 ms before changing anything.

### 6.6 EY-26 - M5 rest (only after EY-27)

From research/DOWNLOAD_MANAGERS.md M5, in the order of value over risk: (e) stream jobs draw their workers from the same connection budget as files (today up to 3 per stream job, StreamEngine.kt:554, outside `maxConnsTotal`; two downloads plus a stream can open 9 sockets on a hotspot where a token limit or a carrier policer is the bottleneck); (d) end-game: a worker idle for more than 2 s and no tail of at least 4 MiB left -> cut the slowest window's remainder at `cursor + 3 s x speed`; (c) per-worker meter, abort a connection below 25 % of its siblings' median for 10 s (at most once per 20 s per segment, only with at least 2 siblings); (a) bounded probe window for the URLConnection fetcher (`probeWindow = 8 MiB`) so a cut wastes nothing. Each with the FakeServer extensions listed in M5 (`perConnBps`, `totalBps`, `slowConn`).

### 6.7 EY-27 - the download lab (measure before tuning)

One Diagnostics action "Download lab" (asks first: "uses about 100 MB"). It downloads the first 24 MiB of a range-capable test file with 1, 2, 3 and 4 connections in turn, through the real engine with a discarding sink, and prints one copyable line: `lab: n=1 2.3 MB/s | n=2 2.9 | n=3 3.0 | n=4 2.8 | ttfb 410 ms | tls 210 ms | proto h2 | net hotspot|fastwifi`. Test file: the app's own update asset on GitHub (public, range-capable, HTTP/2; the update code already knows its address) plus an address the owner can paste. Run it once on the hotspot and once with Fast Wi-Fi on. It answers: does a second or third connection help (decides EY-22 step 2 and EY-26), what does Fast Wi-Fi do to the first byte, and whether HTTP/3 is in play. Record the numbers in `research/DOWNLOAD_LAB.md` and set the defaults from them (`dlConns`, `maxConnsTotal`).

### 6.8 EY-28 - T0, the raw-socket fake server (enabler for EY-7, 22, 24, 26)

Spec is in research/DOWNLOAD_MANAGERS.md (T0, about 6 h). Scenarios to build into `FakeServer` (loopback `ServerSocket`, plain HTTP/1.1, JVM tests in `core`):

| Mode | Behaviour | Used by |
| :- | :- | :- |
| no validators | no ETag, no Last-Modified, Accept-Ranges on | EY-22 |
| `mutateAt(offset)` | the file changes at an offset between two requests | EY-22 (tail and seam compare) |
| `token(ttl)` | an address works for `ttl` seconds, then answers 403; a new address is minted from a playlist | EY-7, 9, 10 |
| `flaky(n)` | the n-th request answers 403/503 once | EY-7 |
| `reset(after)` | the connection is reset after N bytes | EY-24, EY-26 |
| `perConnBps`, `totalBps`, `slowConn` | throttles | EY-26, EY-27 |

### 6.9 Later: a download-only process (V3b, TASKS.md line 137)

`ProcessLife` keeps the whole app process alive while a download or update is busy, and the process includes the engine (a content process may remain idle after the sessions are closed). The repo measured it on the emulator (TASKS.md:137, the V3b entry): about 280 MB for the main engine process plus the content, GPU and helper processes, against about 40 MB for a bare downloader [R]; the tablet's own figure is [N]. That memory is held for the length of a download after the owner closed the browser, on a 2 GB tablet. The clean fix is the open V3b: the download service in its own process, so closing the browser kills the engine at once while the downloads continue at about 40 MB [R, emulator]. It touches SQLite/prefs access across processes and `Health`; it needs a design pass of its own and is not a Wave 1-3 row. Decide it with two numbers: how long downloads typically run after the browser is closed [N: ask the owner / count in Diagnostics], and the engine's idle memory in that state [N: Diagnostics per-process memory]. A cheaper partial step does not exist: a GeckoView runtime cannot be shut down and created again inside one process.

---

## 7. Part E - speed and RAM (EY-29 .. EY-33)

### 7.1 Where the tablet stands [R]

From research/TABLET_DIAG_2026-11-13.md (alpha27): scroll 62-65 frames a second with 0 of 150 dropped, 450-600 MB available, 4 of 6 tabs live and hibernation working, heap 14/128 MB, page loads 1.5-5.2 s, blocker image loaded in 2974 ms at start, video decoding hardware and smooth. The tablet has 1.62 GB free of 27.75 GB. Round-1 work (EX-1..EX-18, alpha25-26) is in. What is left is not one big thing: it is the start-up path (Part A), memory kills between Android's callbacks, the cost of the request path, and a few engine settings that only a measurement can decide. The largest owner-visible speed gain in this file is EY-1 (the page is simply there). Everything in Part E is a measured step.

### 7.2 EY-29 - a memory governor that does not wait for Android

**Today [R].** `onTrimMemory` (GA/BrowserActivity.kt:399-408) acts at level 10 and above: `LiveBudget.afterTrim(level, Health.memAvailableMb(), liveBudget())` then `enforceLiveLimit`, which hibernates least-recently-used hidden tabs that have no live pop-up child (805-815, `hibernate` 818-829); the budget is also re-read when a tab is opened or switched (`liveBudget` 796-803, 2 to 8 tabs from `MemAvailable`). Between those events nothing looks at memory while the owner reads one page for ten minutes. On Android 10 the callbacks for a foreground app arrive late, when the system is already killing cached processes. The kill that hurts is the content process: with one content process all live tabs die together (`onKill`, TabDelegates.kt:197; recovery `sessionGone`, BrowserActivity.kt:1949-1975 restores the front tab from its saved state behind a preview and defers the revival when the app is in the background). That recovery is good and is not to be rebuilt; the aim here is to need it less.

**Design.** A pure state machine in `core` (`MemoryGovernor`, unit-tested with fake samples) driven by a 10 s `Handler` tick in the Activity while it is resumed and the screen is on (stopped in `onPause`; one `MemAvailable` read is about 0.2 ms: Health.kt:121-123):

| State | Condition (thresholds are [E]: tune from the logs) | Action |
| :- | :- | :- |
| LOW | two samples in a row (20 s) below 300 MB | hibernate the least recently used hidden tab that has been hidden for at least 120 s, has no live pop-up child and is not playing media; at most one per 20 s |
| CRITICAL | one sample below 160 MB | hibernate every hidden tab except the most recently used one; `BackPictures.clear()` (the same first step `onTrimMemory` takes at level 15) |
| RECOVER | three samples above 450 MB | nothing is closed; `liveBudget()` rises again by itself at the next tab open |

Log with `Health.count("mem.gov.low")`, `.critical`, `.hibernated`, and one Diagnostics line `governor: low 3, critical 0, hibernated 5, last 276 MB`. There is no public engine call to drop Gecko's caches; do not look for one.

**Proof.** (1) Core tests for hysteresis (no thrash: after an action wait 20 s; a sample series 320, 290, 280, 450 acts once). (2) Emulator: a debug property (for example `debug.kestrel.fakeavail`, read through `DebugProps`, APP/diag/DebugProps.kt, which answers null in a release build: GA/GeckoEngine.kt:137) feeds a pressure series into the governor in place of `memAvailableMb()`; check the hibernation log and that the front tab is untouched. (3) Tablet, no owner action: after a few days of normal use compare `gecko.kill` and `gecko.recover.*` counts per hour with the baseline counts in a Diagnostics copy taken BEFORE the row ships (those counters exist: TabDelegates.kt:193-197, BrowserActivity.kt:1960-1961).

### 7.3 EY-30 - what the request path costs

**Today [R].** The blocker runs inside the extension ("fast" mode is the default: Prefs.kt:75 `eng_ext = true`; the extension process is off, `extensionsProcessEnabled(false)`, GA/GeckoEngine.kt, so its work shares the process family with pages). Per request the extension records only a mean, a maximum and a count above 2 ms (`stats.us`, `maxUs`, `slow`: EXT/background.js:362). Tablet numbers: 648 requests judged, mean 723 us, slowest 11 ms; the image had to wait 2335 ms for the app at start (TABLET_DIAG line 29). A mean of 0.7 ms per request is high for a filter check [E: desktop engines are far below it; the cause is probably the 64-bit hash done on 32-bit limbs in JavaScript, EXT/engine.js `mul64`, which is slow on a 32-bit CPU - [N] profile it]. For a page with 650 requests that is about 0.5 s of work on a thread that also runs page script. Caches that exist [R]: engine.js keeps a 256-entry host -> site cache (EXT/engine.js:169-175); only the legacy path (the app asked per request) keeps a verdict map of 4000 entries, cleared at each new page (EXT/background.js:216-242, 380). The engine path has no verdict cache.

**Steps.**
1. **Histogram, not mean.** Replace `us/maxUs/slow` with a 12-bucket histogram (under 64 us, under 128, ... 64 ms and above) and per-request-type time totals (type only, never an address). Diagnostics prints p50/p95/p99 and the three request types that cost the most time. Cost: one array increment per request.
2. **Only if p95 is above 2 ms:** (a) a decision cache keyed by `type|request host|page site` for verdicts that came from a host-level rule only (valid because nothing else can change that verdict; needs a flag from `check` saying "host rule only" [N: read engine.js `check` first]); (b) an LRU of full address -> verdict (4000 entries, emptied at every list generation and at each new page, as the legacy `verdicts` map already is: EXT/background.js:216-242): count hits first, build it only if the hit rate is above 20 % [N]; (c) change the hash to 32-bit `Math.imul` based arithmetic - this changes the image format on both sides (`core/.../filter/ExtImage.kt`, `FilterSet.kt`, `engine.js`, the shared fixtures in `web-tests/engine.test.js`), so do it only when the profile shows hashing dominates.
3. Do not move the check to native code (repo rule) and do not return to the per-request question to the app (that is the legacy path, 1-3 ms of waiting per request [E]).

**Proof.** Same pages, same cache state, before/after: histogram, plus the per-visit `paint` and `done` lines the app already prints (TABLET_DIAG line 29 shows the format). `web-tests/engine.test.js` must stay green: the Kotlin and JavaScript engines must give the same verdicts.

### 7.4 EY-31 - engine settings to A/B (each one alone, each gated by a measurement)

Rules: every name must pass `tools/check-gecko-prefs.sh` (it greps the real library and the resource archive; 16 popular names are already gone in 160: gecko-prefs.txt header, check script header); a name already in the read-back list (GA/GeckoEngine.kt:192-215) can be read before it is changed; on the emulator set it live with `debug.kestrel.pref1..3` (debug builds only: GA/GeckoEngine.kt:137-145), on the tablet give each candidate one Network lab switch (`GeckoEngine.applyLab`, line 260), OFF by default, and remove the switch or make the value permanent after the test; `kestrel.config.version` is bumped only when a value is made permanent. Protocol: three of the owner's usual sites, three loads each, cache cleared (the "Reload without cache" action exists), compare the median `paint` and `done` lines from the Diagnostics page-load list; adopt only with a gain of at least 10 % and no new failed files; never test two at once.

| Candidate | Why it might help | Risk | Name status |
| :- | :- | :- | :- |
| `network.http.http3.enable`, `network.trr.mode` | EY-33 (below) | low | already read back |
| `network.dns.disableIPv6` on the STANDARD profile (the tunnel profile already sets it, NetProfile.kt:66 and 88) | a carrier network with a broken IPv6 path adds fallback delays to every new host | IPv6-only (NAT64) networks fail | already read back |
| `network.dnsCacheExpiration`, `network.dnsCacheEntries` | revisits within minutes skip the lookup (a DoH lookup costs a round trip) | stale address for a site that moved: very low | already read back |
| request pacing at page start (`network.http.pacing.requests.enabled` and its burst setting) | pacing delays the first requests of a page to protect slow links; on a latency-bound link it may cost more than it saves | more simultaneous flows on a 5 Mbit link: may hurt | [N] name not in the read-back; check the script |
| `javascript.options.mem.gc_parallel_marking` | GC marking on four A53 cores shortens pauses on heavy pages | none seen | already read back (value unknown: read it first) |
| a spare content process: `dom.ipc.processPrelaunch.enabled` | with one content process a pre-launched spare is memory for nothing, if one exists on Android | none | [N] name; first count the app's processes in Diagnostics |

Not on this list on purpose: anything in section 10.

### 7.5 EY-32 - baseline profile and the `BrowserDb` singleton

- **Baseline profile.** CLAUDE.md line 76 already allows `androidx.profileinstaller` in `:app`. A profile of the start-up path (`GeckoApp.onCreate`, `BrowserActivity.onCreate`, `buildUi`, `restore`) lets ART compile that code ahead of time instead of interpreting it for the first runs after each install. On Android 10 the library writes the profile at first launch and the system's background compile uses it; that compile only runs when the device is idle and charging (EX-17 told the owner: charger overnight, screen off, about weekly). So the gain appears after the first idle night, and is [E] 100-300 ms on `first-frame`. Proof: the EY-2 `first-frame` mark before and after a forced compile (`cmd package compile -m speed-profile`) on the emulator, then the tablet after one night. Cost: a profile file (a few KB) generated by a small instrumented run; the build already has the app module.
- **`BrowserDb` as a process singleton** (TASKS.md line 31, "Low priority"): a new `SQLiteOpenHelper` is made in every `onCreate` (BrowserActivity.kt:202) and never closed. Make it a lazily created holder in the process (the same pattern as `Prefs.get`), opened on a worker, so a rotation or an activity restart does not pay for it again and the CloseGuard warnings stop. Combine with EY-3.

### 7.6 EY-33 - encrypted DNS and HTTP/3: measure on the standard profile

The Fast Wi-Fi profile already decides for the tunnel: no DoH, no HTTP/3, no IPv6, lookups by the phone's resolver (NetProfile.kt:56-66, 84-88 [R]). The STANDARD profile (hotspot without the tunnel) uses `TRR_MODE_FIRST` with Cloudflare's resolver when `prefs.dohOn` (GA/GeckoEngine.kt, "Encrypted DNS" comment) and HTTP/3 when `http3On`. On a flaky carrier hotspot, DoH to one host costs one extra connection set-up (TLS) before the first lookup and a timeout (1.5 s by default [N: `network.trr.request_timeout_ms`]) whenever that connection stalls; HTTP/3 over a policed UDP path may fall back late. Neither is known to hurt; neither has been measured here.

Do: add the missing Settings switch for encrypted DNS (TASKS.md line 28, backlog) next to the existing Network lab switches; run the EY-31 protocol with DoH on/off and HTTP/3 on/off on the hotspot, on cold DNS (new hosts: use the owner's less-visited sites or hosts never seen: the page-load list shows them) and on warm; record in `research/NET_AB.md`; change the default only for a gain of at least 10 % in the median `done` of cold-host loads. Privacy note for the report: turning DoH off makes the carrier's resolver see every host name again; say so in the switch's one-line help.

---

## 8. Part F - pages that load and work (EY-34 .. EY-37)

### 8.1 What decides whether a page works [R]

Settings the engine runs with (GA/GeckoEngine.kt:88-130 unless noted), with the kind of page each can break. Every row is "settings read in the repo, effect not measured": the test column says how to find out.

| Setting | Can break | Test |
| :- | :- | :- |
| Tracking protection STRICT without its CONTENT category (88-92) | player and CDN hosts named by Mozilla's lists (fixed for jsDelivr, JW Player, Brightcove, Cloudflare in alpha28: research/DORABY.md); anything new on those lists | Page report (exists); `tools/sitelab` mirror of the page |
| Total Cookie Protection: `ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS` in normal AND private tabs (94-95) | embedded logins, payment frames, comment widgets and "keep me signed in" across sites: a third-party frame gets its own cookie jar per top-level site | lab `/tcp/`: a frame that sets a cookie and a second page that embeds the same frame; expect two separate jars |
| Bounce-tracking protection + cookie purging (96-99) | sign-in chains that pass through a redirect site, short-link redirectors that set cookies: state purged after a day | lab `/bounce/` |
| Query-parameter stripping (97-98) | links whose parameters are on Mozilla's strip list lose them | lab `/strip/` with `?fbclid=` and `?utm_source=` links; expect the page to work |
| Fingerprinting protection on in normal tabs (`setFingerprintingProtection(true)`) | canvas read-back (image editors, canvas captchas, QR/barcode tools, some players) | lab `/canvas/`: draw, `getImageData`, compare |
| Local Network Access blocked (`setLnaEnabled/Blocking`) | pages that talk to `localhost` or a LAN address: companion apps, router and printer pages, local dev servers | lab `/lna/`: `fetch('http://192.168.1.1/')` and `http://127.0.0.1:1/`; expect a visible, explainable result |
| WebRTC address hiding (`media.peerconnection.ice.default_address_only`, gecko-prefs.txt:44) | local-network peer discovery (browser file-sharing between two devices on one Wi-Fi) | lab `/rtc/` |
| `dom.push.enabled`, `dom.webnotifications.enabled` off | web apps that insist on notifications | Page report line |
| Certificate transparency enforced; OS roots not trusted (`enterpriseRootsEnabled(false)`) | sites behind a corporate or antivirus TLS proxy; a hotspot that rewrites TLS | only visible as a certificate error |
| 32-bit ARM: no WebAssembly SIMD, no `SharedArrayBuffer` unless the site sends isolation headers (research/VIDEO_AUDIT.md:121, the owner's own feature run [R]) | sites that REQUIRE SIMD or threads (in-browser video/audio converters, some ML and photo tools) answer "unsupported browser" | a feature lab page (8.4); nothing in the app can change it |
| One content process (`dom.ipc.processCount = 1`, `fissionEnabled(false)`) | none functionally; one heavy page slows all | Diagnostics |
| Content permissions: only geolocation is asked; every other permission request (Storage Access API, persistent storage, others) is denied at once (GA/web/TabDelegates.kt:286-312: autoplay, media-key (DRM) and geolocation are answered, the line at 309 denies everything else) | embedded third-party sign-in frames that call `document.requestStorageAccess()` | lab `/sa/` (8.4) |
| Prompts: alert, text, button, choice, file, auth, popup, repost, before-unload are answered; **date/time, colour and share prompts are not** (no `onDateTimePrompt`, `onColorPrompt` or `onSharePrompt` anywhere in the repo; TASKS.md line 25 lists "date/colour prompts" as a known gap) | date, time, datetime-local, month, week and color `<input>` types do nothing; `navigator.share` fails | lab `/inputs/` (8.4) |
| `loginAutofillEnabled(false)` | no saved-password help (friction, not breakage) | none |

### 8.2 EY-34 - a "this page may be broken" chip (the page report already has the data)

Data exists [R]: the extension keeps a per-page log of failed files, script errors, our blocks (`pageLog`, `logFailed`, `L.errs`, background.js ~178-212, `sendReport`) and the app shows it in Diagnostics > Page report; Firefox's own blocks are listed separately (alpha28). Add a trigger that turns that into one visible suggestion, once per page load, 4 s after the page stops:

- **Condition A (a resource the page needs failed):** at least two failed or engine-blocked files of type script, stylesheet or font whose host is the page's own site or a CDN host (not an ad or tracker host that OUR lists blocked: those failures are intended), or at least three script errors.
- **Condition B (the site says no):** `content.js` checks once, 3 s after load, short pages only (visible text under 3000 characters): a case-insensitive phrase list ("browser is not supported", "unsupported browser", "incompatible browser", "update your browser", "enable javascript", "please unblock" / "unblock challenges"). This is the owner's own report ("one site said incompatible browser or the browser can't access cloudflare", TASKS AI1c).
- **Action:** a slim chip above the dock: "This page may not work fully. Try: Shields off for this site / Desktop site / Dismiss". "Shields off for this site" is the existing per-site switch, which since alpha29 also switches Firefox's own protection off for that session (TASKS AH1c [R]); "Desktop site" is the existing per-tab switch (BrowserActivity.kt:660, 1336). Remember a dismissal per site. Never show on `about:` pages or engine error pages; in a private tab show it but do not remember the dismissal.
- **Cost:** counting at report time; no new listener. Proof: sitelab pages `/broken-cdn/` (script from a host that fails) and `/unsupported/` (a page with the phrase), plus unit tests of the condition function in `core` with fake report data.

### 8.3 EY-35 - the compatibility audit

1. **Read back the facts the owner cannot see.** Extend Diagnostics (GA/GeckoEngine.kt `readBackPrefs`, 185-222) with one line per setting in 8.1 that the engine can report: the cookie behaviour actually in force (the runtime's own getter), tracking protection categories, bounce tracking, fission, FPP. Where a getter does not exist, say "not readable" - do not guess.
2. **Answer the questions the app does not answer.** Look at `onContentPermissionRequest` and decide each permission type consciously: Storage Access (allow once per top site after a tap in that frame, with a chip "Allow sign-in from <host>?" - a privacy trade-off: ask the owner), persistent storage (allow for first-party), others stay denied. Do not change anything before the lab page `/sa/` shows a real failure.
3. **Lab pages** in `tools/sitelab` (static, served from the host over http like the existing ones): `/tcp/`, `/bounce/`, `/strip/`, `/canvas/`, `/lna/`, `/rtc/`, `/sa/`, `/inputs/` (every input type, `navigator.share`, `window.print`, file input with `capture`, `<select multiple>`, `<datalist>`), `/features/` (prints a green/red list of what modern sites test for: `structuredClone`, `Array.prototype.at`, `Promise.withResolvers`, `ResizeObserver`, `OffscreenCanvas`, `ReadableStream`, `CSS.supports('selector(:has(a))')`, `Intl.Segmenter`, WebCodecs, `crypto.subtle`, WebGL2, WebAssembly SIMD and threads, `SharedArrayBuffer`, MediaSource, EME, `navigator.userAgent`).
4. Record in `research/PAGE_COMPAT.md`: the setting, the lab result, the decision (keep, change, ask the owner). Findings that change a default go to the owner as a question, not as a silent change.

### 8.4 EY-37 - pickers and share: the prompts the app never answers

Not answered today (no handler in the repo): date and time pickers, colour picker, `navigator.share`. A GeckoView app that does not answer these makes the controls dead: a booking or filter form with a date field cannot be filled in. Fix with framework dialogs only (no AppCompat, no Compose; the rules in CLAUDE.md):

- `onDateTimePrompt` -> `DatePickerDialog` / `TimePickerDialog` (and the two together for `datetime-local`; month/week with a small custom view or the date dialog and a note), honouring min, max and step from the prompt, answering with the engine's date string format;
- `onColorPrompt` -> a small dialog with a palette of 16 swatches and the current value (a hex field is optional);
- `onSharePrompt` -> `Intent.ACTION_SEND` chooser with the title, text and address; answer SUCCESS/ABORT;
- `window.print` is not part of this row (TASKS.md line 25 lists print/PDF as a known gap; GeckoView's print support in 160 is [N] - find the delegate first).

Proof: the `/inputs/` lab page on the emulator (does the picker open, does the value arrive, does Cancel leave the field unchanged), plus a unit test for the value formatting if it is made pure. Risk: low; effect: pages with date forms work at all.

### 8.5 EY-36 - the page matrix (pages "load properly" is checked, not hoped)

`research/PAGE_MATRIX.md`: one row per page type, one column per alpha (pass / partial / fail, one line why). Method: `tools/sitelab/mirror.js` copies a public page and its files for `serve.py` (the emulator reaches the host as `10.0.2.2` and can load only http pages: [R] research/EX_RESULTS.md:42 and the AH1c proof in TASKS.md), so mirrored rows test layout, script errors, blocked requests and our own filters, NOT third-party hosts or real sign-ins. Rows that need the real internet are tablet rows: the owner opens each address, and one Diagnostics copy at the end carries a line per page (add `failed=N errs=M engine-blocked=K` to the existing per-visit page-load line, host only, so a single Copy covers the whole matrix).

The 20 rows (the receiving Claude picks the sites from the owner's own list in INBOX.md and TABLET_DIAG: doraby.com, pelisjuanita.com, tiktok, Proton Mail, claude.ai, plus public ones): (1) news article with ads, (2) social feed with endless scroll, (3) web mail, (4) the owner's chat site, (5) movie site with six embedded servers, (6) video platform page, (7) sign-in with a Google or Microsoft popup, (8) embedded payment frame, (9) a Cloudflare "checking your browser" page, (10) PDF opened from a link, (11) a long Wikipedia page, (12) forum with lazy images, (13) a captcha, (14) map page (WebGL), (15) page with file upload, (16) page with date fields (EY-37), (17) web app that needs the desktop site, (18) page with a `blob:` or `data:` download (EY-14), (19) page with CJK, Arabic and Indic text (the tablet's font set is [N]), (20) a page that needs notifications or push (expected: degrade quietly). The first run is the baseline; every later release re-runs the emulator-possible rows; a regression blocks the release note.

---

## 9. Proof and measurement plan

### 9.1 Split: what the emulator proves, what only the tablet can

| Kind | Emulator (Android 10 x86_64, no KVM, slow) / JVM | Tablet (owner) |
| :- | :- | :- |
| Logic, state machines, parsers, text choice | core unit tests: all of Parts B, C (scoring, structure compare), D (planner gates, space and network gates), E (governor), F (chip condition) | - |
| Lab pages (finder, expiry, compat) | `tools/sitelab` + `vidlab_srv.py` over http | the same pages once, only for timing-sensitive rows |
| Last-page snapshot | draw, hide rule, privacy wipes, rotation, offline reopen | first-frame and paint times (EY-2 line), the felt result |
| Expiry recovery | all of it (the lab CDN is under our control) | one real expired-link movie: "does the download carry on by itself" |
| Download speed, connections | functional only | EY-27 lab line (the only source of numbers) |
| Memory | replayed pressure series; counters | days of use, then one Diagnostics copy |
| Page loads | mirrored pages: errors, blocked lists | the 20-row matrix, one Copy |

### 9.2 Per-topic recipes (each writes its numbers to a file in `research/`)

1. **Cold start (EY-2, then EY-1, 3, 4, 5, 6).** Five cold starts (swipe away, wait 5 s, open), median of the `cold start` line; file `research/COLD_START.md` with a before/after table per row. Airplane-mode reopen: page snapshot visible, line, then chip after 4 s, then the normal error page after the cap (EY-1).
2. **Expired links (EY-7..EY-14).** `vidlab_srv.py` routes of 4.12; for each: the Health lines, the request count per part index, and the result (done / needs fresh link / failed). Add the unit tests of 4.4-4.9. File `research/EXPIRY_LAB.md`.
3. **Finder (EY-15..EY-21).** The 13 pages of 5.8; table before/after; web test for the hook. File `research/FINDER_LAB.md`.
4. **Download manager (EY-22..EY-28).** The T0 fake server for the logic; EY-27's one line from the tablet on the hotspot and on Fast Wi-Fi; kill-and-resume fuzz as in M4's tests for the new gates. File `research/DOWNLOAD_LAB.md`.
5. **Memory (EY-29).** Baseline counters first; then the governor; then counters again. File `research/MEMORY_GOV.md`.
6. **Request path (EY-30).** Histogram line before/after on the same three pages. File in `research/SPEED5.md` together with the EY-31/33 A/B tables.
7. **Pages (EY-34..EY-37).** The lab pages, the matrix, the chip's unit tests. Files `research/PAGE_COMPAT.md`, `research/PAGE_MATRIX.md`.

### 9.3 What the owner is asked to do (as little as possible)

Once per wave, in this order, each one tap or one short routine, never a long test:
1. After the wave that contains EY-2: open and close the browser five times and copy Diagnostics (the cold-start line is in it).
2. After EY-1: turn Wi-Fi off, swipe the app away, open it: say whether the page shows and whether it looks the same.
3. After EY-27: the Diagnostics "Download lab" button (about 100 MB, once on the hotspot, once on Fast Wi-Fi), copy.
4. After EY-29: use the tablet as normal for a few days, then copy Diagnostics once.
5. After EY-31/33: ten minutes: visit three sites three times each per setting while the app tells which setting is on; copy at the end.
6. A video that said "link expired" before: try it again after EY-7..EY-11 and say what happened.
7. For the matrix: open the 20 addresses the app lists, then copy Diagnostics once.

### 9.4 Rules for numbers

A number without its conditions is not a number: write the build, the network (hotspot or Fast Wi-Fi), screen on/off, tabs open, cache state. Medians of five. Never publish a result measured on the emulator as a tablet result, and say so in the file. A row whose gain is below the noise (10 %) is not shipped; record "no gain" in OUTBOX and strike it from the ladder.

---

## 10. Do not repeat - already in the tree (verified against a395248)

Do not re-propose, re-measure or rebuild any of these; extend them only where a row above says so.

**Engine and prefs:** one content process, fission off, extensions process off, bfcache 6 pages for an hour, 64 MB memory cache, 512 MB fixed disk cache, decoded-image cache sized by `size_factor 20`, decode steps and look-ahead of two screens, lazy-load margins, shader precache, bytecode cache after 2 loads, JPEG XL off, double-tap zoom off, zoom-to-field setting, font fallback, Wi-Fi high-performance lock for downloads and the optional lock while the browser is open, speculative connect on the engine thread, link look-ahead and three warm-up triggers (EX-6, EX-15), DoH/HTTP3/IPv6 handling per network profile (Fast Wi-Fi profile), the speculation-rules and no-AVIF switches (EX-12, EX-14, Network lab), engine read-back lines (gecko-prefs.txt version 7; EX-1..EX-18, research/EX_RESULTS.md). EX-13 (a bigger network buffer) was decided NOT to build; do not reopen it without new A/B numbers.
**Protection and compatibility:** tracking protection STRICT minus CONTENT, per-site Shields off also switches Firefox's own protection off, Page report with failed files / script errors / engine-blocked line, "Reload without cache", embeds wait for a tap, popup and redirect guard, own HTTPS-first policy, bundled lists with the default-lists regression test, per-site switches, sitelab mirror and server.
**Memory and tabs:** `LiveBudget` (2-8 live tabs from `MemAvailable`), hibernation with state, pictures cleared at trim, recovery after a content-process death with state, `ProcessLife` exit, thumbnails and tab state stores, `flushSessionState` in `onPause`.
**Downloads:** the engine with segments, validators, `If-Match`/`If-Unmodified-Since`, the 4 KiB tail compare, the outage ladder, refused-connection retire, auto-resume after a kill, fast start (two connections from 8 MiB with a validator), content sanity, queue actions, speed limit and settings, MediaStore sinks with allocation, HLS (AES-128, byte ranges, fMP4 init, TS to MP4) and static DASH, pair merge, cancel deletes the partial file on every path (AL4).
**Video finder:** the sniffer by content type and by address, disguised-playlist probes by first bytes with the header ladder, per-request Referer/Origin, the working header set remembered, markup scan, `vid` info, long press, "only the page I am looking at" (AL1), server-switch refresh (AL5), names from the page (AL6, `EpisodeNames`).
**In flight by the other session (do not touch until it lands):** AL1-AL6: `MiniTracker`, `DownloadPopup`, the taller progress bar, `EpisodeNames`, `MediaNames`.

---

## 11. Limits of this report

- **Read only the repo.** HEAD a395248. No web, no engine download, no tablet, no emulator run. Everything tagged [E] is reasoning, everything tagged [N] needs the test named beside it. Line numbers are valid for a395248 and drift as the other session commits: re-grep the symbol before relying on a number.
- **Engine-specific claims I could not check** (all [N]): that `runtime.connectNative` works for a built-in extension on engine 160 (EY-18); which `initiatorType` values appear in resource timing for media loads and that `buffered:true` works for resource entries (EY-16); whether media events cross shadow roots (EY-16); whether `onPageStart` fires for a restore from the page cache and so whether the list survives Back (EY-16, lab case 8); whether the body stream of `onExternalResponse` can be read after the callback returns (EY-14); whether Gecko offers a partition option for the manager's fetcher (EY-13); the exact names of the pacing and prelaunch prefs (EY-31); how `GeckoPrefs.writeConfig` builds the config file (EY-23 step 4); whether GeckoView 160 has a print delegate (EY-37); how the tablet's font set handles CJK/Arabic/Indic pages (EY-36).
- **Owner facts I do not have:** which sites produce "link expired" (only the doraby/vidsrc case is documented, AK4); how long downloads run after the browser is closed (V3b); which sites need multi-period DASH or subtitles (EY-19, EY-20); whether 2 connections beat 1 on the owner's links (EY-27 decides EY-22 step 2 and EY-26).
- **Not covered here on purpose:** DRM, live recording, a Matroska muxer, native code, a separate download process (V3b is only discussed), changes to AL1-AL6 files, anything that would send a cookie, token or signed address into a log, a report or a commit.
- **Judgement calls to confirm with the owner before building:** the Storage Access answer (EY-35 step 2), "Stay ready after closing" memory cost (EY-5), a hidden background refresh of a video page (EY-11 phase 2), turning DoH off on the standard profile (EY-33).

---

## 12. The copy-paste TASKS block

Paste below the last section of `TASKS.md` as is. Then run `bash tools/ckpt.sh`, then work the ladder wave by wave (section 0). Mark `[x]` only with the proof named in the row; a row waiting for the tablet is `[~]` with the owner's smallest step written in it.

```
## Owner request 2026-11-13 (round 2) - research/REVIEW-EXTERNAL-2.md - work the waves of its section 0
- [ ] EY "The changes from the report are in. Do another round: Read only the browser repo and make a report on methods to improve the speed, ram handling, page functionality so all different types of pages load quickly and properly, download manager improvements, video finder improvements so it can properly find as many video sources as possible to download, a fix when some video downloads say the link expired (can it automatically try to resume or open a fresh connection by itself?), how to instantly show the last page viewed when I close then reopen the browser (right now it reloads the last page I visited)" - the report is research/REVIEW-EXTERNAL-2.md (ladder EY-1..EY-37); Wave 1 = EY-2, EY-1, EY-7, EY-8, EY-22 step 1, EY-17; Wave 2 = EY-9..EY-12, EY-15, EY-16, EY-18, EY-23, EY-24, EY-37; Wave 3 = the rest
  - [ ] EY-1 last-look snapshot: capture at pause + when scrolling settles, store in filesDir (never private tabs), draw 1:1 undimmed in the first frame, hide at first paint AND scroll restored; wipes in coldStartClear/clear data/tab close; proof: emulator offline reopen + EY-2 line
  - [ ] EY-2 cold-start timeline: Health marks (app-create, activity-create, first-frame, look-drawn, runtime-ready, session-open, first-paint, page-stop) + one copyable Diagnostics line; five cold starts into research/COLD_START.md
  - [ ] EY-3 first-frame diet: move DownloadController.init, UpdateManager.init, BrowserDb, non-essential Shields.init after the first frame, one at a time, judged by the first-frame mark
  - [ ] EY-4 engine file prefetch (posix_fadvise WILLNEED on the engine library from GeckoApp.onCreate, UI process only, skipped when MemAvailable < 400 MB); keep only if the runtime-ready mark improves
  - [ ] EY-5 "Stay ready after closing" Off/5/15 min (ProcessLife delayed exit, exit on trim, never with clear-on-exit); off by default
  - [ ] EY-6 test whether the restored page paints from the HTTP cache offline; find the lever only if it does not
  - [ ] EY-7 a refused part (401/403/410) is transient first: Denied exception, 3 tries with jittered backoff, other workers wait, reset on success; Failed(LINK_EXPIRED) only after recovery fails; core tests with a fake Fetcher
  - [ ] EY-8 carry master address, frame, header-set ladder, fingerprint, expiry hint into StreamSource/StreamRec (old records still load); rotate header sets on refusal
  - [ ] EY-9 refresh the playlist in place (same structure check: count, durations, names, key bytes), swap part/key/init addresses by index, keep staged parts; refresh before resume when stale
  - [ ] EY-10 expiry hints (expires/exp/X-Amz-Expires/hdnts/JWT), refresh at start/resume/60 s before; a default-network change marks links as possibly bound to the old address
  - [ ] EY-11 "needs fresh link" flow: NEEDS_DECISION + LINK_EXPIRED + Action.REFRESH, auto-match the next sniffed copy by fingerprint, assisted page open with muted play, optional hidden refresh (phase 2, ask the owner)
  - [ ] EY-12 single files: Engine.refreshSource (M10 adopt) and per-connection useOriginal; L0 retry before the one re-ask
  - [ ] EY-13 cookie-gated CDNs: lab case first (iframe cookie + TCP); only if it fails copy the browser's Cookie for that candidate (never logged, scrubbed at terminal states, never private)
  - [ ] EY-14 keep the first response of engine-started downloads (one-time links, POST, blob:/data:): test first whether the body can be read after onExternalResponse returns
  - [ ] EY-15 MAIN-world media hook (fetch/XHR text scan, src setters, play, MediaSource/EME markers) registered next to guard-main.js; relays as k:'dom'; site opt-out list; web test + lab cases 1, 2, 4, 12
  - [ ] EY-16 DOM scan v2: shadow roots, data-* attributes, track/link tags, throttled observer, pageshow rescan, PerformanceObserver resource safety net, 1500 links; scroll fps must not change
  - [ ] EY-17 MediaList by rank not age (cap 48, touched entries kept), display filter exemptions, probe quota refilled by time; MediaListTest
  - [ ] EY-18 "Look for videos on this page" entry on every http(s) page: app-to-extension channel (onConnect + connectNative), rescan + muted play nudge, truthful empty-state text (DRM / pieces / nothing)
  - [ ] EY-19 stream coverage: DASH multi-period (longest period when >= 60 %), WebM-only manifests as two labelled downloads, EXT-X-DEFINE only on a real example; no live, no DRM
  - [ ] EY-20 subtitle companion files (HLS SUBTITLES, track elements, sniffed vtt/srt), single-file subtitles first
  - [ ] EY-21 finder lab: 13 pages in tools/sitelab/vidlab_srv.py with expected before/after, web test for the hook; research/FINDER_LAB.md
  - [ ] EY-22 validator-less files: step 1 Rec.canContinue (tail-compare gate) opens Pause, outage wait and resume + reopen length check (G18); step 2 parallel segments with a seam compare only if EY-27 shows a gain of 25 % or more
  - [ ] EY-23 space preflight for streams (estimate x 1.10 + 64 MiB vs StorageInfo allocatable), NEEDS_DECISION + MAKE_ROOM, pause (not fail) below 48 MiB; optional adaptive disk-cache cap
  - [ ] EY-24 abort in-flight sockets when the default network changes (not on capability changes); netEpoch for EY-10; emulator wifi off/on timing
  - [ ] EY-25 IO path: checkpoint sync off the control thread, fdatasync when preallocated, no preallocation under 8 MiB; Health mark for slow control tasks
  - [ ] EY-26 M5 rest, only after EY-27: stream workers inside the connection budget, end-game cut, slow-connection recycle, bounded probe window
  - [ ] EY-27 Diagnostics "Download lab" (1/2/3/4 connections, first-byte and TLS time, protocol, network profile); research/DOWNLOAD_LAB.md; set defaults from it
  - [ ] EY-28 T0 raw-socket fake server (no validators, mutateAt, token(ttl), flaky(n), reset(after), throttles) as the test base for EY-7, 22, 24, 26
  - [ ] EY-29 memory governor (core state machine, 10 s tick while resumed): LOW hibernates the oldest hidden tab, CRITICAL all but one; counters; baseline gecko.kill counts first
  - [ ] EY-30 request-path histogram (p50/p95/p99, type totals) and, only if p95 > 2 ms, host-rule decision cache / URL LRU / 32-bit hash; engine.test.js stays green
  - [ ] EY-31 pref A/B batch from section 7.4, one at a time, 10 % rule; check-gecko-prefs.sh for every name; research/SPEED5.md
  - [ ] EY-32 baseline profile (androidx.profileinstaller is allowed by CLAUDE.md) and BrowserDb process singleton
  - [ ] EY-33 encrypted DNS / HTTP3 A/B on the standard profile; add the Settings switch for encrypted DNS; research/NET_AB.md
  - [ ] EY-34 "this page may be broken" chip from the page report (failed or engine-blocked own/CDN files, script errors, "unsupported browser" phrases): Shields off for the site / desktop site / dismiss; core condition tests + sitelab pages
  - [ ] EY-35 compatibility audit: read-back lines for cookie/ETP/bounce/FPP, decide content-permission answers (Storage Access) with the owner, lab pages /tcp/ /bounce/ /strip/ /canvas/ /lna/ /rtc/ /sa/ /inputs/ /features/; research/PAGE_COMPAT.md
  - [ ] EY-36 page matrix: 20 page types, pass/partial/fail per alpha in research/PAGE_MATRIX.md; per-visit page-load line gains failed/errs/engine-blocked counts
  - [ ] EY-37 answer the prompts nobody answers: date/time pickers, colour picker, navigator.share (framework dialogs only); lab page /inputs/ first to confirm they are dead
```

End of report. Nothing in it was run on the tablet.
