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

## 3. The failure that matches the owner's words

Same local copy, same real Gecko build, but the jsDelivr host answers 404 (`tools/sitelab/serve.py <dir> 8002 /_cdn/cdn.jsdelivr.net`): the header shows only the DORABY logo (no menu icon, no search icon), the
"Latest movies" heading has no icon and every poster is a black box. That IS "no photos and no search bar". The same picture appears when the page's main stylesheet is missing.
**So the owner's tablet almost certainly did not get files from cdn.jsdelivr.net (or the main stylesheet) while Brave did.** Why only in Kestrel is not known yet. Candidates, none proven:
* DNS: Kestrel asks Cloudflare over HTTPS (DoH) unless the Fast Wi-Fi tunnel is on (then the phone's resolver); Brave uses the system resolver or its own choice.
* HTTP/3: jsDelivr and doraby.com advertise `h3`; a carrier that drops UDP/443 can stall them (Settings > Network lab > HTTP/3). The Fast Wi-Fi profile already turns it off.
* ECH / the large post-quantum TLS ClientHello through the tunnel or the carrier.
* A stale or damaged cached file (the disk cache keeps files for months; the stylesheet has `max-age=120 days`) or one bad answer from the CDN: new menu item **Reload without cache**.
* A per-site Shields switch for this site, or a list update on the tablet that is newer than the bundled snapshot.

## 4. Tools built for it (alpha28)

* **Diagnostics > Page report** (extension + app): per page, the requests Shields blocked, the files that FAILED (network error name or HTTP status), and script errors, e.g.
  `FAILED: script cdn.jsdelivr.net/npm/swiper@5.4.4/js/swiper.min.js?.. CONNECTION_RESET` and `script errors: ReferenceError: Swiper is not defined @ doraby.com/.../script.js:1`.
  Collected by `background.js` (`webRequest.onErrorOccurred`, `onCompleted` with a status of 400 or more, our own cancels excluded) and `content.js` (`error` events, at most 8 per frame), pushed to the app 4 s
  after the page settles (at most 3 times per page), kept for the last 6 pages, NEVER for a private tab (`ShieldsBridge.pageIsPrivate`). Tests: `PageReportsTest` (5), `background.test.js` (3), `content.test.js` (3).
* **Menu > Reload without cache** (`GeckoSession.LOAD_FLAGS_BYPASS_CACHE`).
* `tools/sitelab/mirror.js` + `serve.py`: make a local http copy of any page and open it in the real Gecko build on the emulator, optionally with one host failing. Use it for every "site X is broken in Kestrel".

## 5. What the owner can do now / what I need

1. On doraby.com in Kestrel: Menu > Diagnostics > Copy, paste it (the Page report names the failed files).
2. Menu > Reload without cache: do the photos and icons appear?
3. Same page in a private tab, and with Shields off for the site (shield icon): which of them changes anything?
4. Is Brave on the same tablet and the same network (Fast Wi-Fi on or off)?
