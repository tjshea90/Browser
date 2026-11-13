# doraby.com in Kestrel G (owner message 19, 2026-11-13) - TASKS AF1

> "On doraby.com, brave browser loaded the site fine, but kestrel doesn't show any photos or the search bar"

Everything below was measured: the real page read headless through the sandbox proxy (Chromium, with Kestrel's Firefox user agent), our network filter and element-hiding rules run over it, our extension scripts
replayed on it, and a local copy of it opened in the REAL Gecko build on the emulator. **The tablet itself was not available**, so the one thing that decides the case (what failed on the owner's tablet) is
exactly what the new Diagnostics > Page report is for.

## 1. The site

WordPress movie site (theme "fmovie", behind Cloudflare, `Vary: User-Agent`, HTML `zstd`, no-store). 32 requests on the home page: first-party HTML, one 39 KB stylesheet, jQuery, theme scripts, 6 posters,
and 8 files from **cdn.jsdelivr.net** (lazyload.js, bootstrap.bundle, tooltipster, swiper, three stylesheets, one icon font) plus Google Fonts and four ad / tracker hosts.

How the two things the owner named are built:
* **Photos**: every poster is `<img class="lazyload" loading="lazy" src="data:image/webp;base64,...(1x1)" data-src="https://doraby.com/wp-content/uploads/.../17845-poster.jpg">`. Only JavaScript puts the real address in:
  `lazyload.js` (from jsDelivr) and `script.js`, whose last statement is `lazyload()` and whose FIRST statements are `new Swiper(...)` (also from jsDelivr). If any of those files is missing, the posters stay empty black boxes.
* **Search bar (and the menu)**: the magnifier and the hamburger are glyphs of the `bootstrap-icons` font, pulled in by `@import url(https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11/font/bootstrap-icons.min.css)`
  inside the theme stylesheet. No icon CSS / font: an empty header with only the logo. (On a phone-sized screen the search bar is only that icon until it is tapped.)

## 2. What Kestrel's own layers do to the page: nothing wrong

| Layer | Method | Result |
| :- | :- | :- |
| Network filter (bundled default.kfs) | `FilterSet.check` on all 32 requests | blocks exactly 7: `googletagmanager.com/gtag/js`, `cloudflareinsights.com` beacon, `llvpn.com/tag.min.js` (a pop-under ad tag), `google-analytics.com` collect, `my.rtmark.net/gid.js`, `luugy.com/5/...` (ad), `/cdn-cgi/rum`. Every first-party and jsDelivr file is allowed |
| The extension's own JS matcher (engine.js on the real list image) | same 32 requests | identical verdicts (7 blocked, 25 allowed) |
| Element hiding (site rules 17 KB + the generic selectors for this page's classes and ids) | every selector against the live DOM | no selector matches any element |
| Replay in Chromium with the blocked requests aborted, content.js + guard-main.js injected, our CSS injected | posters and the search icon | both appear, no page error |
| Real Gecko build on the emulator, local copy of the whole page (all files served from the copy) | screenshot | complete page: menu icon, search icon, posters |

## 3. The failure that matches the owner's words, and the cause (found, proven on the emulator in every link but the last)

Same local copy, same real Gecko build, but the jsDelivr host answers 404 (`tools/sitelab/serve.py <dir> 8002 /_cdn/cdn.jsdelivr.net`): the header shows only the DORABY logo (no menu icon, no search icon), the
"Latest movies" heading has no icon and every poster is a black box. That IS "no photos and no search bar". So the owner's tablet did not get files from cdn.jsdelivr.net. Why only in Kestrel:

1. **Mozilla's tracking-protection list names jsDelivr.** `disconnect-blacklist.json` (mozilla-services/shavar-prod-lists) has `cdn.jsdelivr.net` (entity "Volentio JSD") in the **"Content"** category. The same category holds JW Player's
   CDN (jwpcdn.com), Brightcove, Kaltura, Mux, Bitmovin, Uplynk, Vimeo, UNPKG, Cloudflare, Shopify's CDN, pinimg.com, twimg.com ... (115 entities): the libraries and video players pages are built from.
2. **Kestrel asked Firefox for its Strict set, which includes that category.** `GeckoEngine.create` used `AntiTracking.STRICT` = 766 = AD + ANALYTIC + SOCIAL + **CONTENT (16)** + TEST + CRYPTOMINING + FINGERPRINTING + EMAIL (strict social tracking protection is a separate switch).
   Read back on the emulator: `urlclassifier.trackingTable = moztest-track-simple,ads-track-digest256,analytics-track-digest256,social-track-digest256,content-track-digest256`.
3. **Firefox blocks what is in that table.** Experiment on the emulator (test entries mark a third-party host as a tracker: `debug.kestrel.pref1 urlclassifier.trackingTable.testEntries=127.0.0.1`): the page's third-party script and picture
   did not load, the dock shield counted 3, and the new Page report said `FAILED: script 127.0.0.1:8003/probe.js TRACKING_URI` (that is Firefox's error name for "blocked as a tracker").
4. Firefox only gets away with this in Strict mode because Mozilla's service sends it allow-lists for the sites that break (`privacy.trackingprotection.allow_list.baseline/convenience.enabled` are on, but a GeckoView app that
   does not run Mozilla's service has nothing to fill them); and the lists themselves are fetched by the engine from Mozilla over the network, which the emulator cannot do (it cannot validate https certificates):
   **the one thing NOT observed is jsDelivr itself being refused on the tablet.** The Page report (below) will name it if it ever happens again.
5. **Fix (alpha28):** `antiTracking = STRICT and CONTENT.inv()` (750). Read back with the fix: `urlclassifier.trackingTable = moztest-track-simple,ads-track-digest256,analytics-track-digest256,social-track-digest256`
   (no content table); the test-entry control still blocks, so blocking of the other categories is intact. Ads and trackers remain the job of Kestrel's own lists (EasyList, EasyPrivacy, uBlock), which do not touch these CDNs.
   Debug builds only: `setprop debug.kestrel.etpcontent 1` brings the old set back to compare.

**This probably also explains the owner's earlier video complaint** ("the loading symbol from the site's own video player stays on top of the video"): the players of JW Player, Brightcove, Kaltura, Mux, Bitmovin, Uplynk and Vimeo
were all in the blocked category, so a player page loaded but its scripts, skins and streams did not. (The muted-autoplay refusal found in alpha27 is a second, real, separately proven cause.)

Other candidates that remain possible if the page is still broken in alpha28: DNS (DoH to Cloudflare vs the system resolver), HTTP/3 over a carrier that drops UDP, a stale or damaged cached file (stylesheet `max-age` 120 days),
a per-site Shields switch, a list update on the tablet newer than the bundled snapshot.

## 4. Tools built for it (alpha28)

* **Diagnostics > Page report** (extension + app): per page, the requests Shields blocked, the files that FAILED (network error name or HTTP status) and script errors; plus one line "Firefox's own tracking protection, this
  page: blocked N: <file> [category]". Collected by `background.js` (`webRequest.onErrorOccurred`, `onCompleted` with a status of 400 or more, our own cancels excluded, `onBeforeRequest` for the counts) and `content.js`
  (`error` events, at most 8 per frame), pushed to the app 4 s after the page settles (at most 3 times per page, also for a page that began before the extension was ready), kept for the last 6 pages, NEVER for a private tab
  (`ShieldsBridge.pageIsPrivate`). Proven on the emulator: `FAILED: script 127.0.0.1:8003/probe.js TRACKING_URI ... script errors: file not usable: script ...`. Tests: `PageReportsTest` (5), `background.test.js` (+4), `content.test.js` (+3).
* **Menu > Reload without cache** (`GeckoSession.LOAD_FLAGS_BYPASS_CACHE`).
* **Engine read-back** of the tracking-protection preferences (`urlclassifier.*`, `privacy.trackingprotection.*`, the allow-lists) in Diagnostics.
* `core/shield/EtpCategories` (the categories and the choice, pinned by a test), `tools/sitelab/mirror.js` + `serve.py` (make a local http copy of any page and open it in the real Gecko build on the emulator, optionally with one host failing:
  use it for every "site X is broken in Kestrel"), debug-only `debug.kestrel.pref1..3` (set an engine preference at start, for emulator experiments).

## 5. What the owner can do / what I need

1. Install alpha28 and open doraby.com: photos, menu and search icons should show now.
2. If anything is still missing: Menu > Diagnostics > Copy and paste it: the Page report names each failed file and why.
3. Menu > Reload without cache for a page that looks stale.
